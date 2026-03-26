package com.lx.aisuperagent.app;


import com.lx.aisuperagent.chatmemory.FileBaseChatMemory;
import com.lx.aisuperagent.rag.LawAppVectorStoreConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import com.lx.aisuperagent.advisor.MyLoggerAdvisor;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;

import java.util.List;


@Component
@Slf4j
public class LawApp {
    //    使用构造器初始化
    private final ChatClient chatClient;
    private final VectorStore lawAppVectorStore;
    public static final String SYSTEM_PROMPT =
            "你是温柔甜美可爱大方成熟性感的大姐姐形象,同时是一个专业的法律顾问AI，名为“AI私人法务”，精通中国法律体系" +
                    "你的回答应当专业、清晰、易于理解，同时始终保持礼貌、耐心和同理心，让用户感受到被重视和支持。";

    record LawReport(String title, List<String> suggestions) {

    }

    /**
     * Lawapp的构造函数 实现多种定义和预设
     *
     * @param dashscopeChatModel
     */
    public LawApp(ChatModel dashscopeChatModel, VectorStore lawAppVectorStore) {

//        构造函数注入
        this.lawAppVectorStore = lawAppVectorStore;

//        初始化基于文件的对话记忆
        String fileDir = System.getProperty("user.dir") + "/chat_memory";
        FileBaseChatMemory chatMemory = new FileBaseChatMemory(fileDir);
//        实现多轮记忆存储
//        ChatMemory chatMemory = MessageWindowChatMemory.builder().build();
//       可以对全局启用预设 也可以对单次使用预设
        chatClient = ChatClient.builder(dashscopeChatModel)
                .defaultSystem(SYSTEM_PROMPT)
                .defaultAdvisors(
                        /**
                         *  真正保存在ChatMemory中 MessageChatMemoryAdvisor只是管理z
                         *  MessageChatMemoryAdvisor.builder(chatMemory).build(),
                         *  自定义增强Advisor
                         *  new ReReadingAdvisor()
                         */
                        new MyLoggerAdvisor(),
//                        对话记忆保存在文件中
                        MessageChatMemoryAdvisor.builder(chatMemory).build()
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
                .advisors(advisorSpec -> advisorSpec.param(ChatMemory.CONVERSATION_ID, chatId))
                .call()
                .chatResponse();

        String content = response.getResult().getOutput().getText();
        Usage usage = response.getMetadata().getUsage();
        log.info("usage-token:Input:{},Output:{},Total:{}"
                , usage.getPromptTokens()
                , usage.getCompletionTokens()
                , usage.getTotalTokens());
        log.info("content:{}", content);


        return content;
    }

    /**
     * 带有logger的调用方法
     *
     * @param message
     * @param chatId
     * @return
     */
    public LawReport doChatWithReport(String message, String chatId) {
        LawReport lawReport = chatClient
                .prompt()
                .system(SYSTEM_PROMPT + "每次对话后都要生成法律分析报告，标题为{用户名}的法律报告，内容为建议列表")
                .user(message)
                .advisors(advisorSpec -> advisorSpec.param(ChatMemory.CONVERSATION_ID, chatId))
                .advisors(new MyLoggerAdvisor())
                .call()
                .entity(LawReport.class);
        log.info("content:{}", lawReport);
        return lawReport;
    }

    /**
     * 查询增强 使用向量数据库对问题检索相关文档
     *
     * @param message
     * @param chatId
     * @return
     */
    public ChatResponse doChatWithRAG(String message, String chatId) {
        ChatResponse lawReport = chatClient
                .prompt()
                .system(SYSTEM_PROMPT)
                .user(message)
                .advisors(advisorSpec -> advisorSpec.param(ChatMemory.CONVERSATION_ID, chatId))
                .advisors(new MyLoggerAdvisor())
                .advisors(QuestionAnswerAdvisor.builder(lawAppVectorStore).build())
                .call()
                .chatResponse();
        Usage usage = lawReport.getMetadata().getUsage();
        log.info("Token 消耗详情: 输入={}, 输出={}, 总计={}",
                usage.getPromptTokens(),
                usage.getNativeUsage(),
                usage.getTotalTokens());
        log.info("content:{}", lawReport);

        return lawReport;
    }


}





















