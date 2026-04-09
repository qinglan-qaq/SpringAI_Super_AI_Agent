package com.lx.aisuperagent.controller.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SseEvent {
    private String eventType;         // "message" | "tool_call" | "complete" | "error"
    private String content;           // 事件内容
    private Object data;              // 扩展数据
}