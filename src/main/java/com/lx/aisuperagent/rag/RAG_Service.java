package com.lx.aisuperagent.rag;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;

public class RAG_Service {

    /**
     * RAG服务
     */
    @Service
    @Slf4j
    public static class RagService {

        @Resource
        private VectorStore pgVectorStore;

        @Resource
        private LawAppDocumentLoader documentLoader;

        @Resource
        private Better_MyTokenTextSplitter textSplitter;

        @Resource
        private MyDocumentEnricher documentEnricher;

        /**
         * 初始化向量库
         */
        public void initVectorStore() {
            log.info("开始初始化向量库...");

            // 1. 加载文档
            List<Document> documents = documentLoader.loadMarkdowns();
            log.info("加载文档数量: {}", documents.size());

            // 2. 使用字符递归分块
            List<Document> chunks = textSplitter.apply(documents);

            log.info("分块后数量: {}", chunks.size());

            // 3. 增强元数据（可选，耗时较长）
            chunks = documentEnricher.enrichDocumentsBySummary(chunks);

            // 4. 存入向量库
            pgVectorStore.add(chunks);

            log.info("向量库初始化完成");
        }

        /**
         * 添加新文档
         */
        public void addDocument(Document document) {
            pgVectorStore.add(List.of(document));
        }

        /**
         * 批量添加文档
         */
        public void addDocuments(List<Document> documents) {
            pgVectorStore.add(documents);
        }
    }

}
