package com.lx.aisuperagent.config;

import com.lx.aisuperagent.advisor.MyLoggerAdvisor;
import com.lx.aisuperagent.agent.QinglanManus;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.ToolCallAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

import java.util.Optional;

@Configuration
public class qinglanManusConfig {

    // 1. 手动创建一个 ChatMemoryRepository Bean (使用内存实现)
    @Bean
    public InMemoryChatMemoryRepository chatMemoryRepository() {
        return new InMemoryChatMemoryRepository();
    }

    // 2. 基于上面的 Repository 来构建 ChatMemory Bean
    @Bean
    public ChatMemory chatMemory(InMemoryChatMemoryRepository chatMemoryRepository) {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(chatMemoryRepository)
                .maxMessages(10)  // 根据需要调整窗口大小
                .build();
    }

    @Bean
    public QinglanManus qinglanManus(
            ChatClient.Builder builder,
            ToolCallback[] allTools,
            ChatMemory chatMemory,
            Optional<SyncMcpToolCallbackProvider> mcpToolCallbackProvider
    ) {
        // 1. 创建ToolCallingManager
        ToolCallingManager toolCallingManager = ToolCallingManager.builder()
                .build();

        // 2. 创建ToolCallAdvisor，设置最大轮次
        ToolCallAdvisor toolCallAdvisor = ToolCallAdvisor.builder()
                .toolCallingManager(toolCallingManager)
                .advisorOrder(Ordered.HIGHEST_PRECEDENCE + 300)  // 执行顺序
                .build();

        // 3. 创建ChatMemory Advisor
        MessageChatMemoryAdvisor memoryAdvisor = MessageChatMemoryAdvisor.builder(chatMemory).build();

        // 4. 构建ChatClient
        ChatClient chatClient = builder
                
                .defaultAdvisors(toolCallAdvisor, memoryAdvisor)  // 添加Advisor
                .defaultAdvisors(new MyLoggerAdvisor())
                .build();

        return new QinglanManus(chatClient, allTools, mcpToolCallbackProvider.orElse(null));
    }
}
