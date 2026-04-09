# Advisor 模块形成思路笔记

对应代码:
- `src/main/java/com/lx/aisuperagent/advisor/MyLoggerAdvisor.java`
- `src/main/java/com/lx/aisuperagent/advisor/ReReadingAdvisor.java`

## 1. 真实开发需求
- 需要在不修改业务代码主体的情况下，给模型调用链增加“横切能力”。
- 横切能力包括: 请求/响应日志、Token 消耗追踪、提示词增强。

## 2. MyLoggerAdvisor 的形成思路
- 痛点: 业务层只拿到最终文本，难以观察完整请求上下文与 token 消耗。
- 方案: 自定义 `CallAdvisor` + `StreamAdvisor`，分别处理同步与流式场景。
- 关键点:
  - `before(...)` 记录请求上下文，便于回溯提示词。
  - `after(...)` 记录回复文本与 Usage，形成成本审计信息。
  - `adviseStream(...)` 使用 `ChatClientMessageAggregator` 聚合分片流，再统一记录日志。
- 价值: 不侵入业务方法，即可统一获得可观测性。

## 3. ReReadingAdvisor 的形成思路
- 痛点: 用户问题复杂时，模型容易漏掉重点。
- 方案: 在请求进入模型前，自动改写用户输入，插入“再读一遍问题”的提示模板。
- 关键点:
  - 使用 `PromptTemplate` 渲染 `re2_input_query`。
  - 在 `before(...)` 阶段对 prompt 做 `augmentUserMessage(...)`。
  - `after(...)` 保持透传，避免副作用。
- 价值: 用轻量提示工程提升推理稳定性，不改主业务流程。

## 4. 组合使用的业务意义
- `LawApp` 构造器挂载 `MyLoggerAdvisor`，解决可观测问题。
- 可按场景增加 `ReReadingAdvisor`，在难题咨询时提高回答完整性。
- Advisor 机制让“功能增强”可以插件化迭代，而非频繁改业务方法。

