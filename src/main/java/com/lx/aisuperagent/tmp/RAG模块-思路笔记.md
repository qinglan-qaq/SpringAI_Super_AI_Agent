# RAG 模块形成思路笔记

对应代码:
- `src/main/java/com/lx/aisuperagent/rag/LawAppDocumentLoader.java`
- `src/main/java/com/lx/aisuperagent/rag/MyDocumentEnricher.java`
- `src/main/java/com/lx/aisuperagent/rag/MyKeywordEnricher.java`
- `src/main/java/com/lx/aisuperagent/rag/MySummaryEnricher.java`
- `src/main/java/com/lx/aisuperagent/rag/MyTokenTextSplitter.java`
- `src/main/java/com/lx/aisuperagent/rag/Better_MyTokenTextSplitter.java`
- `src/main/java/com/lx/aisuperagent/rag/config/LawAppVectorStoreConfig.java`
- `src/main/java/com/lx/aisuperagent/rag/config/PgVectorVectorStoreConfig.java`
- `src/main/java/com/lx/aisuperagent/rag/config/LawAppRAGCloudAdvisorConfig.java`
- `src/main/java/com/lx/aisuperagent/rag/config/EnricherConfig.java`

## 1. 真实开发需求
- 法律问答对事实和依据要求高，仅靠模型参数知识容易幻觉。
- 需要把“本地法律材料 + 云知识库 + 向量检索”接入对话链路。

## 2. 文档处理链路形成思路

### LawAppDocumentLoader
- 从 `classpath:document/*.md` 批量加载法律文档。
- 用 `MarkdownDocumentReaderConfig` 控制切分与元数据，保留 filename/title 等检索线索。
- 目标: 把原始 markdown 变成可向量化的 `Document` 集合。

### MyTokenTextSplitter / Better_MyTokenTextSplitter
- 需求: 原文太长，不切分会影响 embedding 与召回质量。
- 做法: 用 `TokenTextSplitter` 默认参数或 Builder 自定义 chunk 策略。
- 价值: 在检索召回时保持语义完整与粒度平衡。

### MyKeywordEnricher / MySummaryEnricher / MyDocumentEnricher
- 需求: 提升检索与 rerank 质量。
- 做法:
  - `KeywordMetadataEnricher` 给文档补关键词元数据。
  - `SummaryMetadataEnricher` 生成前后文感知摘要。
- 价值: 文档在向量库中不仅有正文向量，还有更丰富的语义标签。

## 3. 向量库配置形成思路

### LawAppVectorStoreConfig（本地内存向量库）
- 使用 `SimpleVectorStore` 快速搭建开发验证环境。
- 启动时直接把加载好的文档 `add` 到向量库。
- 优点: 依赖少、上手快，适合快速迭代。

### PgVectorVectorStoreConfig（生产向量库）
- 使用 `PgVectorStore` 对接 PostgreSQL + pgvector。
- 可配置 distance/index/schema/table/batch，兼顾性能与可运维性。
- 优点: 可持久化、可扩展，适合生产环境。

## 4. 云 RAG Advisor 形成思路

### LawAppRAGCloudAdvisorConfig
- 需求: 本地知识不足时，接入阿里云知识库补充召回。
- 做法:
  - 用 `DashScopeApi` + `DashScopeDocumentRetriever` 指定知识库索引。
  - 外层包装 `RetrievalAugmentationAdvisor`，可直接挂到 ChatClient advisors 链。
- 价值: 云知识检索能力以“插件方式”接入，不改业务主流程。

## 5. EnricherConfig 的角色
- 把 `SummaryMetadataEnricher` 注册成 Bean，供摘要增强场景复用。
- 通过 PREVIOUS/CURRENT/NEXT 摘要策略保留上下文结构信息。

## 6. 与 LawApp 的闭环
- 文档加载/切分/增强/建库解决“知识准备”。
- `QuestionAnswerAdvisor` / 云 RAG Advisor 解决“检索注入”。
- 最终在 `LawApp#doChatWithRAG` 和 `doChatWithCloudRAG` 中实现可落地的检索增强问答。

