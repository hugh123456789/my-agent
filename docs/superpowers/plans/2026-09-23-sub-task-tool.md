# `sub_task` Nested Agent Loop Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a synchronous `task` tool that runs an isolated nested Agent Loop and returns its final text to the parent Agent.

**Architecture:** Keep the parent streaming loop unchanged. Add a focused `SubAgentRunner` for synchronous child execution and a `SubTaskTool` adapter registered only in the main dispatcher. The runner owns an in-memory child message list, uses the child dispatcher without `task`, and reuses the existing model, `ToolCallProcessor`, and `HookRegistry`.

**Tech Stack:** Java 21, Spring Boot 4.1, LangChain4j 1.3.0, Reactor, JUnit 5, Maven.

**Spec:** `docs/superpowers/specs/2026-09-23-sub-task-tool-design.md`

## Global Constraints

- The nested loop starts with a new in-memory `messages[]` containing only the child prompt.
- The nested Agent can use `bash`, `getCurrentDateTime`, `getCurrentDate`, `getCurrentTime`, and `todo_write`.
- The nested Agent cannot see or call `task`.
- The nested Agent shares the existing `ToolCallProcessor` and `HookRegistry`.
- Child messages are not persisted; only the parent `task` call and returned result are persisted by the parent loop.
- A child `AskUser` permission result returns a descriptive child error instead of suspending the synchronous call.
- Preserve existing parent loop limits and approval behavior.

## Review Focus

- Tool exposure: the main dispatcher exposes `task`, while the child dispatcher exposes exactly the five base tools and no `task`.
- Message isolation: child requests contain only child-local messages and never load or append to a parent session.
- Multi-round execution: a child tool call is followed by a second model request before the final result is returned.
- Hook and permission reuse: child tools pass through `PRE_TOOL_USE` and `POST_TOOL_USE` using the shared registry.
- Synchronous failure behavior: model errors, child limits, and `AskUser` produce readable tool results without hanging the parent.

### Task 1: Define child runner and tool contract with failing tests

**Files:**
- Create: `src/main/java/com/agent/codeagent/agents/core/SubAgentRunner.java`
- Create: `src/main/java/com/agent/codeagent/tools/SubTaskTool.java`
- Test: `src/test/java/com/agent/codeagent/agents/core/SubAgentRunnerTest.java`
- Test: `src/test/java/com/agent/codeagent/tools/SubTaskToolTest.java`

**Interfaces:**
- `SubAgentRunner.run(String prompt)` returns a `String` final result and performs no persistence.
- `SubTaskTool.execute(String toolName, String arguments)` implements `ToolExecutor` and delegates the decoded prompt to `SubAgentRunner`.

- [ ] **Step 1: Write the failing runner tests**

Add tests using a deterministic fake `ChatModel`, a real `ToolDispatcher`, a real `ToolCallProcessor`, and a `HookRegistry` with test callbacks. Assert that a prompt-only child request returns the model's final text, that a tool-call response causes a second request, and that the captured child request messages do not include a parent message or a durable store.

- [ ] **Step 2: Run the focused runner tests and verify they fail**

Run: `mvn -q -Dtest=SubAgentRunnerTest test`

Expected: compilation/test failure because `SubAgentRunner` and its synchronous loop do not exist.

- [ ] **Step 3: Write the failing tool adapter tests**

Add tests asserting that `SubTaskTool` parses `{\"prompt\":\"...\"}`, delegates exactly that prompt, and returns the delegated final string; malformed JSON must return a readable error rather than throw through the dispatcher.

- [ ] **Step 4: Run the focused tool tests and verify they fail**

Run: `mvn -q -Dtest=SubTaskToolTest test`

Expected: compilation/test failure because the adapter does not exist.

### Task 2: Implement the synchronous nested loop

**Files:**
- Modify: `src/main/java/com/agent/codeagent/agents/core/SubAgentRunner.java`
- Modify: `src/main/java/com/agent/codeagent/tools/SubTaskTool.java`
- Modify: `src/main/java/com/agent/codeagent/agents/core/ConversationContext.java` only if a package-private in-memory construction helper is needed

**Interfaces:**
- `SubAgentRunner(ChatModel model, ToolDispatcher childToolDispatcher, ToolCallProcessor toolCallProcessor, HookRegistry hookRegistry)` constructs a runner with shared model/hook/processor dependencies.
- `String run(String prompt)` creates a fresh `ConversationContext`, adds one `UserMessage`, and synchronously loops until a final `AiMessage` text is available.
- Child `TOOL_EXECUTION` responses append `ToolExecutionResultMessage` to the child context and request another model round.

- [ ] **Step 1: Implement the minimal prompt-only path**

Use `ChatModel.chat(ChatRequest)` and a fresh `ConversationContext()` with no session ID or store. Return the final AI message text for a non-tool response. Keep the child dispatcher supplied by dependency injection and do not expose the parent dispatcher.

- [ ] **Step 2: Run runner tests and verify the prompt-only path passes**

Run: `mvn -q -Dtest=SubAgentRunnerTest#returnsFinalTextFromFreshChildMessages test`

Expected: PASS.

- [ ] **Step 3: Implement tool-call continuation**

For every tool request, enforce the existing maximum round/tool-call limits, call `toolCallProcessor.process`, append allowed or denied results to the local context, and continue the synchronous loop. Convert `REQUIRES_APPROVAL` to a descriptive result and terminate the child loop without waiting for approval.

- [ ] **Step 4: Run all runner tests and verify they pass**

Run: `mvn -q -Dtest=SubAgentRunnerTest test`

Expected: PASS, including multi-round tools, hook reuse, limits, model errors, and `AskUser` behavior.

- [ ] **Step 5: Implement and verify `SubTaskTool` JSON decoding**

Decode the required `prompt` property with the project's existing Jackson configuration. Return `Error: ...` for malformed or missing prompts. Run: `mvn -q -Dtest=SubTaskToolTest test`. Expected: PASS.

### Task 3: Register isolated tool sets and Spring wiring

**Files:**
- Modify: `src/main/java/com/agent/codeagent/common/ToolConfig.java`
- Modify: `src/main/java/com/agent/codeagent/agents/core/AgentLoopExecutor.java` only if bean wiring currently needs adjustment
- Test: `src/test/java/com/agent/codeagent/common/ToolConfigTest.java`
- Create or modify: `src/test/java/com/agent/codeagent/common/SubTaskToolConfigTest.java`

**Interfaces:**
- Main dispatcher registration receives a `SubTaskTool` and registers `task` with a required `prompt` string schema.
- Child dispatcher registration receives the five base tool dependencies and registers no `task` tool.

- [ ] **Step 1: Extend tool-set tests before changing configuration**

Assert that the main tool names are the five base tools plus `task`, that the child names are exactly the five base tools, and that the two dispatchers are independent instances.

- [ ] **Step 2: Run configuration tests and verify they fail**

Run: `mvn -q -Dtest=ToolConfigTest,SubTaskToolConfigTest test`

Expected: FAIL because `task` is not registered and the child dispatcher currently omits bash/todo.

- [ ] **Step 3: Implement the main and child registrations**

Inject `SubTaskTool` into the main bean, add a `registerTask` method with the required prompt schema, and expand `subAgentToolDispatcher` to reuse `registerBash`, `registerDateTime`, and `registerTodo`. Pass the same `DateTimeTool` and `TodoTool` instances so tool side effects remain shared.

- [ ] **Step 4: Run configuration tests and verify they pass**

Run: `mvn -q -Dtest=ToolConfigTest,SubTaskToolConfigTest test`

Expected: PASS with no `task` in the child set.

### Task 4: Spring integration and regression verification

**Files:**
- Modify: `src/test/java/com/agent/codeagent/agents/core/SubAgentRunnerTest.java` if constructor/wiring coverage needs completion
- Create: `src/test/java/com/agent/codeagent/common/SubTaskSpringWiringTest.java` if a focused context test is needed

- [ ] **Step 1: Add a wiring test for shared dependencies**

Verify the main dispatcher can invoke `task`, the child dispatcher cannot resolve `task`, and the `SubTaskTool` receives the same model/hook/tool-processing collaborators used by the application.

- [ ] **Step 2: Run the focused integration tests**

Run: `mvn -q -Dtest=SubTaskSpringWiringTest,SubAgentRunnerTest,SubTaskToolTest,ToolConfigTest test`

Expected: PASS. If external model auto-configuration prevents context startup, keep the unit coverage and report the environment-specific context failure separately.

- [ ] **Step 3: Run the full verification suite**

Run: `mvn -q test`; then run `mvn -q -DskipTests compile` and `git diff --check`.

Expected: all tests pass, compilation exits 0, and `git diff --check` reports no whitespace errors.

- [ ] **Step 4: Inspect the final diff**

Confirm only the spec, plan, nested-loop implementation, tool configuration, and focused tests changed; confirm no child messages are written through `JsonlConversationStore`.

