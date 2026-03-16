package com.lx.aisuperagent.demo.invoke;

import dev.langchain4j.community.model.dashscope.QwenChatModel;

public class LangchainAIInvoke {

    public static void main(String[] args) {
//        这个调用也很简洁
        QwenChatModel qwenChatModel = QwenChatModel.builder()
                .apiKey(TestApiKey.apiKey)
                .modelName("qwen-plus")
                .build();
        String chat = qwenChatModel.chat("以莫言的口吻解释,在长时间艰辛的努力后仍然失败的原因,一百字以内");
        System.out.println(chat);
    }
}
