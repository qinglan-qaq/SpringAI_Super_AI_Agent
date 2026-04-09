package com.lx.aisuperagent.controller.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Builder
@Data
public class AgentChatRequest {
    private String message;           // 用户消息
    private String chatId;            // 会话ID
    private List<String> toolIds;      // 启用的工具ID列表
    private boolean agentMode;        // 是否启用Agent模式
}
