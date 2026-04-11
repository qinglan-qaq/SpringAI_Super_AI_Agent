package com.lx.aisuperagent.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Agent聊天响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentChatResponse {
    /**
     * 响应内容
     */
    private String content;

    /**
     * 会话ID
     */
    private String chatId;

    /**
     * 是否完成
     */
    private Boolean isComplete;

    /**
     * 工具调用信息
     */
    private List<ToolCallInfo> toolCalls;

    /**
     * 错误信息
     */
    private String error;

    /**
     * 思考过程
     */
    private List<ThinkingStep> thinkingProcess;


    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ThinkingStep {
        private int stepNumber;
        private String thought;      // 思考内容
        private String action;       // 执行的动作
        private String observation;  // 观察到的结果
    }


    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ToolCallInfo {
        private String toolName;
        private String arguments;
        private String result;
    }
}