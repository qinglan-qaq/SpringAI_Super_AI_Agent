package com.lx.aisuperagent.rag;

import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MyTokenTextSplitter {
    /**
     * 未指定为默认值
     * @param documents
     * @return
     */
    public List<Document> splitDocuments(List<Document> documents) {

        TokenTextSplitter splitter = new TokenTextSplitter();
        return splitter.apply(documents);
    }

    /**
     * 具体参数设置详见原码
     * @param documents
     * @returnd
     */
    public List<Document> splitCustomized(List<Document> documents) {
//        自定义参数
        TokenTextSplitter splitter = new TokenTextSplitter(1000, 300, 20, 5000, true);

        return splitter.apply(documents);
    }
}
