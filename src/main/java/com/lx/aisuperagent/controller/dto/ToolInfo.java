package com.lx.aisuperagent.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToolInfo {
    private String id;                // 工具唯一ID
    private String name;              // 工具名称
    private String description;       // 工具描述
    private String category;          // 工具分类
    private boolean enabled;          // 是否启用
}