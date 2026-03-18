package com.lx.aisuperagent.app;

import com.lx.aisuperagent.advisor.MyLoggerAdvisor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;

import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Component;

import java.util.List;

import static jdk.vm.ci.hotspot.HotSpotJVMCICompilerFactory.CompilationLevel.Simple;

@Component
@Slf4j
public class LawApp {
    //    使用构造器初始化
    private final ChatClient chatClient;

    record LawReport(String title, List<String> suggestions) {
    }


    public static final String SYSTEM_PROMPT = "你是一个专业的法律顾问AI，名为“AI私人法务”，精通中国法律体系。" +
            "你的任务是为用户提供初步的法律咨询和建议，帮助他们理解自己的法律处境和可行的行动方案。" +
            "在对话中，你需要通过引导性问题逐步深入了解用户的具体情况，模拟真实法律咨询场景。" +
            "你的回答应当专业、清晰、易于理解，同时始终保持礼貌、耐心和同理心，让用户感受到被重视和支持。";

    public LawApp(ChatModel dashscopeChatModel) {

        ChatMemory chatMemory = MessageWindowChatMemory.builder().build();
//       可以对全局启用预设 也可以对单次使用预设
        this.chatClient = ChatClient.builder(dashscopeChatModel)
                .defaultSystem(SYSTEM_PROMPT)
                .defaultAdvisors(
//                        真正保存在ChatMemory中 MessageChatMemoryAdvisor只是管理
                        MessageChatMemoryAdvisor.builder(chatMemory).build()
//                        new MyLoggerAdvisor()
//                        自定义增强Advisor
//                        new ReReadingAdvisor()

                )
                .build();
    }

    /**
     * Ai 对话支持对话记忆ChatMemory
     *
     * @param message
     * @param chatId
     * @return
     */
    public String doChat(String message, String chatId) {
        ChatResponse response = chatClient
                .prompt()
                .user(message)
//                新版本写法
                .advisors(advisorSpec -> advisorSpec
                        .param(MessageChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(MessageChatMemoryAdvisor.CHAT_MEMORY_RETRIEVE_SIZE_KEY,10)
                )
                .call()
                .chatResponse();
        String content = response.getResult().getOutput().getText();
        log.info("content:{}", content);
        return content;
    }

    public LawReport doChatWithReport (String message, String chatId) {
        LawReport lawReport = chatClient
                .prompt()
                .system(SYSTEM_PROMPT + "每次对话后都要生成恋爱结果，标题为{用户名}的恋爱报告，内容为建议列表")
                .user(message)
//                新版本写法
                .advisors(advisorSpec -> advisorSpec
//                        新版本写法静态类都写在MessageChatMemoryAdvisor下面
                        .param(MessageChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(MessageChatMemoryAdvisor.CHAT_MEMORY_RETRIEVE_SIZE_KEY,10)

                )
                .call()
                .entity(LawReport.class);

        log.info("content:{}", lawReport);
        return lawReport;
    }



}





















