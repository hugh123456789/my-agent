# Tool Set Isolation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将工具注册拆分为可复用的具名工具集，使主 Agent 和未来子 Agent 可以使用不同的 `ToolDispatcher`。

**Architecture:** 保留现有 `ToolDispatcher` 和执行协议，在 `ToolConfig` 中把每个工具的注册逻辑拆成独立方法，并创建 `mainToolDispatcher` 与 `subAgentToolDispatcher` 两个具名 Bean。`AgentLoopExecutor` 使用 `@Qualifier("mainToolDispatcher")` 明确绑定主 Agent 工具集。

**Tech Stack:** Java 21、Spring Boot 4.1、JUnit 5、Maven。

**Spec:** `docs/superpowers/specs/2026-09-22-tool-set-design.md`

## Global Constraints

- 保持现有 `ToolDispatcher` API 不变。
- 主 Agent 仍拥有 bash、日期时间和 todo 工具。
- 本次不改造 `AgentLoopExecutor` 为运行时动态选择工具集。
- 本次不改变 HookRegistry 的作用域和权限策略。

## Review Focus

- 两个 Dispatcher 必须是不同实例，避免一个工具集的注册污染另一个工具集；由 `ToolConfigTest#createsIndependentToolSets` 覆盖。
- 主工具集必须包含 `bash`、`getCurrentDateTime`、`getCurrentDate`、`getCurrentTime`、`todo_write`；由 `ToolConfigTest#mainToolSetContainsAllMainTools` 覆盖。
- 子工具集不能意外暴露 bash 或 todo；由 `ToolConfigTest#subAgentToolSetContainsOnlySelectedTools` 覆盖。
- `AgentLoopExecutor` 必须明确使用主工具集，避免多个 `ToolDispatcher` Bean 导致注入歧义；由编译和 Spring Bean 定义检查覆盖。
- 已有工具执行逻辑不能因注册方法拆分而改变；由现有 `TodoToolTest` 和 `DateTimeToolTest` 覆盖。

### Task 1: Split tool registration and define named tool sets

**Files:**
- Modify: `src/main/java/com/agent/codeagent/common/ToolConfig.java`
- Modify: `src/main/java/com/agent/codeagent/agents/core/AgentLoopExecutor.java`
- Test: `src/test/java/com/agent/codeagent/common/ToolConfigTest.java`

**Interfaces:**
- Produces Spring beans named `mainToolDispatcher` and `subAgentToolDispatcher`, both of type `ToolDispatcher`.
- `mainToolDispatcher` contains the five current tool names: `bash`, `getCurrentDateTime`, `getCurrentDate`, `getCurrentTime`, `todo_write`.
- `subAgentToolDispatcher` contains only the selected date-time tools: `getCurrentDateTime`, `getCurrentDate`, `getCurrentTime`.
- `AgentLoopExecutor.toolDispatcher` is injected with `@Qualifier("mainToolDispatcher")`.

- [ ] **Step 1: Write the failing tests**

Create `ToolConfigTest` as a plain unit test. Instantiate `DateTimeTool`, `TodoTool`, and `ToolConfig`; use the configuration methods after they are made callable, then assert the tool-name sets and dispatcher identity:

```java
@Test
void mainToolSetContainsAllMainTools() {
    ToolConfig config = new ToolConfig();
    ToolDispatcher dispatcher = config.mainToolDispatcher(new TodoTool(), new DateTimeTool());

    assertEquals(Set.of("bash", "getCurrentDateTime", "getCurrentDate",
            "getCurrentTime", "todo_write"), names(dispatcher));
}

@Test
void subAgentToolSetContainsOnlySelectedTools() {
    ToolConfig config = new ToolConfig();
    ToolDispatcher dispatcher = config.subAgentToolDispatcher(new DateTimeTool());

    assertEquals(Set.of("getCurrentDateTime", "getCurrentDate", "getCurrentTime"),
            names(dispatcher));
}

@Test
void createsIndependentToolSets() {
    ToolConfig config = new ToolConfig();
    ToolDispatcher main = config.mainToolDispatcher(new TodoTool(), new DateTimeTool());
    ToolDispatcher sub = config.subAgentToolDispatcher(new DateTimeTool());

    assertNotSame(main, sub);
    assertNotEquals(names(main), names(sub));
}

private Set<String> names(ToolDispatcher dispatcher) {
    return dispatcher.getSpecifications().stream()
            .map(ToolSpecification::name)
            .collect(Collectors.toSet());
}
```

- [ ] **Step 2: Run the focused test and verify it fails**

Run: `mvn -q -Dtest=ToolConfigTest test`

Expected: FAIL because `ToolConfig` does not yet expose `mainToolDispatcher` or `subAgentToolDispatcher`.

- [ ] **Step 3: Implement the minimal configuration change**

In `ToolConfig`:

1. Change the current `toolDispatcher(...)` bean method into `mainToolDispatcher(...)`, keeping the current parameter list and returning the full tool set.
2. Extract the existing registration blocks into private methods with these signatures:

```java
private void registerBash(ToolDispatcher dispatcher)
private void registerDateTime(ToolDispatcher dispatcher, DateTimeTool dateTimeTool)
private void registerTodo(ToolDispatcher dispatcher, TodoTool todoTool)
```

3. Add:

```java
@Bean("subAgentToolDispatcher")
public ToolDispatcher subAgentToolDispatcher(DateTimeTool dateTimeTool) {
    ToolDispatcher dispatcher = new ToolDispatcher();
    registerDateTime(dispatcher, dateTimeTool);
    return dispatcher;
}
```

4. Annotate the main bean explicitly as `@Bean("mainToolDispatcher")` and call all three registration methods.

In `AgentLoopExecutor`, add `org.springframework.beans.factory.annotation.Qualifier` and annotate the existing `ToolDispatcher` field:

```java
@Resource
@Qualifier("mainToolDispatcher")
private ToolDispatcher toolDispatcher;
```

- [ ] **Step 4: Run the focused test and verify it passes**

Run: `mvn -q -Dtest=ToolConfigTest test`

Expected: PASS.

- [ ] **Step 5: Run compile and existing tool tests**

Run: `mvn -q -DskipTests compile` and `mvn -q -Dtest=DateTimeToolTest,TodoToolTest,ToolCallProcessorTest test`

Expected: compile succeeds and the focused tool tests pass. If the full Spring context test fails while creating the external model client, report that existing environment failure separately.

- [ ] **Step 6: Review the final diff**

Run: `git diff --check` and inspect only `ToolConfig.java`, `AgentLoopExecutor.java`, and `ToolConfigTest.java` for this task. Confirm no existing unrelated worktree changes are included in the implementation edits.
