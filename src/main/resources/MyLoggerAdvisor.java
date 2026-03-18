package com.lx.aisuperagent.advisor;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

/**
 * 自定义日志 Advisor
 * 适配 Spring AI 1.x 版本，打印用户提示词和 AI 回复
 */
@Slf4j
public class MyLoggerAdvisor implements  CallAdvisor, StreamAdvisor  {

    @Override
    public String getName() {
        return this.getClass().getSimpleName();
    }

    @Override
    public int getOrder() {
        // 建议优先级设置，0 通常代表较高优先级
        return 0;
    }

    /**
     * 前置处理：提取用户发送的文本
     */
    private AdvisedRequest before(AdvisedRequest request) {
        // 在最新版本中，request.userText() 依然可用
        log.info("AI Request: {}", request.userText());
        return request;
    }

    /**
     * 后置处理：提取 AI 返回的文本
     */
    private void observeAfter(AdvisedResponse advisedResponse) {
        if (advisedResponse != null && advisedResponse.response() != null) {
            String content = advisedResponse.response().getResult().getOutput().getText();
            log.info("AI Response: {}", content);
        }
    }


    public AdvisedResponse aroundCall(AdvisedRequest advisedRequest, CallAroundAdvisorChain chain) {
        // 1. 请求前执行
        AdvisedRequest processedRequest = this.before(advisedRequest);

        // 2. 传递给执行链
        AdvisedResponse advisedResponse = chain.nextAroundCall(processedRequest);

        // 3. 响应后执行
        this.observeAfter(advisedResponse);

        return advisedResponse;
    }


    public Flux<AdvisedResponse> aroundStream(AdvisedRequest advisedRequest, StreamAroundAdvisorChain chain) {
        // 1. 请求前执行
        AdvisedRequest processedRequest = this.before(advisedRequest);

        // 2. 处理流式响应
        // 使用 MessageAggregator 将流式的 ChatCompletionChunk 聚合，以便在流结束时调用 observeAfter
        return new MessageAggregator().aggregateAdvisedResponse(
                chain.nextAroundStream(processedRequest),
                this::observeAfter
        );
    }


}