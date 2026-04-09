package com.lx.aisuperagent.controller;

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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/agent")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class AgentController {

    private final QinglanManus qinglanManus;
    private final ToolCallback[] allTools;

    // 会话管理：chatId -> 消息列表
    private final Map<String, List<AssistantMessage>> chatHistories = new ConcurrentHashMap<>();

    /**
     * 同步聊天接口
     */
    @PostMapping("/chat/sync")
    public AgentChatResponse chatSync(@RequestBody AgentChatRequest request) {
        log.info("收到同步请求: chatId={}, message={}", request.getChatId(), request.getMessage());
        try {
            // 获取或创建会话历史
            List<AssistantMessage> history = chatHistories.computeIfAbsent(
                    request.getChatId(), k -> new ArrayList<>()
            );

            // 执行Agent
            String result = executeAgent(request.getMessage(), history);

            return AgentChatResponse.builder()
                    .content(result)
                    .chatId(request.getChatId())
                    .isComplete(true)
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
        List<AssistantMessage> history = chatHistories.computeIfAbsent(
                chatId, k -> new ArrayList<>()
        );

        // 异步执行流式输出
        new Thread(() -> {
            try {
                ChatClient chatClient = qinglanManus.getChatClient();

                chatClient.prompt()
                        .user(request.getMessage())
                        .toolCallbacks(allTools)
                        .stream()
                        .chatResponse()
                        .doOnNext(response -> {
                            AssistantMessage msg = response.getResult().getOutput();
                            try {
                                if (msg.getText() != null) {
                                    emitter.send(SseEmitter.event()
                                            .name("message")
                                            .data(msg.getText()));
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
        List<ToolInfo> tools = new ArrayList<>();

        // 从 allTools 动态获取工具信息
        for (ToolCallback tool : allTools) {
            String toolName = tool.getToolDefinition().name();
            String description = tool.getToolDefinition().description();

            // 根据工具名称分类
            String category = categorizeTool(toolName);

            tools.add(ToolInfo.builder()
                    .id(toolName.toLowerCase().replace("tool", ""))
                    .name(toolName)
                    .description(description)
                    .category(category)
                    .enabled(true)
                    .build());
        }

        return tools;
    }

    /**
     * 获取对话历史
     */
    @GetMapping("/history/{chatId}")
    public List<Map<String, String>> getHistory(@PathVariable String chatId) {
        List<AssistantMessage> history = chatHistories.get(chatId);
        if (history == null) {
            return new ArrayList<>();
        }

        return history.stream()
                .filter(msg -> msg.getText() != null && !msg.getText().isEmpty())
                .map(msg -> Map.of(
                        "role", "assistant",
                        "content", msg.getText()
                ))
                .toList();
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

    private String executeAgent(String message, List<AssistantMessage> history) {
        ChatClient chatClient = qinglanManus.getChatClient();

        StringBuilder result = new StringBuilder();

        chatClient.prompt()
                .user(message)
                .toolCallbacks(allTools)
                .stream()
                .chatResponse()
                .doOnNext(response -> {
                    AssistantMessage msg = response.getResult().getOutput();
                    if (msg.getText() != null) {
                        result.append(msg.getText());
                    }
                    if (msg.getToolCalls() != null && !msg.getToolCalls().isEmpty()) {
                        history.add(msg);
                    }
                })
                .blockLast(); // 等待完成

        return result.toString();
    }

    private String categorizeTool(String toolName) {
        String lower = toolName.toLowerCase();
        if (lower.contains("search")) return "search";
        if (lower.contains("code")) return "code";
        if (lower.contains("file")) return "file";
        if (lower.contains("law")) return "law";
        if (lower.contains("image")) return "image";
        return "other";
    }
}