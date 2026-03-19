package com.lx.aisuperagent.advisor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClientMessageAggregator;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import reactor.core.publisher.Flux;

/**
 * 自定义Advisor
 * 打印输出info日志 只输出单词用户提示词和AI回复的文本
 */
public class MyLoggerAdvisor implements CallAdvisor, StreamAdvisor {

    /**
     * 定义静态不可变的工厂类 用于获取logger实例
     * 接受一个class参数 用以确定当前日志来自哪个类
     */
    private static final Logger logger = LoggerFactory.getLogger(MyLoggerAdvisor.class);

    //	获取名字
    @Override
    public String getName() {
        return this.getClass().getSimpleName();
    }

    //获取优先级 数字越小优先级越高 (Lower values execute first.)
    @Override
    public int getOrder() {
        return 0;
    }

    private ChatClientRequest before(ChatClientRequest request) {

        logger.info("AI Request.content:{} ", request.context());
        logger.info("AI Request:{} ", request);
        return request;
    }

    private ChatClientResponse after(ChatClientResponse response) {
        logger.info("AI Request:{} ", response.chatResponse().getResult().getOutput().getText());
        return response;
    }


    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
//      处理当前请求
        logRequest(request);
//      调用下一个请求
        ChatClientResponse chatClientResponse = chain.nextCall(request);
//      处理下一个
        logResponse(chatClientResponse);

        return chatClientResponse;
    }

    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest request, StreamAdvisorChain chain) {
//        处理当前请求
        logRequest(request);
//      流式处理下一个
        Flux<ChatClientResponse> chatClientResponses = chain.nextStream(request);

        /**
         * Helper that for streaming chat responses, aggregate the chat response messages into a single AssistantMessage.
         * 辅助类 在处理流式聊天响应（Streaming）时，将多个分块的 ChatClientResponse 聚合为一个完整的 AssistantMessage
         */
        Flux<ChatClientResponse> responseFlux = new ChatClientMessageAggregator()
                .aggregateChatClientResponse(chatClientResponses, this::logResponse);
        return responseFlux;
    }

    private void logRequest(ChatClientRequest request) {
        logger.debug("request: {}", request);
    }

    private void logResponse(ChatClientResponse response) {
        logger.debug("response: {}", response);
    }

}