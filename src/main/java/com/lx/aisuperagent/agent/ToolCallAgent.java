package com.lx.aisuperagent.agent;

import cn.hutool.core.collection.CollUtil;


import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.lx.aisuperagent.agent.model.AgentState;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * 处理工具调用的基础类, 实现think 和 act方法
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Slf4j
public class ToolCallAgent extends ReActAgent {

    //      可用的工具列表
    private final ToolCallback[] availableTools;
    //      MCP 工具提供者（可选）
    private final SyncMcpToolCallbackProvider mcpToolCallbackProvider;
    //      保存了工具调用信息的响应
    private ChatResponse toolCallChatResponse;
    //      工具调用管理者
    private final ToolCallingManager toolCallingManager;
    //      自己维护上下文
    private final ChatOptions chatOptions;

    public ToolCallAgent(ToolCallback[] availableTools) {
        super();
        this.availableTools = availableTools;
        this.mcpToolCallbackProvider = null;
        this.toolCallingManager = ToolCallingManager.builder().build();
        this.chatOptions = DashScopeChatOptions.builder()
//                .enableInternalToolExecution(false)
                .build();
    }

    public ToolCallAgent(ToolCallback[] availableTools, SyncMcpToolCallbackProvider mcpToolCallbackProvider) {
        super();
        this.availableTools = availableTools;
        this.mcpToolCallbackProvider = mcpToolCallbackProvider;
        this.toolCallingManager = ToolCallingManager.builder().build();
        this.chatOptions = DashScopeChatOptions.builder()
//                .enableInternalToolExecution(false)
                .build();
    }

    /**
     * 合并本地工具和 MCP 工具
     */
    private ToolCallback[] getAllTools() {
        List<ToolCallback> toolList = new ArrayList<>();

        // 添加本地工具
        if (availableTools != null && availableTools.length > 0) {
            toolList.addAll(Arrays.asList(availableTools));
        }

        // 添加 MCP 工具（如果可用）
        if (mcpToolCallbackProvider != null) {
            try {
                ToolCallback[] mcpTools = mcpToolCallbackProvider.getToolCallbacks();
                if (mcpTools != null && mcpTools.length > 0) {
                    toolList.addAll(Arrays.asList(mcpTools));
                    log.info("成功加载 {} 个 MCP 工具", mcpTools.length);
                }
            } catch (Exception e) {
                log.warn("MCP 工具加载失败: {}", e.getMessage());
            }
        }

        return toolList.toArray(new ToolCallback[0]);
    }

    /**
     * 处理当前装填并决定下一步行动
     *
     * @return
     */
    @Override
    public boolean think() {
        //      若下一个提示词不为空,新建对象并加入消息列表
        if (getNextStepPrompt() != null && !getNextStepPrompt().isEmpty()) {

            UserMessage userMessage = new UserMessage(getNextStepPrompt());

            getMessageList().add(userMessage);

        }
        List<Message> messageList = getMessageList();
        Prompt prompt = new Prompt(messageList, chatOptions);
        try {
            //      配置对应的有工具调用的（合并本地工具和 MCP 工具）
            ToolCallback[] allTools = getAllTools();
            ChatResponse chatResponse = getChatClient().prompt(prompt)
                    .system(getSystemPrompt())
                    .toolCallbacks(allTools)
                    .call()
                    .chatResponse();

            //      记录响应
            this.toolCallChatResponse = chatResponse;

            AssistantMessage assistantMessage = chatResponse.getResult().getOutput();
            //      记录提示信息
            String result = assistantMessage.getText();

            List<AssistantMessage.ToolCall> toolCallList = assistantMessage.getToolCalls();

            log.info(getName() + "的思考" + result);
            int callCount = toolCallList == null ? 0 : toolCallList.size();
            log.info("{} 选择了 {} 个工具", getName(), callCount);

            if (callCount > 0) {
                String toolCallInfo = toolCallList
                        .stream()
                        .map(toolCall -> String.format("工具名称: %s, 参数: %s",
                                toolCall.name(),
                                toolCall.arguments())
                        )
                        .collect(Collectors.joining("\n"));
                log.info(toolCallInfo);
                return true;
            }

            //      如果没有工具调用但模型明确表示任务已完成，则提前结束代理
            if (result != null) {
                String lowerResult = result.toLowerCase(Locale.ROOT);
                if (lowerResult.contains("任务结束") || lowerResult.contains("已完成") || lowerResult.contains("完成任务") || lowerResult.contains("结束任务") || lowerResult.contains("任务已完成")) {
                    setState(AgentState.FINISHED);
                    log.info("{} 检测到结束信号，已提前停止任务。", getName());
                }
            }

            getMessageList().add(assistantMessage);
            return false;
        } catch (Exception e) {
            log.error(getName() + "思考过程遇到困难" + e.getMessage());
            getMessageList().add(new AssistantMessage("处理时遇到错误: " + e.getMessage()));
            return false;
        }
    }

    /**
     * 执行工具调用并处理结果
     *
     * @return 执行结果
     */
    @Override
    public String act() {
        if (!toolCallChatResponse.hasToolCalls()) {
            return "没用工具调用";
        }
        //      调用工具
        Prompt prompt = new Prompt(getMessageList(), chatOptions);

        ToolExecutionResult toolExecutionResult = toolCallingManager.executeToolCalls(prompt, toolCallChatResponse);
        //      记录上下文conversationHistory包含助手信息和返回结果
        setMessageList(toolExecutionResult.conversationHistory());
        //      当前工具调用的结果
        ToolResponseMessage toolResponseMessage = (ToolResponseMessage) CollUtil.getLast(toolExecutionResult.conversationHistory());

        String results = toolResponseMessage
                .getResponses()
                .stream()
                .map(response -> "工具调用: " + response.name() + "，结果: " + response.responseData())
                .collect(Collectors.joining("\n"));

        // 匹配任何包含 terminate 的工具调用，允许提前结束任务
        boolean terminateToolCalled = toolResponseMessage.getResponses().stream()
                .anyMatch(toolResponse -> toolResponse.name() != null && toolResponse.name().equalsIgnoreCase("terminate"));
        if (terminateToolCalled) {
            setState(AgentState.FINISHED);
            results = results + "\n任务已提前终止。";
        }

        log.info(results);
        return results;
    }
}

