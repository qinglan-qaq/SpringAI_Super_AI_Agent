package com.lx.aisuperagent.agent;

import cn.hutool.core.collection.CollUtil;
import com.alibaba.cloud.ai.dashscope.agent.DashScopeAgentOptions;

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


import java.util.List;
import java.util.stream.Collectors;

/**
 * 处理工具调用的基础类, 实现think 和 act方法
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Slf4j
public class ToolCallAgent extends ReActAgent {

    //      可用的工具列表
    private final ToolCallback[] availableTools;
    //      保存了工具调用信息的响应
    private ChatResponse toolCallChatResponse;
    //      工具调用管理者
    private final ToolCallingManager toolCallingManager;
    //      自己维护上下文
    private final ChatOptions chatOptions;

    public ToolCallAgent(ToolCallback[] availableTools) {
        super();
        this.availableTools = availableTools;
        this.toolCallingManager = ToolCallingManager.builder().build();
        this.chatOptions = DashScopeChatOptions.builder()
//                .enableInternalToolExecution(false)
                .build();
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
            //      配置对应的有工具调用的
            ChatResponse chatResponse = getChatClient().prompt(prompt)
                    .system(getSystemPrompt())
                    .toolCallbacks(availableTools)
                    .options(DashScopeChatOptions.builder().withTopP(0.85).build())
                    .call()
                    .chatResponse();

            //      记录响应
            this.toolCallChatResponse = chatResponse;

            AssistantMessage assistantMessage = chatResponse.getResult().getOutput();
            //      记录提示信息
            String result = assistantMessage.getText();

            List<AssistantMessage.ToolCall> toolCallList = assistantMessage.getToolCalls();

            log.info(getName() + "的思考" + result);

            log.info("{} 选择了 {} 个工具", getName(), toolCallList != null ? toolCallList.size() : 0);

            String toolCallInfo = toolCallList
                    .stream()
                    .map(toolCall -> String.format("工具名称: %s,参数: %s",
                            toolCall.name(),
                            toolCall.arguments())
                    )
                    .collect(Collectors.joining("\n"));
            log.info(toolCallInfo);
            //      调用工具不使用时记录助手消息
            if (toolCallList.isEmpty()) {
                getMessageList().add(assistantMessage);
                return false;
            } else {
                return true;
            }
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
                .map(response -> "工具" + response.name() + "完成了任务喵~😎😎😎成果为: " + response.responseData())
                .collect(Collectors.joining("\n"));

        //        匹配任何有执行terminate的函数, 判断是否为true,改变状态
        boolean terminateToolCalled = toolResponseMessage.getResponses().stream()
                .anyMatch(toolResponse -> "doTerminate".equals(toolResponse.name()));
        if (terminateToolCalled) {
            setState(AgentState.FINISHED);
        }

        log.info(results);
        return results;
    }
}

