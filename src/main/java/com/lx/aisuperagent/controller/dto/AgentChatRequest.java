package com.lx.aisuperagent.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AgentChatRequest {
    private String message;           // 用户消息
    private String chatId;            // 会话ID
    private List<String> toolIds;     // 启用的工具ID列表
    private boolean agentMode;        // 是否启用Agent模式
    private String systemPrompt;      // 自定义系统提示词
}
