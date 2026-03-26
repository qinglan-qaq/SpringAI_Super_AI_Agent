package com.lx.aisuperagent.rag;

import org.springframework.ai.document.Document;
import org.springframework.ai.model.transformer.SummaryMetadataEnricher;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
class MySummaryEnricher {

    private final SummaryMetadataEnricher enricher;

    MySummaryEnricher(SummaryMetadataEnricher enricher) {
        this.enricher = enricher;
    }

    List<Document> enrichDocuments(List<Document> documents) {
        return this.enricher.apply(documents);
    }
}