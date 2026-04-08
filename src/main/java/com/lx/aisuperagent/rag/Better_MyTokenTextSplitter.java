package com.lx.aisuperagent.rag;

import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 使用Builder模式,链式调用
 * 提供了更加灵活的api接口
 * 能自定义更多的文档分割细节
 */
@Component
public class Better_MyTokenTextSplitter {
    public List<Document> splitWithBuilder(List<Document> documents) {
        TokenTextSplitter splitter = TokenTextSplitter.builder()
                .withChunkSize(1000)
                .withMinChunkSizeChars(400)
                .withMinChunkLengthToEmbed(10)
                .withMaxNumChunks(5000)
                .withKeepSeparator(true)
                .build();

        return splitter.apply(documents);
    }
}

