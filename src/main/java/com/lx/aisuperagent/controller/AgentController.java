package com.lx.aisuperagent.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lx.aisuperagent.agent.QinglanManus;
import com.lx.aisuperagent.controller.dto.AgentChatRequest;
import com.lx.aisuperagent.controller.dto.AgentChatResponse;
import com.lx.aisuperagent.controller.dto.ToolInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/agent")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class AgentController {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final QinglanManus qinglanManus;
    private final ToolCallback[] allTools;

    // 会话管理：chatId -> 消息列表 (role / content)
    private final Map<String, List<Map<String, String>>> chatHistories = new ConcurrentHashMap<>();

    /**
     * 同步聊天接口
     */
    @PostMapping("/chat/sync")
    public AgentChatResponse chatSync(@RequestBody AgentChatRequest request) {
        log.info("收到同步请求: chatId={}, message={}", request.getChatId(), request.getMessage());
        List<Map<String, String>> history = chatHistories.computeIfAbsent(
                request.getChatId(), k -> new ArrayList<>());

        try {
            appendHistory(request.getChatId(), "user", request.getMessage());
            ToolCallback[] selectedTools = filterTools(request.getToolIds());

            String result = executeAgent(request, selectedTools, history);

            return AgentChatResponse.builder()
                    .content(result)
                    .chatId(request.getChatId())
                    .isComplete(true)
                    .toolCalls(extractToolCalls(history))
                    .build();
        } catch (Exception e) {
            log.error("聊天异常", e);
            return AgentChatResponse.builder()
                    .error(e.getMessage())
                    .chatId(request.getChatId())
                    .isComplete(true)
                    .build();
        }
    }

    /**
     * 流式聊天接口 (SSE)
     */
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStream(@RequestBody AgentChatRequest request) {
        SseEmitter emitter = new SseEmitter(300_000L); // 5分钟超时

        String chatId = request.getChatId();
        List<Map<String, String>> history = chatHistories.computeIfAbsent(
                chatId, k -> new ArrayList<>());
        appendHistory(chatId, "user", request.getMessage());
        ToolCallback[] selectedTools = filterTools(request.getToolIds());

        // 异步执行流式输出
        new Thread(() -> {
            try {
                ChatClient chatClient = qinglanManus.getChatClient();
                var prompt = chatClient.prompt();
                if (request.getSystemPrompt() != null && !request.getSystemPrompt().isBlank()) {
                    prompt.system(request.getSystemPrompt());
                }
                prompt.user(request.getMessage())
                        .toolCallbacks(selectedTools)
                        .stream()
                        .chatResponse()
                        .doOnNext(response -> {
                            AssistantMessage msg = response.getResult().getOutput();
                            try {
                                if (msg.getText() != null && !msg.getText().isBlank()) {
                                    appendHistory(chatId, "assistant", msg.getText());
                                    emitter.send(SseEmitter.event()
                                            .name("message")
                                            .data(msg.getText()));
                                }
                                if (msg.getToolCalls() != null) {
                                    msg.getToolCalls().forEach(toolCall -> {
                                        try {
                                            emitter.send(SseEmitter.event()
                                                    .name("tool_call")
                                                    .data(OBJECT_MAPPER.writeValueAsString(Map.of(
                                                            "name", toolCall.name(),
                                                            "arguments", toolCall.arguments()))));
                                        } catch (IOException ex) {
                                            log.error("tool_call 事件序列化失败", ex);
                                        }
                                    });
                                }
                            } catch (IOException e) {
                                log.error("SSE发送失败", e);
                            }
                        })
                        .doOnError(e -> {
                            try {
                                emitter.send(SseEmitter.event()
                                        .name("error")
                                        .data(e.getMessage()));
                                emitter.completeWithError(e);
                            } catch (IOException ex) {
                                log.error("SSE完成失败", ex);
                            }
                        })
                        .doOnComplete(() -> {
                            try {
                                emitter.send(SseEmitter.event()
                                        .name("complete")
                                        .data(""));
                                emitter.complete();
                            } catch (IOException e) {
                                log.error("SSE完成失败", e);
                            }
                        })
                        .subscribe();

            } catch (Exception e) {
                log.error("流式处理异常", e);
                try {
                    emitter.send(SseEmitter.event()
                            .name("error")
                            .data("处理异常: " + e.getMessage()));
                    emitter.complete();
                } catch (IOException ex) {
                    log.error("SSE错误发送失败", ex);
                }
            }
        }).start();

        return emitter;
    }

    /**
     * 获取可用工具列表
     */
    @GetMapping("/tools")
    public List<ToolInfo> getTools() {
        return List.of(allTools).stream()
                .map(tool -> {
                    String toolName = tool.getToolDefinition().name();
                    return ToolInfo.builder()
                            .id(toolName.toLowerCase(Locale.ROOT).replace("tool", ""))
                            .name(toolName)
                            .description(tool.getToolDefinition().description())
                            .category(categorizeTool(toolName))
                            .enabled(true)
                            .build();
                })
                .collect(Collectors.toList());
    }

    /**
     * 获取对话历史
     */
    @GetMapping("/history/{chatId}")
    public List<Map<String, String>> getHistory(@PathVariable String chatId) {
        return chatHistories.getOrDefault(chatId, new ArrayList<>());
    }

    /**
     * 清空会话
     */
    @DeleteMapping("/history/{chatId}")
    public Map<String, String> clearHistory(@PathVariable String chatId) {
        chatHistories.remove(chatId);
        return Map.of("message", "会话已清空", "chatId", chatId);
    }

    // ========== 私有方法 ==========

    private String executeAgent(AgentChatRequest request, ToolCallback[] selectedTools,
                                List<Map<String, String>> history) {
        ChatClient chatClient = qinglanManus.getChatClient();
        var prompt = chatClient.prompt();
        if (request.getSystemPrompt() != null && !request.getSystemPrompt().isBlank()) {
            prompt.system(request.getSystemPrompt());
        }
        prompt.user(request.getMessage());

        StringBuilder result = new StringBuilder();

        prompt.toolCallbacks(selectedTools)
                .stream()
                .chatResponse()
                .doOnNext(response -> {
                    AssistantMessage msg = response.getResult().getOutput();
                    if (msg.getText() != null && !msg.getText().isBlank()) {
                        result.append(msg.getText());
                        appendHistory(request.getChatId(), "assistant", msg.getText());
                    }
                    if (msg.getToolCalls() != null) {
                        msg.getToolCalls().forEach(toolCall -> appendHistory(request.getChatId(), "tool", toolCall.name() + ": " + toolCall.arguments()));
                    }
                })
                .blockLast(); // 等待完成

        return result.toString();
    }

    private List<AgentChatResponse.ToolCallInfo> extractToolCalls(List<Map<String, String>> history) {
        return history.stream()
                .filter(entry -> "tool".equals(entry.get("role")))
                .map(entry -> {
                    String content = entry.get("content");
                    String[] parts = content.split(":", 2);
                    return AgentChatResponse.ToolCallInfo.builder()
                            .toolName(parts[0])
                            .arguments(parts.length > 1 ? parts[1].trim() : "")
                            .result("")
                            .build();
                })
                .collect(Collectors.toList());
    }

    private ToolCallback[] filterTools(List<String> toolIds) {
        if (toolIds == null || toolIds.isEmpty()) {
            return allTools;
        }
        return List.of(allTools).stream()
                .filter(tool -> {
                    String toolName = tool.getToolDefinition().name().toLowerCase(Locale.ROOT);
                    String toolId = toolName.replace("tool", "");
                    return toolIds.stream()
                            .map(id -> id.toLowerCase(Locale.ROOT))
                            .anyMatch(id -> id.equals(toolId) || toolName.contains(id));
                })
                .toArray(ToolCallback[]::new);
    }

    private void appendHistory(String chatId, String role, String content) {
        if (chatId == null || chatId.isBlank() || content == null || content.isBlank()) {
            return;
        }
        chatHistories.computeIfAbsent(chatId, k -> new ArrayList<>())
                .add(Map.of("role", role, "content", content));
    }

    private String categorizeTool(String toolName) {
        String lower = toolName.toLowerCase(Locale.ROOT);
        if (lower.contains("search") || lower.contains("google"))
            return "search";
        if (lower.contains("code"))
            return "code";
        if (lower.contains("file"))
            return "file";
        if (lower.contains("law"))
            return "law";
        if (lower.contains("image"))
            return "image";
        return "other";
    }
}