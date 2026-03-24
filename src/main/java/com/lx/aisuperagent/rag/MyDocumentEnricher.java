package com.lx.aisuperagent.rag;


import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.document.Document;
import org.springframework.ai.model.transformer.KeywordMetadataEnricher;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MyDocumentEnricher {
    private final ChatModel chatModel;


    public MyDocumentEnricher(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    List<Document> enrichDocumentsByKeyword(List<Document> documents) {
        KeywordMetadataEnricher enricher = KeywordMetadataEnricher
                .builder(chatModel)
                .keywordCount(5)
                .build();
        return enricher.apply(documents);
    }
}
