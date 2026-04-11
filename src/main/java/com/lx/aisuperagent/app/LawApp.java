package com.lx.aisuperagent.app;


import com.lx.aisuperagent.chatmemory.FileBaseChatMemory;
import com.lx.aisuperagent.rag.RAG_Service;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.memory.ChatMemory;
import com.lx.aisuperagent.advisor.MyLoggerAdvisor;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.document.Document;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.List;


@Component
@Slf4j
public class LawApp {
    //    使用构造器初始化
    private final ChatClient chatClient;

//    private final VectorStore lawAppVectorStore; 云向量模型

    public static final String SYSTEM_PROMPT =
            "你是温柔甜美可爱大方成熟性感的大姐姐形象,同时是一个专业的法律顾问AI，名为“AI私人法务”，精通中国法律体系" +
                    "你的回答应当专业、清晰、易于理解，同时始终保持礼貌、耐心和同理心，让用户感受到被重视和支持。";

    //注入RAG服务
    @Resource
    private RAG_Service.RagService ragService;

    // 云知识库Advisor
    @Resource
    private Advisor lawAppRAGCloudAdvisor;

    // RAG初始化标志
    private volatile boolean ragInitialized = false;

    // 最大上下文字符数（约1000 token）
    private static final int MAX_CONTEXT_CHARS = 2000;

    //      添加对MCP工具调用
    private final SyncMcpToolCallbackProvider toolCallbackProvider;

    //      本地工具作为备用方案
    private final ToolCallback[] allTools;

    record LawReport(String title, List<String> suggestions) {

    }

    /**
     * Lawapp的构造函数 实现多种定义和预设
     *
     * @param dashscopeChatModel
     * @param syncMcpToolCallbackProvider
     * @param allTools
     */
    public LawApp(ChatModel dashscopeChatModel,
                  RAG_Service.RagService ragService,
                  SyncMcpToolCallbackProvider syncMcpToolCallbackProvider,
                  ToolCallback[] allTools) {

//        向量数据库构造函数注入
//        this.lawAppVectorStore = lawAppVectorStore;
        this.toolCallbackProvider = syncMcpToolCallbackProvider;
        this.allTools = allTools;
        this.ragService = ragService;

//        初始化基于文件的对话记忆
        String fileDir = System.getProperty("user.dir") + "/chat_memory";
        FileBaseChatMemory chatMemory_file = new FileBaseChatMemory(fileDir);

//        实现多轮记忆存储
        ChatMemory chatMemory = MessageWindowChatMemory.builder().build();
//       可以对全局启用预设 也可以对单次使用预设
        chatClient = ChatClient.builder(dashscopeChatModel)
                .defaultSystem(SYSTEM_PROMPT)
                .defaultAdvisors(
                        /**
                         *  真正保存在ChatMemory中 MessageChatMemoryAdvisor只是管理
                         *  MessageChatMemoryAdvisor.builder(chatMemory).build(),
                         *  自定义增强Advisor
                         *  new ReReadingAdvisor()
                         */
                        new MyLoggerAdvisor(),
//                        对话记忆保存在文件中
                        MessageChatMemoryAdvisor.builder(chatMemory_file).build(),
                        MessageChatMemoryAdvisor.builder(chatMemory).build()
                )
                .build();

        // 构造器启动时初始化向量库
        try {
            this.ragService.initVectorStore();
            log.info("RAG向量库初始化完成");
        } catch (Exception e) {
            log.warn("RAG向量库初始化失败，将使用降级方案: {}", e.getMessage());
        }

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

        Usage usage = response.getMetadata().getUsage();
        log.info("usage-token:Input:{},Output:{},Total:{}"
                , usage.getPromptTokens()
                , usage.getCompletionTokens()
                , usage.getTotalTokens());

        String content = response.getResult().getOutput().getText();
        log.info("content:{}", content);

        return content;
    }

    /**
     * 带有MyLoggerAdvisor的调用方法
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
     * 初始化RAG向量库
     * 调用RagService的initVectorStore方法
     * 需要在应用启动或需要更新知识库时调用
     */
    public void initRAGVectorStore() {
        log.info("开始初始化RAG向量库...");
        try {
            ragService.initVectorStore();
            log.info("RAG向量库初始化成功");
        } catch (Exception e) {
            log.error("RAG向量库初始化失败", e);
            throw new RuntimeException("RAG向量库初始化失败", e);
        }
    }

    /**
     * 查询增强 使用RAG_Service进行高级语义查询获取相关文档
     * 集成SearchWitchCustomer方法实现完整的RAG功能(使用默认参数)
     *
     * @param message
     * @param chatId
     * @return
     */
    public ChatResponse doChatWithRAG(String message, String chatId) {
        initRAGVectorStore();
        return doChatWithRAGCustom(message, chatId, 5, 0.5);
    }

    /**
     * 查询增强 使用RAG_Service进行高级语义查询获取相关文档
     * 集成SearchWitchCustomer方法实现完整的RAG功能(自定义参数)
     *
     * @param message             用户输入消息
     * @param chatId              对话ID
     * @param topK                返回的相关文档数量
     * @param similarityThreshold 相似度阈值（0-1）
     * @return
     */
    public ChatResponse doChatWithRAGCustom(
            String message,
            String chatId,
            int topK,
            double similarityThreshold) {
        log.info("开始执行RAG增强对话，消息: {}, 对话ID: {}, topK: {}, threshold: {}",
                message, chatId, topK, similarityThreshold);

        // 使用RagService进行高级语义查询获取相关文档
        List<Document> relevantDocuments = ragService.SearchWitchCustomer(
                message,
                topK,  // 返回的相关文档数量
                similarityThreshold // 相似度阈值
        );

        log.info("RAG检索到相关文档数量: {}", relevantDocuments.size());

        // 2. 构建上下文（带长度控制）
        String context = buildContextWithLimit(relevantDocuments); // 限制2000字符

        // 3. 构建增强提示词
        String enhancedUserMessage = context.isEmpty() ? message :
                String.format("【参考资料】\n%s\n\n【用户问题】%s", context, message);

        // 4. 调用ChatClient进行对话
        return chatClient
                .prompt()
                .system(SYSTEM_PROMPT)
                .user(enhancedUserMessage)
                .advisors(advisorSpec -> advisorSpec.param(ChatMemory.CONVERSATION_ID, chatId))
                .advisors(new MyLoggerAdvisor())
                .call()
                .chatResponse();
    }


    /**
     * 使用云知识库实现RAG
     *
     * @param message
     * @param chatId
     * @return
     */
    public ChatResponse doChatWithCloudRAG(String message, String chatId) {

        ChatResponse lawReport = chatClient
                .prompt()
                .system(SYSTEM_PROMPT)
                .user(message)
                .advisors(advisorSpec -> advisorSpec.param(ChatMemory.CONVERSATION_ID, chatId))
                .advisors(new MyLoggerAdvisor())
//                使用云知识库
                .advisors(lawAppRAGCloudAdvisor)
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

    /**
     * 工具调用
     *
     * @return
     */
    private ToolCallback[] getToolCallbacks() {
        // 如果 MCP 服务可用，使用 MCP 工具；否则 fallback 到本地工具
        if (toolCallbackProvider != null) {
            return toolCallbackProvider.getToolCallbacks();
        }
        log.warn("MCP 服务未启用，使用本地工具");
        return allTools != null ? allTools : new ToolCallback[0];
    }

    /**
     * 使用MCP图片搜索工具
     *
     * @param message
     * @param chatId
     * @return
     */
    @Tool
    public String doChatWithMcp(String message, String chatId) {
        ToolCallback[] tools = getToolCallbacks();
        if (tools == null || tools.length == 0) {
            log.warn("没有可用的工具");
            return "错误：MCP 工具不可用，且本地工具为空";
        }
        ChatResponse response = chatClient
                .prompt()
                .user(message)
                .advisors(advisorSpec -> advisorSpec.param(ChatMemory.CONVERSATION_ID, chatId))
                .advisors(new MyLoggerAdvisor())
                //      启用MCP服务或本地工具
                .toolCallbacks(tools)
                .call()
                .chatResponse();

        Usage usage = response.getMetadata().getUsage();
        log.info("Token 消耗详情: 输入={}, 输出={}, 总计={} \n",
                usage.getPromptTokens(),
                usage.getNativeUsage(),
                usage.getTotalTokens());

        String content = response.getResult().getOutput().getText();
        log.info("content:{}", content);
        return content;
    }

    /**
     * 工具调用
     *
     * @param message
     * @param chatId
     * @return
     */
    public String doChatWithTools(String message, String chatId) {
        if (toolCallbackProvider == null) {
            log.warn("MCP 工具提供者为 null，无法使用 .tools() 方法");
            return "错误：MCP 工具提供者未初始化";
        }
        ChatResponse response = chatClient
                .prompt()
                .user(message)
                .advisors(advisorSpec -> advisorSpec.param(ChatMemory.CONVERSATION_ID, chatId))
                .advisors(new MyLoggerAdvisor())
//                启用MCP服务
                .tools(toolCallbackProvider)
                .call()
                .chatResponse();

        Usage usage = response.getMetadata().getUsage();
        log.info("Token 消耗详情: 输入={}, 输出={}, 总计={} \n",
                usage.getPromptTokens(),
                usage.getNativeUsage(),
                usage.getTotalTokens());

        String content = response.getResult().getOutput().getText();
        log.info("content:{}", content);
        return content;
    }

    /**
     * 流式输出
     *
     * @param message
     * @param chatId
     * @return
     */
    public Flux<String> doChatByStream(String message, String chatId) {
        return chatClient
                .prompt()
                .user(message)
                .advisors(advisorSpec -> advisorSpec.param(ChatMemory.CONVERSATION_ID, chatId))
                .stream()
                .content();
    }

    /**
     * 本地RAG向量数据库流式输出
     *
     * @param message
     * @param chatId
     * @param topK
     * @param threshold
     * @return
     */
    public Flux<String> doChatWithLocalRAGStream(
            String message,
            String chatId,
            int topK,
            double threshold) {
        // 先检索
        List<Document> docs = ragService.SearchWitchCustomer(message, topK, threshold);

        String context = buildContextWithLimit(docs);
        String enhancedMessage = context.isEmpty() ? message :
                String.format("【参考资料】\n%s\n\n【用户问题】%s", context, message);

        // 流式输出
        return chatClient.prompt()
                .system(SYSTEM_PROMPT)
                .user(enhancedMessage)
                .advisors(advisorSpec -> advisorSpec.param(ChatMemory.CONVERSATION_ID, chatId))
                .stream()
                .content();
    }

    /**
     * 限制上下文的长度
     *
     * @param docs
     * @return
     */
    private String buildContextWithLimit(List<Document> docs) {
        if (docs.isEmpty()) return "";

        StringBuilder sb = new StringBuilder();
        sb.append("检索到相关法律知识：\n");

        int totalChars = 0;
        for (int i = 0; i < docs.size() && totalChars < 2000; i++) {
            String text = docs.get(i).getText();
            // 截断单个文档
            if (text.length() > 2000 / 2) {
                text = text.substring(0, 2000 / 2) + "...";
            }
            sb.append("【").append(i + 1).append("】").append(text).append("\n");
            totalChars += text.length();
        }
        return sb.toString();
    }


}





















