package com.lx.aisuperagent.rag;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.spec.DashScopeModel;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.document.Document;
import org.springframework.ai.model.transformer.SummaryMetadataEnricher;
import org.springframework.ai.model.transformer.SummaryMetadataEnricher.SummaryType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.util.List;

@Configuration
class EnricherConfig {

    @Bean
    public SummaryMetadataEnricher summaryMetadata(DashScopeChatModel dashScopeModel) {
        return new SummaryMetadataEnricher( dashScopeModel,
            List.of(SummaryType.PREVIOUS, SummaryType.CURRENT, SummaryType.NEXT));
    }
}
