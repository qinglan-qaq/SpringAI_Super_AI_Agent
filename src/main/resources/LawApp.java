package com.lx.aisuperagent.app;

import com.lx.aisuperagent.advisor.MyLoggerAdvisor;
import com.lx.aisuperagent.advisor.ReReadingAdvisor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
public class LawApp {

    private final ChatClient chatClient;

    record LawReport(String title, List<String> suggestions) {}

    public static final String SYSTEM_PROMPT = "你是一个专业的法律顾问AI，名为“AI私人法务”，精通中国法律体系。" +
            "你的任务是为用户提供初步的法律咨询和建议，帮助他们理解自己的法律处境和可行的行动方案。" +
            "在对话中，你需要通过引导性问题逐步深入了解用户的具体情况，模拟真实法律咨询场景。" +
            "你的回答应当专业、清晰、易于理解，同时始终保持礼貌、耐心和同理心，让用户感受到被重视和支持。";

    public LawApp(ChatModel dashscopeChatModel) {
        ChatMemory chatMemory = MessageWindowChatMemory.builder().build();

        this.chatClient = ChatClient.builder(dashscopeChatModel)
                .defaultSystem(SYSTEM_PROMPT)
                .defaultAdvisors(
                        // 使用构造函数创建 MessageChatMemoryAdvisor
                        new MessageChatMemoryAdvisor(chatMemory),
                        new ReReadingAdvisor()
                        // 可取消注释以启用日志
                        // new MyLoggerAdvisor()
                )
                // 移除多余的 .advisors() 调用
                .build();
    }

    /**
     * AI 对话支持对话记忆
     */
    public String doChat(String message, String chatId) {
        ChatResponse response = chatClient
                .prompt()
                .user(message)
                .advisors(advisorSpec -> advisorSpec
                        // 使用正确的常量键名
                        .param(MessageChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(MessageChatMemoryAdvisor.CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10)
                )
                .call()
                .chatResponse();
        String content = response.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;
    }

    /**
     * 返回结构化报告
     */
    public LawReport doChatWithReport(String message, String chatId) {
        LawReport lawReport = chatClient
                .prompt()
                // 如果 {用户名} 需要动态替换，请使用 systemParams
                .system(spec -> spec
                        .text(SYSTEM_PROMPT + "每次对话后都要生成恋爱结果，标题为{username}的恋爱报告，内容为建议列表")
                        .param("username", chatId) // 示例：用chatId作为用户名
                )
                .user(message)
                .advisors(advisorSpec -> advisorSpec
                        .param(MessageChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(MessageChatMemoryAdvisor.CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10)
                )
                .call()
                .entity(LawReport.class);
        log.info("lawReport: {}", lawReport);
        return lawReport;
    }
}