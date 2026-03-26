package com.lx.aisuperagent.rag.config;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import org.springframework.ai.model.transformer.SummaryMetadataEnricher;
import org.springframework.ai.model.transformer.SummaryMetadataEnricher.SummaryType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
class EnricherConfig {
    @Bean
    public SummaryMetadataEnricher summaryMetadata(DashScopeChatModel dashScopeModel) {
        return new SummaryMetadataEnricher( dashScopeModel,
            List.of(SummaryType.PREVIOUS, SummaryType.CURRENT, SummaryType.NEXT));
    }
}
