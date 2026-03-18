package com.lx.aisuperagent.advisor;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.prompt.PromptTemplate;
import reactor.core.publisher.Flux;

import java.util.Map;

/**
 * 自定义 Re2 Advisor（重读提示）
 * 通过将用户原始问题重复一遍，提升大语言模型的推理能力。
 * 完全适配 Spring AI 1.1.2，支持自定义提示模板和顺序。
 */
public class ReReadingAdvisor implements CallAdvisor, StreamAdvisor {

    private static final String DEFAULT_RE2_ADVISE_TEMPLATE = """
            {re2_input_query}
            Read the question again: {re2_input_query}
            """;

    private final String re2AdviseTemplate;
    private int order = 0;

    public ReReadingAdvisor() {
        this(DEFAULT_RE2_ADVISE_TEMPLATE);
    }

    public ReReadingAdvisor(String re2AdviseTemplate) {
        this.re2AdviseTemplate = re2AdviseTemplate;
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        ChatClientRequest modifiedRequest = doBefore(request);
        return chain.nextCall(modifiedRequest);
    }

    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest request, StreamAdvisorChain chain) {
        ChatClientRequest modifiedRequest = doBefore(request);
        return chain.nextStream(modifiedRequest);
    }

    /**
     * 在调用前修改请求，将原始用户文本按模板进行增强。
     */
    private ChatClientRequest doBefore(ChatClientRequest request) {
        // 获取原始用户文本模板
        String originalUserText = request.prompt().getUserMessages().toString();
        if (originalUserText == null || originalUserText.isBlank()) {
            return request; // 没有用户文本则无需增强
        }

        // 使用模板和原始文本作为变量，生成增强后的用户文本
        String augmentedUserText = PromptTemplate.builder()
                .template(this.re2AdviseTemplate)
                .variables(Map.of("re2_input_query", originalUserText))
                .build()
                .render();

        // 保留原始参数，使原始模板中的占位符能够正常解析
        Map<String, Object> originalParams = request.prompt().getUserMessage();

        // 构造新的请求：替换用户文本为增强后的文本，保留原始参数
        return ChatClientRequest.from(request)
                .userText(augmentedUserText)
                .userParams(originalParams)
                .build();
    }

    @Override
    public int getOrder() {
        return this.order;
    }

    public ReReadingAdvisor withOrder(int order) {
        this.order = order;
        return this;
    }

    @Override
    public String getName() {
        return this.getClass().getSimpleName();
    }
}