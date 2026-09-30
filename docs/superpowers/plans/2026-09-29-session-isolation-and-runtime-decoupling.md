# Session Isolation and Runtime Decoupling Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Isolate Todo state per Agent session and complete the conversation persistence dependency inversion.

**Architecture:** Add a session-scoped Todo state model, passed through the existing tool-processing and hook contexts without changing public HTTP or CLI APIs. Supply `ConversationRepository` from Spring configuration, remove the one-use prompt component, and leave the Vue application untouched.

**Tech Stack:** Java 21, Spring Boot, JUnit 5, Mockito, LangChain4j.

**Spec:** `docs/superpowers/specs/2026-09-29-session-isolation-and-runtime-decoupling-design.md`

## Global Constraints

- Backend code only; do not modify `web-ui`.
- Public Agent HTTP and CLI interfaces remain unchanged.
- Todo state remains in memory and is discarded on process exit.
- Add behavioral tests before implementation and run the full Maven test suite.

## Review Focus

- A blank session ID still receives a generated UUID and an isolated Todo state.
- Two concurrently active session IDs cannot read each other's tasks or reminder counters.
- A child Agent's Todo operations cannot mutate the parent session.
- Invalid Todo status still returns the existing validation error without changing session state.
- The default JSONL store remains selected through Spring configuration.

---

### Task 1: Session-aware Todo state

**Files:**
- Modify: `src/main/java/com/agent/codeagent/tools/TodoStore.java`
- Modify: `src/main/java/com/agent/codeagent/tools/TodoTool.java`
- Test: `src/test/java/com/agent/codeagent/tools/TodoToolTest.java`

**Interfaces:**
- Produces: `TodoStore.session(String sessionId)` returning an isolated state object with `upsert`, `formatTaskList`, `formatReminder`, `recordToolCall`, and `resetReminder` operations.
- Consumed by Task 2 through the current Agent-loop session ID.

- [x] **Step 1: Write failing Todo isolation tests**

Add tests proving equal task titles in two session IDs produce independent task lists, and invalid updates do not mutate the selected session.

- [x] **Step 2: Run the focused test to verify it fails**

Run: `./mvnw.cmd -Dtest=TodoToolTest test`
Expected: FAIL because session-aware Todo access does not exist.

- [x] **Step 3: Implement session-aware Todo state**

Introduce `TodoStore.session(String)` and move task map, sequence, and reminder counter into a per-session state object. Change `TodoTool.writeTask` to accept a session ID and operate only on that state; retain its existing validation messages.

- [x] **Step 4: Run the focused test to verify it passes**

Run: `./mvnw.cmd -Dtest=TodoToolTest test`
Expected: PASS.

### Task 2: Propagate session identity through tools and hooks

**Files:**
- Modify: `src/main/java/com/agent/codeagent/agents/core/ToolCallProcessor.java`
- Modify: `src/main/java/com/agent/codeagent/agents/core/AgentLoopExecutor.java`
- Modify: `src/main/java/com/agent/codeagent/agents/core/SubAgentRunner.java`
- Modify: `src/main/java/com/agent/codeagent/agents/hook/core/HookContext.java`
- Modify: `src/main/java/com/agent/codeagent/tools/TodoHookRegistrar.java`
- Modify: `src/main/java/com/agent/codeagent/tools/TodoReminderHook.java`
- Test: `src/test/java/com/agent/codeagent/tools/TodoReminderHookTest.java`
- Test: `src/test/java/com/agent/codeagent/agents/core/AgentLoopExecutorTest.java`

**Interfaces:**
- Consumes: `TodoStore.session(String)` from Task 1.
- Produces: session ID stored in `HookContext` / tool-processing calls; child loops use a unique ephemeral session ID.

- [x] **Step 1: Write failing propagation tests**

Add tests that record three non-Todo calls for session A, verify no reminder for session B, and verify the executor passes its conversation session ID to tool processing.

- [x] **Step 2: Run focused tests to verify they fail**

Run: `./mvnw.cmd -Dtest=TodoReminderHookTest,AgentLoopExecutorTest test`
Expected: FAIL because hooks and processors do not carry a session ID.

- [x] **Step 3: Implement session propagation**

Extend `ToolCallProcessor` and `HookContext` with a session ID while preserving existing test-friendly constructors, pass `ConversationContext.sessionId()` from `AgentLoopExecutor`, and use a UUID-backed ephemeral ID in `SubAgentRunner`. Update Todo registration/reminder code to resolve state with that ID.

- [x] **Step 4: Run focused tests to verify they pass**

Run: `./mvnw.cmd -Dtest=TodoReminderHookTest,AgentLoopExecutorTest test`
Expected: PASS.

### Task 3: Wire conversation persistence through the port

**Files:**
- Modify: `src/main/java/com/agent/codeagent/common/AgentRuntimeConfig.java`
- Modify: `src/main/java/com/agent/codeagent/agents/core/SessionService.java`
- Modify: `src/main/java/com/agent/codeagent/agents/core/AgentLoopExecutor.java`
- Delete: `src/main/java/com/agent/codeagent/agents/core/PromptBuilder.java`
- Test: `src/test/java/com/agent/codeagent/agents/core/SessionServiceTest.java`
- Test: `src/test/java/com/agent/codeagent/agents/core/AgentLoopExecutorTest.java`

**Interfaces:**
- Produces: a `ConversationRepository` Spring bean backed by `JsonlConversationStore` and a `SessionService(ConversationRepository)` runtime dependency.

- [x] **Step 1: Write failing repository-wiring tests**

Add a test using a fake `ConversationRepository` to prove `SessionService.open` calls only the injected port, plus an executor construction test without `PromptBuilder`.

- [x] **Step 2: Run focused tests to verify they fail**

Run: `./mvnw.cmd -Dtest=SessionServiceTest,AgentLoopExecutorTest test`
Expected: FAIL because runtime configuration has no repository bean or because the executor still requires `PromptBuilder`.

- [x] **Step 3: Implement configuration-owned persistence and remove PromptBuilder**

Declare the JSONL implementation as a configuration bean, inject `ConversationRepository` into `SessionService`, and move prompt combination into a private `AgentLoopExecutor` method. Update constructor injection and tests accordingly.

- [x] **Step 4: Run focused tests to verify they pass**

Run: `./mvnw.cmd -Dtest=SessionServiceTest,AgentLoopExecutorTest test`
Expected: PASS.

### Task 4: Full regression verification

**Files:**
- Modify: only files required by Tasks 1–3.

- [x] **Step 1: Run the complete backend suite**

Run: `./mvnw.cmd test`
Expected: BUILD SUCCESS with zero failures and zero errors.

- [x] **Step 2: Inspect the final diff**

Run: `git diff --check` and `git diff --stat`
Expected: no whitespace errors and only backend/spec/plan changes.
