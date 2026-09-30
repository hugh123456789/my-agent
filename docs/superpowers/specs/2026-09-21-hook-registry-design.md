# Hook Registry 重构设计

## 目标

将 `agents/hook` 重构为统一的事件注册与触发模型，消除生命周期方法和通用回调模型并存造成的职责混乱。调用方只通过 `registerHook` 注册回调，通过 `triggerHooks` 触发事件。

## 核心模型

`HookRegistry` 维护 `Map<HookEvent, List<HookCallback>>`：

- `registerHook(HookEvent, HookCallback)`：按注册顺序追加回调。
- `triggerHooks(HookEvent, block, output)`：依次执行事件回调，将非 `null` 返回值作为下一次回调的 output。
- 回调抛出异常时隔离异常并继续执行同一事件的后续回调。
- 未注册事件直接返回原 output。

`HookContext` 统一封装事件、block、当前 output 及可扩展元信息，避免 Agent 和各个 hook 之间通过多个 `Object` 参数传递上下文。

## Hook 职责

- `ContextInjectHook`：处理 `USER_PROMPT_SUBMIT`，注入工作目录等上下文。
- `PermissionHook`：处理 `PRE_TOOL_USE`，返回 `PermissionResult`。
- `LogHook`：处理工具调用、工具结果和循环结束等审计事件。
- `LargeOutputHook`：处理 `POST_TOOL_USE`，裁剪或转换过大的工具输出。
- `SummaryHook`：处理循环结束或摘要事件。

Spring 组件通过 `HookRegistrar` 注册自身回调；注册机制与触发机制分离，`HookRegistry` 不再了解具体 hook 实现。

## 事件

事件名称由 `HookEvent` 枚举统一维护，至少包含：

- `USER_PROMPT_SUBMIT`
- `PRE_TOOL_USE`
- `POST_TOOL_USE`
- `AFTER_LOOP`
- `SUMMARY`

枚举提供稳定的外部名称，用于日志和未来配置化注册。

## Agent 集成

`Agent` 的用户输入、工具执行前、工具执行后、循环结束等位置统一调用 `triggerHooks`。Agent 只处理事件结果，不直接调用具体 hook 类，也不保留 `beforeTool`、`beforeRound`、`afterLoop` 等旧式生命周期方法。

## 错误处理与兼容性

- Hook 异常默认隔离，不影响后续 hook 和 Agent 主流程。
- 权限 hook 返回 `Allowed`、`Denied` 或 `AskUser`，由 Agent 继续执行现有权限分支。
- 现有 `DefaultPermissionChecker` 和 `TodoTool` 的行为保持不变，只调整其注册方式。
- 删除旧 `AgentHook` 兼容层，避免两套调用路径长期并存。

## 测试

覆盖注册顺序、output 传递、`null` 放行、异常隔离、事件未注册、权限结果和大输出处理；同时运行完整 Maven 测试套件。
