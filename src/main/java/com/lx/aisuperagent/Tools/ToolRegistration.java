package com.lx.aisuperagent.Tools;

import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ToolRegistration {

//    @Value("${search-api.api-key}")
//    private String searchApiKey;

    @Bean
    public ToolCallback[] allTools() {
        FileOptionTool fileOptionTool = new FileOptionTool();
        TerminateTool terminateTool = new TerminateTool();
        GoogleWebSearchTool googleWebSearchTool = new GoogleWebSearchTool;
        PDFGenerationTool pdfGenerationTool = new PDFGenerationTool();
        return ToolCallbacks.from(
                fileOptionTool,
                terminateTool,
                googleWebSearchTool,
                pdfGenerationTool

        );
    }
}
