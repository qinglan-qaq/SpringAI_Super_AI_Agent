# FileBaseChatMemory 形成思路笔记

对应代码: `src/main/java/com/lx/aisuperagent/chatmemory/FileBaseChatMemory.java`

## 1. 真实开发需求
- 仅使用内存记忆会在服务重启后丢失会话历史。
- 法律咨询往往是多轮长对话，需要持久化会话上下文。

## 2. 设计思路
- 实现 `ChatMemory` 接口，自定义“基于文件”的记忆存储。
- 每个 `conversationId` 映射到一个独立 `.kryo` 文件。
- 用 Kryo 做序列化，减少对象存储复杂度。

## 3. 核心函数形成逻辑
- `add(conversationId, messages)`:
  - 先读取旧会话，再追加新消息，最后整体写回文件。
  - 满足“追加式会话记忆”的业务语义。
- `get(conversationId)`:
  - 从会话文件读取完整消息列表，供 `MessageChatMemoryAdvisor` 注入历史。
- `clear(conversationId)`:
  - 删除会话文件，实现按会话清空记忆。

## 4. 为什么选择 Kryo
- Spring AI 的 `Message` 对象结构相对复杂，JSON 手工序列化成本高。
- Kryo 对对象图序列化高效，适合开发阶段快速落地。

## 5. 工程层面的价值
- 与 `LawApp` 组合后，可实现跨请求、跨重启的聊天上下文延续。
- 不依赖数据库，部署门槛低，适合本地开发与小规模场景验证。

## 6. 后续可优化方向
- 增加并发写锁，避免同一 `conversationId` 并发写冲突。
- 增加文件损坏恢复机制与过期清理策略。
- 如进入生产，建议迁移到 Redis/DB 存储实现。

