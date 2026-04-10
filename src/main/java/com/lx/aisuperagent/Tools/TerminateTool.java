package com.lx.aisuperagent.Tools;

import org.springframework.ai.tool.annotation.Tool;

/**
 * 任务终止工具
 */
public class TerminateTool {
    @Tool(name = "terminate", description = "Call this tool when the task is completed or cannot continue. The agent should stop executing further steps.")
    public String terminate() {
        return "任务结束";
    }
}
