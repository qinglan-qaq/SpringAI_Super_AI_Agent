package com.lx.aisuperagent.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 历史消息DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HistoryItem {
    /**
     * 角色：user/assistant
     * 用户或者AI
     */
    private String role;

    /**
     * 消息内容
     */
    private String content;
}