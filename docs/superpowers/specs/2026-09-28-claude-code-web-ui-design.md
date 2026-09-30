# Claude Code 风格 Web 工作台设计

## 目标

将 `web-ui/` 的 Vite 默认 Vue 模板替换为一个类似 Claude Code 的开发者工作台：用户可以在浏览器中发起任务，查看 Agent 的流式思考、文本和工具活动，并在需要时批准工具调用。

## 约束与成功标准

- 保留 Vue 3 + Vite，不新增 UI 框架。
- 后端继续使用当前 Spring Boot 协议：`GET /agent/loop`、`GET /agent/loop/approve`，前端通过 Vite 代理访问 `18081`。
- 使用 SSE 增量事件；`session` 事件驱动会话 ID 持久化，审批事件驱动允许/拒绝操作。
- 主题使用深色中性背景和 Claude 风格暖橙色强调色，避免默认 Vite 紫色视觉。
- 桌面端提供左侧会话栏、中间对话流、右侧运行上下文；窄屏时右侧上下文收起，消息区保持可用。
- 页面必须明确表达空闲、发送中、流式接收、审批等待、完成和错误状态。

## 结构与数据流

`App.vue` 负责页面组合、消息状态和交互；`src/services/agentApi.js` 负责构造 URL、读取 SSE 和解析事件。发送消息时由 API 服务返回一个可取消的 stream 控制器，App 将事件映射为统一的消息/活动模型。

事件映射规则：

- `session`：保存当前 session ID 到 `localStorage`，并显示在运行信息中。
- `text`：追加到当前 assistant 消息。
- `thinking`：追加到可折叠的 reasoning 区域。
- `tool_call`、工具结果或未知结构化事件：生成工具活动卡片，保留原始 JSON 便于排查。
- `approval`/审批请求：显示审批卡片，调用 `/agent/loop/approve`；批准后继续消费新流。
- `complete`：结束 loading 状态并将当前回合标记完成。
- `error`：保留已收到内容，显示错误恢复提示。

## 视觉与交互

- 使用 CSS 变量集中管理背景、面板、边框、文本和 Claude 橙色 token。
- 侧栏展示产品标识、当前 workspace 和最近会话；当前版本可以使用本地静态会话项，不额外虚构后端历史接口。
- 中心区域顶部展示任务标题、连接状态和停止按钮；底部 composer 支持多行输入、Enter 发送、Shift+Enter 换行。
- 首屏空状态提供简短引导和三个可点击示例 prompt。
- 工具活动使用等宽字体和细边框；审批动作使用原生 button，具备 hover/focus/disabled 状态。
- 右侧显示 session、模型、事件数量和当前运行阶段，作为对流式状态的可见反馈。

## 错误与安全边界

- 所有 fetch/SSE 失败都转换为页面内错误，不使用浏览器 alert/confirm。
- 发送期间禁用重复发送，停止操作使用 AbortController 取消当前请求。
- 不把 prompt 或敏感值写入 URL 以外的持久化存储；只保存后端返回的 session ID。
- 后端事件字段可能随实现变化，解析器要容忍 JSON、纯文本和未知 event 名称。

## 验证

- `npm run build` 必须成功。
- 手动检查空状态、发送中、流式文本、thinking/tool 卡片、审批、错误、停止和窄屏布局。
- 运行 `git diff --check` 确保无空白错误。
