# Claude Code 风格 Web 工作台 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在 `web-ui/` 中实现 Claude Code 风格的深色 Agent 工作台，并接入现有 `/agent/loop` SSE 与审批接口。

**Architecture:** `App.vue` 负责工作台布局、会话状态、消息状态和交互；`src/services/agentApi.js` 只负责请求 URL、SSE 解析和取消控制。CSS 通过 token 化变量提供 Claude 暖橙色主题和响应式三栏布局。

**Tech Stack:** Vue 3 Composition API, Vite, 原生 Fetch/ReadableStream, CSS。

**Spec:** `docs/superpowers/specs/2026-09-28-claude-code-web-ui-design.md`

## Global Constraints

- 保留 Vue 3 + Vite，不新增 UI 框架。
- 后端继续使用当前 Spring Boot 协议：`GET /agent/loop`、`GET /agent/loop/approve`，前端通过 Vite 代理访问 `18081`。
- 主题使用深色中性背景和 Claude 风格暖橙色强调色。
- 不使用浏览器 `alert()`、`confirm()` 或 `prompt()`。
- 只将后端返回的 session ID 写入 `localStorage`。

## Review Focus

- SSE 返回非 JSON 的文本事件时，页面仍然追加可读内容；测试归入 Task 1。
- 流式请求被停止后，页面回到可发送状态且不显示误导性的成功状态；测试归入 Task 2。
- 审批请求缺少部分可选字段时仍显示可操作的批准/拒绝卡片；测试归入 Task 2。
- 视口窄于 900px 时三栏布局不产生横向溢出；测试归入 Task 3。
- 后端断开或返回错误时，已有消息保留并显示恢复提示；测试归入 Task 2。

### Task 1: SSE API 服务

**Files:**
- Create: `web-ui/src/services/agentApi.js`
- Modify: `web-ui/vite.config.js`

**Interfaces:**
- Produces `createAgentStream({ sessionId, message, onEvent, onError, onComplete }) -> { abort() }`。
- Produces `approveAgentTool({ requestId, approved, onEvent, onError, onComplete }) -> { abort() }`。

- [ ] **Step 1: Implement the event stream reader**

  使用 `fetch` + `ReadableStream` 读取 SSE 帧，解析 `event:` 和 `data:`，对 JSON data 返回对象，对纯文本返回字符串；允许跨 chunk 的半帧拼接，并在完成/异常时调用对应回调。

- [ ] **Step 2: Configure the Vite proxy**

  将 `/agent` 代理到 `http://localhost:18081`，开发环境前端使用相对 URL，避免浏览器跨域。

- [ ] **Step 3: Verify service syntax through the app build**

  Run: `npm run build` in `web-ui`

  Expected: PASS。

### Task 2: 工作台状态与 Agent 交互

**Files:**
- Modify: `web-ui/src/App.vue`

**Interfaces:**
- Consumes `createAgentStream` and `approveAgentTool` from Task 1。
- Maintains `messages`, `activities`, `sessionId`, `isStreaming`, `pendingApproval`, `connectionState`。

- [ ] **Step 1: Implement the send/stop/approval state transitions**

  发送时追加 user 消息和 assistant 占位消息；按事件合并文本与 thinking，记录 tool activity，保存 session ID；审批事件生成卡片并串接批准/拒绝 stream；AbortController 取消请求并恢复 composer。

- [ ] **Step 2: Add empty, loading, success, approval and error rendering**

  让页面在无消息时提供示例 prompt，流式期间显示连接状态和停止动作，错误时保留历史内容并显示可重试提示。

- [ ] **Step 3: Verify behavior with a production build**

  Run: `npm run build` in `web-ui`

  Expected: PASS，且没有 Vue template/compiler error。

### Task 3: Claude 风格视觉系统与响应式布局

**Files:**
- Modify: `web-ui/src/style.css`
- Modify: `web-ui/src/App.vue`

- [ ] **Step 1: Replace the starter CSS with application tokens and global states**

  删除默认 Vite 样式，定义背景/面板/边框/文本/Claude 橙色变量、滚动条、focus ring、button 状态和消息排版。

- [ ] **Step 2: Build the three-panel workbench layout**

  添加侧栏、中心消息流/composer、右侧运行上下文的语义结构；工具调用、思考折叠区和审批卡片使用稳定尺寸和等宽信息样式。

- [ ] **Step 3: Add responsive behavior and narrow viewport checks**

  在 `max-width: 1100px` 收窄侧栏，在 `max-width: 900px` 隐藏/折叠右侧栏并保证输入区可用，在 `max-width: 640px` 调整内边距和按钮布局。

- [ ] **Step 4: Run final verification**

  Run: `npm run build` in `web-ui`; `git diff --check` at repository root。

  Expected: both PASS。
