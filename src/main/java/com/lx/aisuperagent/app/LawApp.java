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
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.stream.Collectors;


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
     * @param ragService
     */
    public LawApp(ChatModel dashscopeChatModel,
                  RAG_Service ragService,
                  SyncMcpToolCallbackProvider syncMcpToolCallbackProvider,
                  ToolCallback[] allTools) {

//        向量数据库构造函数注入
//        this.lawAppVectorStore = lawAppVectorStore;
        this.toolCallbackProvider = syncMcpToolCallbackProvider;
        this.allTools = allTools;
        this.ragService.initVectorStore();

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
     * @param message 用户输入消息
     * @param chatId 对话ID
     * @param topK 返回的相关文档数量
     * @param similarityThreshold 相似度阈值（0-1）
     * @return
     */
    public ChatResponse doChatWithRAGCustom(String message, String chatId, int topK, double similarityThreshold) {
        log.info("开始执行RAG增强对话，消息: {}, 对话ID: {}, topK: {}, threshold: {}", 
                message, chatId, topK, similarityThreshold);

        // 1. 使用RagService进行高级语义查询获取相关文档
        List<Document> relevantDocuments = ragService.SearchWitchCustomer(
                message,
                topK,  // 返回的相关文档数量
                similarityThreshold // 相似度阈值
        );

        log.info("RAG检索到相关文档数量: {}", relevantDocuments.size());

        // 2. 构建RAG上下文，将检索到的文档内容融入提示词
        StringBuilder contextBuilder = new StringBuilder();
        if (!relevantDocuments.isEmpty()) {
                
            contextBuilder.append("\n\n【检索到的相关法律知识库内容】\n");

            contextBuilder.append("=".repeat(50)).append("\n");
            
            for (int i = 0; i < relevantDocuments.size(); i++) {

                Document doc = relevantDocuments.get(i);

                contextBuilder.append(String.format("【文档 %d】\n", i + 1));
                
                contextBuilder.append(doc.getText());

                if (doc.getMetadata() != null && !doc.getMetadata().isEmpty()) {

                    contextBuilder.append("\n【元数据】");

                    doc.getMetadata().forEach((key, value) -> 
                        contextBuilder.append("\n  ").append(key).append(": ").append(value)
                    );

                }
                contextBuilder.append("\n").append("-".repeat(50)).append("\n");
            }
        } else {
            log.warn("未检索到相关文档，仅使用用户输入进行回答");
        }

        // 3. 构建完整的用户提示词（包含RAG上下文）
        String enhancedUserMessage = message + contextBuilder.toString();

        // 4. 调用ChatClient进行对话
        ChatResponse lawReport = chatClient
                .prompt()
                .system(SYSTEM_PROMPT)
                .user(enhancedUserMessage)
                .advisors(advisorSpec -> advisorSpec.param(ChatMemory.CONVERSATION_ID, chatId))
                .advisors(new MyLoggerAdvisor())
                .call()
                .chatResponse();

        Usage usage = lawReport.getMetadata().getUsage();
        log.info("Token 消耗详情: 输入={}, 输出={}, 总计={}",
                usage.getPromptTokens(),
                usage.getNativeUsage(),
                usage.getTotalTokens());
        log.info("RAG对话完成，内容: {}", lawReport);

        return lawReport;
    }

    /**
     * 使用云知识库
     *
     * @param message
     * @param chatId
     * @return
     */

    //注意Bean容器的注入
    @Resource
    private Advisor lawAppRAGCloudAdvisor;

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


    private ToolCallback[] getToolCallbacks() {
        // 如果 MCP 服务可用，使用 MCP 工具；否则 fallback 到本地工具
        if (toolCallbackProvider != null) {
            return toolCallbackProvider.getToolCallbacks();
        }
        log.warn("MCP 服务未启用，使用本地工具");
        return allTools != null ? allTools : new ToolCallback[0];
    }

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


//    @Resource
//    private ToolCallbackProvider toolCallbacks;

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
}





















