# Tools 模块形成思路笔记

对应代码:
- `src/main/java/com/lx/aisuperagent/Tools/ToolRegistration.java`
- `src/main/java/com/lx/aisuperagent/Tools/GoogleWebSearchTool.java`
- `src/main/java/com/lx/aisuperagent/Tools/FileOptionTool.java`
- `src/main/java/com/lx/aisuperagent/Tools/PDFGenerationTool.java`
- `src/main/java/com/lx/aisuperagent/Tools/TerminateTool.java`
- `src/main/java/com/lx/aisuperagent/Tools/FileConstant.java`

## 1. 真实开发需求
- 让 AI 能操作外部世界，而不是只返回文本。
- 核心能力: 检索网络信息、读写本地文件、导出 PDF、结束任务。

## 2. ToolRegistration 的形成思路
- 痛点: 多个工具分散，Agent 不知道可用工具集合。
- 做法: 用 `ToolCallbacks.from(...)` 聚合所有工具并注册为 `ToolCallback[]` Bean。
- 好处: Agent/Controller 可统一注入 `allTools`，方便扩展和测试。

## 3. GoogleWebSearchTool 的形成思路
- 需求: 回答实时问题（新闻、最新事件）时要联网查询。
- 做法:
  - 封装 SerpApi 请求，构建 URL 并处理可选参数（地区、语言、域名）。
  - 解析 `organic_results`，整理为模型易消费的摘要文本。
- 设计价值: 把第三方 HTTP 细节隐藏成一个简单 `googleSearch(query)` 工具。

## 4. FileOptionTool 的形成思路
- 需求: 让 Agent 能读/写项目内临时文件，支持任务中间产物保存。
- 做法:
  - 统一写入目录来自 `FileConstant.FILE_SAVE_DIR`。
  - 提供 `readFile` / `writeFile` 两个基础能力。
- 价值: 低成本支持“生成结果落盘、再读取继续处理”的链路。

## 5. PDFGenerationTool 的形成思路
- 需求: 需要把 AI 结果导出成正式文档给用户。
- 做法:
  - 用 iText 创建 PDF 文档。
  - 选中文字体 `STSongStd-Light` 解决中文乱码。
  - 把内容落到统一目录，返回文件路径。
- 价值: 工具调用一步直达可交付文件。

## 6. TerminateTool 的形成思路
- 需求: 多步 Agent 需要明确“何时停止”机制。
- 做法: 提供 `doTerminate()` 工具，供模型在任务完成后显式调用。
- 价值: 减少无限循环风险，和 `ToolCallAgent` 状态机联动。

## 7. FileConstant 的形成思路
- 用统一常量约束临时产物写入路径，避免工具各自写不同目录。
- 当前路径指向 `src/main/java/com/lx/aisuperagent/tmp`，便于开发阶段查看。

