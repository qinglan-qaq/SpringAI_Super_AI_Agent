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
public class AgentChatResponse {
    private String content;           // AI回复内容
    private String chatId;            // 会话ID
    private boolean isComplete;       // 是否完成
    private List<ToolCallInfo> toolCalls; // 工具调用信息
    private String error;             // 错误信息

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ToolCallInfo {
        private String toolName;
        private String arguments;
        private String result;
    }
}
