# Claude Code Web UI 待修复清单

最后更新：2026-09-28

## 本轮已修复

- [x] 为 `AgentEvent` 序列化补充 `type` 判别字段，保证 `/agent/loop` SSE 可被前端分发。
- [x] 修复审批等待态：审批卡片可操作，批准流不会被旧流的完成回调覆盖。
- [x] 审批恢复前关闭原挂起 SSE sink，避免每次审批泄漏连接。
- [x] 区分完成、EOF、Abort 和错误；断流不再误报完成，错误后 composer 恢复可用。
- [x] 未知事件保留原始 JSON；工具调用/结果增量按调用 ID 聚合。
- [x] 增加事件计数、错误 live region、streaming status、thinking 折叠和生成中可访问名称。
- [x] 为 localStorage、嵌套后端错误和 plain-text SSE 增加容错。
- [x] 修复窄屏滚动所有权、停止按钮可达性、标准滚动条 token 和辅助文本对比度。
- [x] 移除/禁用尚未实现的静态按钮，避免无动作 affordance。
- [x] 补充 SSE、事件归一化和 `AgentEvent` JSON 判别字段测试。

## 仍待修复 / 验证

- [ ] 在真实浏览器中验证 900px、640px、200% zoom、键盘焦点和移动虚拟键盘；当前环境阻止 localhost 页面加载。
- [ ] 将静态 Recent tasks 接入真实会话历史或明确改为纯展示；当前不应伪装成可切换历史。
- [ ] 实现附件按钮，或在产品决定前移除该入口。
- [ ] 将右侧模型名称改为后端实际返回值，避免硬编码 `DeepSeek Reasoner R1`。
- [ ] 增加端到端审批测试，覆盖原 SSE 关闭、批准/拒绝、过期和重复 requestId。
- [ ] 增加真实 Spring SSE 响应测试，覆盖 CRLF、多行 data、未知事件及错误 Throwable 的最终 JSON 形状。

## 验证备注

- 前端 `npm test`：8/8 通过。
- Vite production build：通过。
- 针对 SSE、审批协议和事件序列化的 Maven 测试：6/6 通过。
- 严格 UI 静态审计：0 findings；证据保存在 `web-ui/premium-audit.json`。
- 完整 `mvn test`：49 个测试中 48 个通过；`CodeAgentApplicationTests.contextLoads` 受当前环境 loopback 连接限制失败，不是本轮前端改动引入的失败。
