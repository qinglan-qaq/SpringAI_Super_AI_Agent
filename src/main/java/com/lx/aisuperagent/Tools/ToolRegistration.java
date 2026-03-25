//package com.lx.aisuperagent.Tools;
//
//import org.springframework.ai.support.ToolCallbacks;
//import org.springframework.ai.tool.ToolCallback;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//
//@Configuration
//public class ToolRegistration {
//
//    @Value("${search-api.api-key}")
//    private String searchApiKey;
//
//    @Bean
//    public ToolCallback[] allTools() {
//        FileOperationTool fileOperationTool = new FileOperationTool();
//
//        return ToolCallbacks.from(
//            fileOperationTool,
//
//        );
//    }
//}
