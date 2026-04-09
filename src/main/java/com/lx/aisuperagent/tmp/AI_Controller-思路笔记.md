# AI_Controller 形成思路笔记

对应代码: `src/main/java/com/lx/aisuperagent/controller/AI_Controller.java`

## 1. 真实开发需求
- 给前端提供可直接调用的 AI HTTP 接口。
- 同时支持同步响应和流式响应，满足不同 UI 交互方式。

## 2. 设计思路
- Controller 只做协议层转换，不承载复杂业务逻辑。
- 业务能力全部委托给 `LawApp`，确保控制器轻量、可维护。
- 给同一业务提供两类流式方案: `Flux<ServerSentEvent<String>>` 与 `SseEmitter`。

## 3. 方法形成过程

### doChatWithLawLovelyAppSync
- 需求: 最简单的同步问答接口，便于联调。
- 实现: 直接调用 `lawApp.doChat(message, chatId)` 返回字符串。

### doChatWithLoveAppSSE
- 需求: 前端希望逐段接收模型输出。
- 实现: 将 `Flux<String>` 映射为 `ServerSentEvent<String>`。
- 价值: 更契合标准 SSE 消费模型。

### doChatWithLoveAppSseEmitter
- 需求: 部分传统 Spring MVC 客户端不方便直接消费 Reactor Flux。
- 实现: 使用 `SseEmitter` 手动订阅 `Flux` 并转发。
- 价值: 给非响应式调用方保留兼容路径。

## 4. 关键工程考虑
- 通过 `chatId` 透传到 LawApp，保证会话记忆维持在同一链路。
- Emitter 显式处理 `chunk/error/complete`，避免前端连接悬挂。
- 控制器中注入 `ToolCallback[]`、`ChatModel` 体现后续扩展入口（目前主要由 LawApp 使用）。

