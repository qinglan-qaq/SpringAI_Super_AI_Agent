package com.lx.aisuperagent;

import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class TestVectorStoreConfig {

    @Bean
    @Primary  // 确保这个 Bean 优先于原有的 Bean
    public VectorStore lawAppVectorStore() {
        // 使用内存向量存储，不依赖任何外部服务
        return new SimpleVectorStore(new org.springframework.ai.vectorstore.InMemoryVectorStore());
    }
}