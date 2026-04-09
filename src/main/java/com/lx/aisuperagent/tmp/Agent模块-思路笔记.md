# Agent 模块形成思路笔记

对应代码:
- `src/main/java/com/lx/aisuperagent/agent/BaseAgent.java`
- `src/main/java/com/lx/aisuperagent/agent/ReActAgent.java`
- `src/main/java/com/lx/aisuperagent/agent/ToolCallAgent.java`
- `src/main/java/com/lx/aisuperagent/agent/QinglanManus.java`
- `src/main/java/com/lx/aisuperagent/agent/model/AgentState.java`

## 1. 真实开发需求
- 希望让 AI 从“单次问答”升级为“多步任务执行者”。
- 需要可控状态机、步骤上限、工具调用、流式输出执行过程。

## 2. 架构分层思路
- `BaseAgent`: 定义状态机与运行框架（run/runStream/cleanup）。
- `ReActAgent`: 抽象 ReAct 模式，把一步拆成 `think + act`。
- `ToolCallAgent`: 落地工具调用逻辑，负责识别工具、执行工具、更新上下文。
- `QinglanManus`: 业务人格化封装（提示词、人设、最大步数、默认 advisor）。

## 3. BaseAgent 关键实现思路
- 用 `AgentState` 控制生命周期: IDLE -> RUNNING -> FINISHED/ERROR。
- `run(...)` 负责串行执行循环，按 `maxSteps` 限制风险。
- `messageList` 自维护上下文，避免每步丢失历史。
- `runStream(...)` 通过 `SseEmitter` 把每步结果实时推送给前端。

## 4. ReActAgent 关键实现思路
- `step()` 固化模板方法:
  1) `think()` 决策是否需要行动
  2) 若需要则 `act()`
  3) 若不需要则结束本轮
- 价值: 子类只关心“如何思考/如何行动”，复用执行骨架。

## 5. ToolCallAgent 关键实现思路
- `think()`:
  - 拼装 Prompt（历史消息 + agent options + system prompt）。
  - 模型输出里解析 `toolCalls`，判断是否进入行动阶段。
- `act()`:
  - 交给 `ToolCallingManager.executeToolCalls(...)` 执行工具。
  - 把工具执行结果写回 conversation history，保证下一轮可见。
  - 检测 `doTerminate` 工具，触发状态结束。
- 价值: 形成“模型规划 -> 工具执行 -> 结果回灌”的闭环。

## 6. QinglanManus 形成思路
- 需求: 快速得到一个可运行的具体 Agent，而不是抽象框架。
- 做法:
  - 在构造器中注入可用工具与 DashScope 模型。
  - 设定系统提示词和下一步提示词，强化执行策略。
  - 设置 `maxSteps=20`，平衡任务完成率与成本。
  - 为 chatClient 挂 `MyLoggerAdvisor`，提高执行过程可观测性。

