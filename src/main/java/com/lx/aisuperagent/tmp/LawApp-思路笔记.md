# LawApp 形成思路笔记

对应代码: `src/main/java/com/lx/aisuperagent/app/LawApp.java`

## 1. 真实开发需求
- 希望提供一个可直接落地的法律咨询核心服务，不只是单轮问答。
- 需要同时具备: 多轮记忆、日志审计、RAG检索增强、云知识库增强、工具调用、流式输出。
- 要求 Controller 层调用简单，业务能力集中在一个应用服务中编排。

## 2. 设计思路
- 用 `ChatClient` 作为统一调用入口，把通用能力放进默认配置。
- 用 `MessageChatMemoryAdvisor` 承接会话记忆，让每次请求按 `chatId` 关联上下文。
- 用 Advisor 组合能力: 自定义日志、向量检索、云检索。
- 提供多个 `doChat*` 方法映射不同业务场景，避免 Controller 写复杂分支。

## 3. 构造函数的形成过程
- 先注入 `ChatModel` 与 `VectorStore`，确保模型能力和检索能力都可用。
- 本地落地记忆采用 `FileBaseChatMemory`，解决服务重启后上下文丢失问题。
- `defaultSystem` 放入法律角色设定，保证每个请求都有同一人格和边界。
- `defaultAdvisors` 先挂 `MyLoggerAdvisor`，再挂 `MessageChatMemoryAdvisor`，形成可观测+可记忆的基础链路。

## 4. 关键函数思路

### doChat
- 需求: 最常见的多轮法律咨询。
- 做法: `user(message)` + `advisorSpec.param(ChatMemory.CONVERSATION_ID, chatId)` + `call()`。
- 价值: 在最小调用成本下获得多轮记忆与 token 统计。

### doChatWithReport
- 需求: 让回答结构化，便于后续展示与归档。
- 做法: 强化 system 提示，要求生成法律报告，再 `entity(LawReport.class)` 做结构化解析。
- 价值: 把“自然语言输出”升级为“可程序消费的数据结构”。

### doChatWithRAG
- 需求: 降低法律问答幻觉，提高“有据可查”的可信度。
- 做法: 在调用链加入 `QuestionAnswerAdvisor.builder(lawAppVectorStore).build()`。
- 价值: 查询时把向量库召回片段注入上下文，回答更贴近本地知识语料。

### doChatWithCloudRAG
- 需求: 在本地文档之外接入云端知识库。
- 做法: 注入 `lawAppRAGCloudAdvisor` 并加入 advisors 链。
- 价值: 复用统一问答框架，快速切换到云知识检索增强。

### doChatWithMcp / doChatWithTools
- 需求: 让法律助手具备“行动能力”（查资料、读写文件、生成文档等）。
- 做法: 在 prompt 链中调用 `.tools(toolCallbackProvider)` 或 `.tools(toolCallbacks)`。
- 价值: 从“只会说”升级为“会调用外部能力完成任务”。

### doChatByStream
- 需求: 前端实时展示生成过程，提升交互体验。
- 做法: `stream().content()` 返回 `Flux<String>`。
- 价值: 支持 SSE/WebFlux 风格流式输出。

## 5. 为什么这样拆方法
- 一个方法只对应一个业务能力组合，便于测试和问题定位。
- 复用同一个 `chatClient`，减少重复配置和接入成本。
- 后续如果增加“合规审查 Advisor”“敏感词过滤 Advisor”，只需在链路上扩展。

## 6. 后续可演进点
- 增加统一异常包装，区分模型异常/检索异常/工具异常。
- 对各 `doChat*` 方法补接口级 SLA 指标（耗时、命中率、token）。
- 让 `LawReport` 增加法规条文引用字段，便于结果可追溯。

