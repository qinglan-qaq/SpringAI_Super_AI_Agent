package com.lx.qinglan_imagesearch_mcp;

import com.lx.qinglan_imagesearch_mcp.tools.ImageSearchTool;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class QinglanImageSearchMcpApplication {

    public static void main(String[] args) {
        SpringApplication.run(QinglanImageSearchMcpApplication.class, args);
    }

    //    Bean注册工具
    @Bean
    public ToolCallbackProvider imageSearchTools(ImageSearchTool imageSearchTool) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(imageSearchTool)
                .build();
    }

}
