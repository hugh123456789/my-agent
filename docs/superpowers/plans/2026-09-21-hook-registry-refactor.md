# Hook Registry Refactor Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 将 hook 目录重构为以 `HookRegistry`、`registerHook` 和 `triggerHooks` 为核心的事件回调系统。

**Architecture:** `HookRegistry` 使用 `Map<HookEvent, List<HookCallback>>` 管理有序回调；`HookContext` 统一携带事件输入和当前输出；Spring hook 组件通过 `HookRegistrar` 注册回调。`Agent` 只触发事件并消费结果，不再调用旧生命周期方法。

**Tech Stack:** Java 21, Spring Boot 4.1, JUnit 5, Maven.

**Spec:** `docs/superpowers/specs/2026-09-21-hook-registry-design.md`

## Global Constraints

- Hook 回调按注册顺序执行。
- 回调返回 `null` 时保持当前 output。
- 单个 hook 异常不得阻断同一事件的后续回调。
- `PermissionResult` 继续支持 `Allowed`、`Denied`、`AskUser`。
- Agent 不再保留 `beforeTool`、`beforeRound`、`afterLoop` 旧式调用。
- 不引入新的第三方依赖。

## Review Focus

- 未注册事件必须原样返回 output；由 Task 1 测试覆盖。
- 回调返回 `null` 时后续回调必须仍收到上一个有效 output；由 Task 1 测试覆盖。
- 回调抛异常后后续回调仍需执行；由 Task 1 测试覆盖。
- 权限 hook 返回 `Denied` 或 `AskUser` 时 Agent 分支不能改变；由 Task 3 测试覆盖。
- 工具输出被 hook 修改后，消息和流事件必须使用修改后的值；由 Task 3 测试覆盖。

### Task 1: 建立类型安全的 Hook 核心模型

**Files:**
- Create: `src/main/java/com/agent/codeagent/agents/hook/HookEvent.java`
- Create: `src/main/java/com/agent/codeagent/agents/hook/HookContext.java`
- Create: `src/main/java/com/agent/codeagent/agents/hook/HookCallback.java`
- Create: `src/main/java/com/agent/codeagent/agents/hook/HookRegistrar.java`
- Create: `src/main/java/com/agent/codeagent/agents/hook/HookRegistry.java`
- Modify/Delete: `src/main/java/com/agent/codeagent/agents/hook/HookName.java`, `AgentHookRegistry.java`, `AgentHook.java`
- Test: `src/test/java/com/agent/codeagent/agents/hook/HookRegistryTest.java`

**Interfaces:**
- `HookEvent.value(): String`
- `HookContext(HookEvent event, Object block, Object output)` with `withOutput(Object output)`.
- `HookCallback.apply(HookContext context): Object`.
- `HookRegistrar.register(HookRegistry registry): void`.
- `HookRegistry.registerHook(HookEvent event, HookCallback callback): void`.
- `HookRegistry.triggerHooks(HookEvent event, Object block, Object output): Object`.

- [ ] **Step 1: Write failing tests** for event names, registration order, output chaining, null pass-through, unknown events, exception isolation, and duplicate registrations.
- [ ] **Step 2: Run `mvn -q -Dtest=HookRegistryTest test` and confirm failure because the new API does not exist.**
- [ ] **Step 3: Implement the enum, context, callback, registrar, and registry with a concurrent map and ordered callback lists.**
- [ ] **Step 4: Run the focused test and confirm all core registry behaviors pass.**
- [ ] **Step 5: Keep the old compatibility types temporarily so Tasks 2–3 can migrate callers without breaking the build; remove them in Task 4 after the reference search is clean.**

### Task 2: Convert hook implementations into registrars

**Files:**
- Create: `src/main/java/com/agent/codeagent/agents/hook/ContextInjectHook.java`
- Create: `src/main/java/com/agent/codeagent/agents/hook/PermissionHook.java`
- Create: `src/main/java/com/agent/codeagent/agents/hook/LogHook.java`
- Create: `src/main/java/com/agent/codeagent/agents/hook/LargeOutputHook.java`
- Create: `src/main/java/com/agent/codeagent/agents/hook/SummaryHook.java`
- Modify: `src/main/java/com/agent/codeagent/agents/hook/DefaultPermissionChecker.java`
- Modify: `src/main/java/com/agent/codeagent/agents/hook/PermissionResult.java`
- Test: `src/test/java/com/agent/codeagent/agents/hook/PermissionHookTest.java`

**Interfaces:**
- Each Spring hook component implements `HookRegistrar` and registers only its own events.
- `PermissionHook` registers `PRE_TOOL_USE` and returns a `PermissionResult`.
- `ContextInjectHook` registers `USER_PROMPT_SUBMIT` and returns a prompt string or `null`.
- `LargeOutputHook` registers `POST_TOOL_USE` and returns the original or shortened output.
- `LogHook` and `SummaryHook` preserve side effects and return the current output.

- [ ] **Step 1: Add failing tests for permission decisions and registrar registration.**
- [ ] **Step 2: Run the focused tests and verify they fail before implementation.**
- [ ] **Step 3: Move the existing permission rules into `PermissionHook` without changing deny/ask/allow semantics.**
- [ ] **Step 4: Implement the remaining four registrars with minimal behavior matching the spec and current application behavior.**
- [ ] **Step 5: Run focused hook tests and verify all registrar behavior.**

### Task 3: Migrate Agent and tool integrations

**Files:**
- Modify: `src/main/java/com/agent/codeagent/agents/core/Agent.java`
- Modify: `src/main/java/com/agent/codeagent/tools/TodoTool.java`
- Test: `src/test/java/com/agent/codeagent/agents/core/AgentHookIntegrationTest.java`

**Interfaces:**
- Inject `HookRegistry` into `Agent`.
- Trigger `USER_PROMPT_SUBMIT`, `PRE_TOOL_USE`, `POST_TOOL_USE`, and `AFTER_LOOP` only through `triggerHooks`.
- Register all `HookRegistrar` beans in the registry during construction/configuration.

- [ ] **Step 1: Add integration tests proving prompt modification, permission result propagation, post-tool output modification, and after-loop notification.**
- [ ] **Step 2: Run the integration tests and confirm they fail against the old call paths.**
- [ ] **Step 3: Replace all `Agent` calls to `AgentHookRegistry` and migrate `TodoTool` to the registrar API.**
- [ ] **Step 4: Remove old lifecycle-method references with `rg` and compile the main source set.**
- [ ] **Step 5: Run focused integration tests and confirm the modified values reach messages and events.**

### Task 4: Cleanup and full verification

**Files:**
- Modify: `src/test/java/com/agent/codeagent/CodeAgentApplicationTests.java` only if the existing context test needs the new registry wiring.
- Delete: obsolete hook compatibility files left from Tasks 1–3.

- [ ] **Step 1: Search for `AgentHook`, `AgentHookRegistry`, `HookName`, `beforeTool`, `beforeRound`, and `afterLoop` references outside intentional test fixtures.**
- [ ] **Step 2: Run `mvn -q -DskipTests compile`.**
- [ ] **Step 3: Run `mvn -q test` and record any pre-existing environment failure separately from code failures.**
- [ ] **Step 4: Run `git diff --check` and inspect the final status without staging unrelated user changes.**
