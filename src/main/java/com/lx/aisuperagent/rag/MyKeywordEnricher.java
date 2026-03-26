package com.lx.aisuperagent.rag;


import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.document.Document;
import org.springframework.ai.model.transformer.KeywordMetadataEnricher;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
class MyKeywordEnricher {

    private final ChatModel chatModel;

    MyKeywordEnricher(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    /**
     * 传入多个文档接入大模型自动输出5个关键字
     * @param documents
     * @return
     */
    List<Document> enrichDocuments(List<Document> documents) {
        KeywordMetadataEnricher enricher = KeywordMetadataEnricher.builder(chatModel)
                .keywordCount(5)
                .build();

        // Or use custom templates
//        KeywordMetadataEnricher enricher = KeywordMetadataEnricher.builder(chatModel)
//               .keywordsTemplate(YOUR_CUSTOM_TEMPLATE)
//               .build();

        return enricher.apply(documents);
    }
}