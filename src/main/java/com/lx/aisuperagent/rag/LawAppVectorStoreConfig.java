package com.lx.aisuperagent.rag;

import jakarta.annotation.Resource;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.util.List;

@Configuration
public class LawAppVectorStoreConfig {

    @Resource
    private LawAppDocumentLoader lawAppDocumentLoader;

    /**
     * 注意这里Bean容器需要注入
     * @param dashscopeEmbeddingModel
     * @return
     */
    @Bean
    VectorStore lawAppVectorStore(EmbeddingModel dashscopeEmbeddingModel) {
        SimpleVectorStore simpleVectorStore = SimpleVectorStore.builder(dashscopeEmbeddingModel)
                .build();
        /**
         * 注意不要导错包
         * 加载导入文档
         */
        List<Document> documents = lawAppDocumentLoader.loadMarkdowns();
        simpleVectorStore.add(documents);

        return simpleVectorStore;

    }


}
