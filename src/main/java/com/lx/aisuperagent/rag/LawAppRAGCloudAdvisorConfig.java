package com.lx.aisuperagent.rag;


import com.alibaba.cloud.ai.dashscope.api.DashScopeAgentApi;
import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.rag.DashScopeDocumentRetrievalAdvisor;
import com.alibaba.cloud.ai.dashscope.rag.DashScopeDocumentRetriever;
import com.alibaba.cloud.ai.dashscope.rag.DashScopeDocumentRetrieverOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class LawAppRAGCloudAdvisorConfig {

    @Value("${spring.ai.dashscop.api.key}")
    private String dashscopeApiKey;

    @Bean
    public Advisor lawAppRAGCloudAdvisor() {
        DashScopeApi dashScopeApi = new DashScopeApi(dashscopeApiKey);

        final String KNOWLEDGE_INDEX = "RAG云知识库";
        DashScopeDocumentRetrievalAdvisor documentRetriever = new DashScopeDocumentRetriever(
                dashScopeApi,
                DashScopeDocumentRetrieverOptions.builder()
                        .withIndexName(KNOWLEDGE_INDEX)
                        .build());
        return RetrievalAugmentationAdvisor.builder()
                .documentRetriever(documentRetriever)
                .build();
    }

}
