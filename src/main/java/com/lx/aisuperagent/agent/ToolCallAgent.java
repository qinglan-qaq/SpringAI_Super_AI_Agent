package com.lx.aisuperagent.agent;

import com.alibaba.cloud.ai.dashscope.agent.DashScopeAgentOptions;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.tool.ToolCallback;

/**
 * 处理工具调用的基础类, 实现think 和 act方法
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Slf4j
public class ToolCallAgent extends ReActAgent {
    //    可用的工具列表
    private final ToolCallback[] availableTools;
    //保存了工具调用信息的响应
    private ChatResponse toolCallChatResponse;
    //工具调用管理者
    private final ToolCallingManager toolCallingManager;
    //自己维护上下文
    private final ChatOptions chatOptions;

    public ToolCallAgent(ToolCallback[] availableTools) {
        super();
        this.availableTools = availableTools;
        this.toolCallingManager = ToolCallingManager.builder().build();
        this.chatOptions = DashScopeAgentOptions.builder()
                .build();

    }


    @Override
    public boolean think() {
        return false;
    }

    @Override
    public String act() {
        return "";
    }
}

