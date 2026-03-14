package com.lx.aisuperagent.demo.invoke;

import dev.langchain4j.community.model.dashscope.QwenChatModel;

public class LangchainAIInvoke {

    public static void main(String[] args) {
//        这个调用也很简洁
        QwenChatModel qwenChatModel = QwenChatModel.builder()
                .apiKey(TestApiKey.apiKey)
                .modelName("qwen-plus")
                .build();
        String chat = qwenChatModel.chat("qwen-plus和qwen-max谁更好 区别在哪 我之前问max说自己最好");
        System.out.println(chat);
    }
}
