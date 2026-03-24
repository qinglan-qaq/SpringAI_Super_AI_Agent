package com.lx.aisuperagent.rag;


import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.document.Document;
import org.springframework.ai.model.transformer.KeywordMetadataEnricher;
import org.springframework.ai.model.transformer.SummaryMetadataEnricher;
import org.springframework.ai.model.transformer.SummaryMetadataEnricher.SummaryType;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MyDocumentEnricher {
    private final ChatModel chatModel;


    public MyDocumentEnricher(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    /**
     * 提取关键字
     * 关键字创建键值对,保存在元数据中
     * 本质上是给AI添加额外的提示词生成
     * @param documents
     * @return
     */
    List<Document> enrichDocumentsByKeyword(List<Document> documents) {

        KeywordMetadataEnricher enricher = KeywordMetadataEnricher
                .builder(chatModel)
                .keywordCount(5)
                .build();
        return enricher.apply(documents);
    }

    /**
     * 摘要增强
     * 生成文档摘要
     * 生成考虑上下文的摘要,前者后者和当前文档
     * 本质上是给AI添加额外的提示词生成
     * @param documents
     * @return
     */
    List<Document> enrichDocumentsBySummary(List<Document> documents) {
        SummaryMetadataEnricher enricher = new SummaryMetadataEnricher(chatModel,
                List.of(SummaryType.PREVIOUS, SummaryType.CURRENT, SummaryType.NEXT));
        return enricher.apply(documents);
    }
}
