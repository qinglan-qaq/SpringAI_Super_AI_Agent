package com.lx.aisuperagent.Tools;

import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 注册所有可用的工具
 * MCP工具有Spring自动处理
 * 注意使用```.ToolCallbacks()``` 调用
 * 返回的ToolCallback会被Spring AI自动用于：
 * 1. 生成函数调用Schema（让LLM知道有哪些工具可用）
 * 2. 处理LLM的函数调用请求
 * 3. 返回工具执行结果给LLM
 *
 * @return ToolCallback数组，包含所有工具的回调实例
 *
 */
@Configuration
public class ToolRegistration {

    @Value("${search-api.api-key}")
    private String searchApiKey;

    @Bean
    public ToolCallback[] allTools() {
        FileOptionTool fileOptionTool = new FileOptionTool();
        TerminateTool terminateTool = new TerminateTool();
        GoogleWebSearchTool googleWebSearchTool = new GoogleWebSearchTool(searchApiKey);
        PDFGenerationTool pdfGenerationTool = new PDFGenerationTool();

        return ToolCallbacks.from(
                fileOptionTool,
                terminateTool,
                googleWebSearchTool,
                pdfGenerationTool
        );
    }
}
