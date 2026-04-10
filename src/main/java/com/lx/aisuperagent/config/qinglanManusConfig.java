package com.lx.aisuperagent.config;

import com.lx.aisuperagent.agent.QinglanManus;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.ToolCallAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

@Configuration
public class qinglanManusConfig {

    @Bean
    public QinglanManus qinglanManus(
            ChatClient.Builder builder,
            ToolCallback[] allTools,
            ChatMemory chatMemory
    ) {
        // 1. 创建ToolCallingManager
        ToolCallingManager toolCallingManager = ToolCallingManager.builder()
                .build();

        // 2. 创建ToolCallAdvisor，设置最大轮次
        ToolCallAdvisor toolCallAdvisor = ToolCallAdvisor.builder()
                .toolCallingManager(toolCallingManager)
                .advisorOrder(Ordered.HIGHEST_PRECEDENCE + 300)  // 执行顺序
                .conversationHistoryEnabled(false)  // 关闭内部历史，避免重复记录
                .build();

        // 3. 创建ChatMemory Advisor
        MessageChatMemoryAdvisor memoryAdvisor = new MessageChatMemoryAdvisor(chatMemory);

        // 4. 构建ChatClient
        ChatClient chatClient = builder
                .defaultToolCallbacks(allTools)  // 注册工具
                .defaultAdvisors(toolCallAdvisor, memoryAdvisor)  // 添加Advisor
                .build();

        return new QinglanManus(chatClient);
    }
}
