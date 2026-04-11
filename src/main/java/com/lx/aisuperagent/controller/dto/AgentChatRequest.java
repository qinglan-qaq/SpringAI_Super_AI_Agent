package com.lx.aisuperagent.controller.dto;

import lombok.Data;
import java.util.List;

/**
 * 聊天请求DTO
 */
@Data
public class AgentChatRequest {
    /**
     * 用户消息
     */
    private String message;

    /**
     * 会话ID
     */
    private String chatId;

    /**
     * 选择的工具ID列表（可选）
     */
    private List<String> toolIds;

    /**
     * 是否启用Agent模式（可选）
     */
    private boolean agentMode;

    /**
     * 自定义系统提示词（可选）
     */
    private String systemPrompt;

    /**
     * 是否使用RAG
     */
    private boolean useRAG;

    /**
     * 是否使用云知识库
     */
    private boolean useCloudRAG;

    /**
     * 检索前几的文档
     */
    private int ragTop_k;

    /**
     * RAG相似度阈值
     */
    private double ragThreshold;

}