# 工具集隔离设计

## 背景

当前 `ToolConfig` 创建一个全局 `ToolDispatcher`，`AgentLoopExecutor` 直接注入它，因此所有 Agent 默认共享同一套工具。后续需要让主 Agent、子 Agent 可以分别拥有不同的工具列表。

## 目标

- 将工具注册逻辑从单一列表中拆分为可组合的注册方法。
- 提供具名的 `ToolDispatcher` Bean，允许不同 Agent 选择不同工具集。
- 保持现有工具执行协议和 `ToolDispatcher` API 不变。
- 主 Agent 当前行为不变，仍拥有 bash、日期时间和 todo 工具。

## 设计

### 工具注册

`ToolConfig` 保留 Spring 配置职责，但将每个工具的注册逻辑拆分为独立私有方法：

- `registerBash`
- `registerDateTime`
- `registerTodo`

每个方法只向传入的 `ToolDispatcher` 注册对应工具，不创建新的 Dispatcher，也不修改工具实现类的状态。

### 工具集 Bean

定义具名 Dispatcher：

- `mainToolDispatcher`：注册 bash、日期时间、todo，作为当前主 Agent 的默认工具集。
- `subAgentToolDispatcher`：先提供一个独立的子 Agent 工具集示例，注册日期时间工具；后续可在该方法中增删工具。

两个 Dispatcher 彼此独立，工具规格列表和执行器映射不会互相污染。共享的无状态工具可以复用 Spring Bean；有状态工具是否复用由具体工具集显式决定。

### Agent 注入

`AgentLoopExecutor` 使用 `@Qualifier("mainToolDispatcher")` 注入主 Agent 工具集，避免多个 `ToolDispatcher` Bean 出现注入歧义。未来子 Agent 可以注入 `subAgentToolDispatcher`，或基于同样方式新增其他具名工具集。

## 非目标

- 本次不改造 `AgentLoopExecutor` 为运行时动态选择工具集。
- 本次不引入新的工具注册接口或插件发现机制。
- 本次不改变 HookRegistry 的作用域和权限策略。

## 验证

- 单元测试验证不同工具集包含预期工具名称。
- 编译项目，确认多个 Dispatcher Bean 的注入没有歧义。
- 运行现有工具相关测试；若 Spring 上下文仍受外部模型初始化影响，单独记录该既有环境问题。
