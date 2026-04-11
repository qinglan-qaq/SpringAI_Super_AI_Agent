package com.lx.aisuperagent.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lx.aisuperagent.agent.QinglanManus;
import com.lx.aisuperagent.agent.model.AgentState;
import com.lx.aisuperagent.app.LawApp;
import com.lx.aisuperagent.controller.dto.AgentChatRequest;
import com.lx.aisuperagent.controller.dto.AgentChatResponse;
import com.lx.aisuperagent.controller.dto.ToolInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
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

    private final LawApp lawApp;

    private final ToolCallback[] allTools;

    // 会话管理
    private final Map<String, List<Map<String, String>>> chatHistories = new ConcurrentHashMap<>();

    /**
     * 模式选择分流
     * 执行不同的问答方式
     * @param request
     * @return
     */
    @PostMapping("/chat/sync")
    public AgentChatResponse chatSync(@RequestBody AgentChatRequest request) {
        log.info("收到同步请求: chatId={}, message={}",
                request.getChatId(), request.getMessage());

        List<Map<String, String>> history = chatHistories
                .computeIfAbsent(request.getChatId(), k -> new ArrayList<>());

        appendHistory(request.getChatId(), "user", request.getMessage());

        try {
            String result;
            List<AgentChatResponse.ToolCallInfo> toolcalls = new ArrayList<>();

            // 模式选择
            if (request.isAgentMode()) {

                // 模式1：智能体Agent模式 - 使用QinglanManus的run方法
                result = executeAgentMode(request, history, toolcalls);

            } else if (request.isUseRAG()) {

                // 模式2：知识库RAG模式 - 使用LawApp的doChatWithRAG
                result = executeRAGMode(request, history);

            } else if (request.isUseCloudRAG()) {

                // 模式3：云知识库模式 - 使用LawApp的doChatWithCloudRAG
                result = executeCloudRAGMode(request, history);

            } else {

                // 模式4：普通聊天模式 - 使用LawApp的doChat
                result = executeNormalMode(request);

            }


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
     * RAG模式执行 - 使用本地向量库
     *
     * @params request
     * @params history
     *
     */
    private String executeRAGMode(AgentChatRequest request,
                                  List<Map<String, String>> history) {
        int topK = request.getRagTop_k() > 0 ? request.getRagTop_k() : 5;
        double threshold = request.getRagThreshold() > 0 ? request.getRagThreshold() : 0.5;

        // 使用LawApp的doChatWithRAGCustom（自定义参数）
        ChatResponse response = lawApp.doChatWithRAGCustom(
                request.getMessage(),
                request.getChatId(),
                topK,
                threshold
        );

        String content = response.getResult().getOutput().getText();
        appendHistory(request.getChatId(), "assistant", content);

        // 记录Token消耗
        Usage usage = response.getMetadata().getUsage();
        log.info("RAG模式Token消耗: 输入={}, 输出={}, 总计={}",
                usage.getPromptTokens(),
                usage.getCompletionTokens(),
                usage.getTotalTokens());

        return content;
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

    /**
     * 执行调用qinglanManus
     *
     * @param request
     * @param history
     * @param toolCalls
     * @return
     */
    private String executeAgentMode(AgentChatRequest request,
                                    List<Map<String, String>> history,
                                    List<AgentChatResponse.ToolCallInfo> toolCalls) {

        // 调用QinglanManus的run方法，走think-act循环
        String result = qinglanManus.run(request.getMessage());

        // 从Agent的messageList中提取思考过程
        List<Message> agentMessages = qinglanManus.getMessageList();
        for (Message msg : agentMessages) {
            if (msg instanceof AssistantMessage assistantMsg) {
                // 记录助手的思考
                if (assistantMsg.getText() != null) {
                    appendHistory(request.getChatId(), "assistant_thinking",
                            assistantMsg.getText());
                }
                // 记录工具调用
                if (assistantMsg.getToolCalls() != null) {
                    assistantMsg.getToolCalls().forEach(tc -> {
                        toolCalls.add(AgentChatResponse.ToolCallInfo.builder()
                                .toolName(tc.name())
                                .arguments(tc.arguments())
                                .result("")
                                .build());
                        appendHistory(request.getChatId(), "tool_call",
                                tc.name() + ": " + tc.arguments());
                    });
                }
            } else if (msg instanceof ToolResponseMessage toolResp) {
                // 记录工具执行结果
                toolResp.getResponses().forEach(resp -> {
                    appendHistory(request.getChatId(), "tool_result",
                            resp.name() + " → " + resp.responseData());
                });
            }
        }

        // 清理Agent的上下文（为下次调用准备）
        qinglanManus.setMessageList(new ArrayList<>());
        qinglanManus.setState(AgentState.IDLE);

        return result;
    }

    /**
     * 使用云知识库
     *
     * @param request
     * @param history
     * @return
     */
    private String executeCloudRAGMode(AgentChatRequest request, List<Map<String, String>> history) {
        ChatResponse response = lawApp.doChatWithCloudRAG(
                request.getMessage(),
                request.getChatId()
        );

        String content = response.getResult().getOutput().getText();
        appendHistory(request.getChatId(), "assistant", content);

        return content;
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

    /**
     * 执行普通的聊天方式
     *
     * @param request
     * @return
     */
    private String executeNormalMode(AgentChatRequest request) {
        return lawApp.doChat(request.getMessage(), request.getChatId());
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