# Agent Core Responsibility Refactor Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Split `Agent` responsibilities into focused collaborators without changing the public facade or streaming event protocol.

**Architecture:** Keep `AgentFacade` as the public boundary and make `Agent` a compatibility coordinator. Extract conversation state, model callback adaptation, tool processing, and approval lifecycle into focused classes used by the loop executor.

**Tech Stack:** Java 21, Spring Boot 4.1, Reactor Flux, LangChain4j 1.3, JUnit 5.

**Spec:** `docs/superpowers/specs/2026-09-22-agent-core-responsibility-design.md`

## Global Constraints

- Existing `AgentFacade` methods and controller endpoints remain source-compatible.
- Existing `AgentEvent` type values remain unchanged.
- Todo reminders are request-local and must not mutate durable conversation history.
- Approval requests expire after five minutes and must release retained state.
- Agent execution remains bounded at 20 rounds and 50 tool calls.

## Review Focus

- A reminder must be visible to the current model request but absent from the durable message history; test this in `ConversationContextTest`.
- An expired approval must complete the original stream and remove retained state; test this in `ApprovalManagerTest`.
- Denied, allowed, and approval-required tool calls must each produce the correct message and event; test this in `ToolCallProcessorTest`.
- A model callback after cancellation must not emit events or recurse; test this in `ModelStreamAdapterTest`.
- Loop limits must terminate with an error rather than recurse indefinitely; test this in `AgentLoopExecutorTest`.

### Task 1: Extract conversation context

**Files:**
- Create: `src/main/java/com/agent/codeagent/agents/core/ConversationContext.java`
- Test: `src/test/java/com/agent/codeagent/agents/core/ConversationContextTest.java`
- Modify: `src/main/java/com/agent/codeagent/agents/core/Agent.java`

**Interfaces:** `ConversationContext` owns `add`, `snapshotForRequest`, and `messages` operations. `snapshotForRequest(String reminder)` returns a new list and never mutates stored history.

- [ ] Write the failing test for request-local reminder copies.
- [ ] Run `mvn -q -Dtest=ConversationContextTest test` and confirm failure.
- [ ] Implement the minimal context class.
- [ ] Run the focused test and then the existing Todo tests.

### Task 2: Extract model stream adaptation

**Files:**
- Create: `src/main/java/com/agent/codeagent/agents/core/ModelStreamAdapter.java`
- Test: `src/test/java/com/agent/codeagent/agents/core/ModelStreamAdapterTest.java`
- Modify: `src/main/java/com/agent/codeagent/agents/core/Agent.java`

**Interfaces:** `ModelStreamAdapter.handler(FluxSink<AgentEvent>, Consumer<ChatResponse>, Runnable)` returns a `StreamingChatResponseHandler` and guards emissions after cancellation.

- [ ] Write callback-to-event tests.
- [ ] Run the focused test and confirm failure.
- [ ] Implement the adapter and replace duplicate callbacks in `Agent`.
- [ ] Run focused tests and compile.

### Task 3: Extract tool-call processing

**Files:**
- Create: `src/main/java/com/agent/codeagent/agents/core/ToolCallProcessor.java`
- Create: `src/main/java/com/agent/codeagent/agents/core/ToolProcessResult.java`
- Test: `src/test/java/com/agent/codeagent/agents/core/ToolCallProcessorTest.java`
- Modify: `src/main/java/com/agent/codeagent/agents/core/Agent.java`

**Interfaces:** `ToolCallProcessor.process(List<ToolExecutionRequest>, List<ChatMessage>, FluxSink<AgentEvent>)` performs permission checks and returns either completed processing or an approval request.

- [ ] Write tests for allowed, denied, and approval-required calls.
- [ ] Run focused tests and confirm failure.
- [ ] Implement the processor using existing `ToolDispatcher` and `HookRegistry`.
- [ ] Replace the corresponding Agent methods and run all hook/tool tests.

### Task 4: Extract approval lifecycle

**Files:**
- Create: `src/main/java/com/agent/codeagent/agents/core/ApprovalManager.java`
- Test: `src/test/java/com/agent/codeagent/agents/core/ApprovalManagerTest.java`
- Modify: `src/main/java/com/agent/codeagent/agents/core/Agent.java`

**Interfaces:** `ApprovalManager.suspend(...)`, `approve(requestId, approved, sink)`, `expirePending()`, and `removeForSink(...)` own pending state and five-minute expiry.

- [ ] Write tests for suspend, approve, expiry, and sink cleanup.
- [ ] Run focused tests and confirm failure.
- [ ] Implement the manager with the existing TTL.
- [ ] Delegate Agent approval methods to it and run all tests.

### Task 5: Introduce loop executor and finish coordinator migration

**Files:**
- Create: `src/main/java/com/agent/codeagent/agents/core/AgentLoopExecutor.java`
- Test: `src/test/java/com/agent/codeagent/agents/core/AgentLoopExecutorTest.java`
- Modify: `src/main/java/com/agent/codeagent/agents/core/Agent.java`

**Interfaces:** `AgentLoopExecutor.execute(String)` and `resume(...)` own ReAct transitions while `Agent` delegates public compatibility methods.

- [ ] Write tests for round/tool limits and terminal completion.
- [ ] Run focused tests and confirm failure.
- [ ] Implement the executor using the extracted collaborators.
- [ ] Reduce Agent to a thin coordinator and run the complete non-network suite.

### Task 6: Final verification

- [ ] Run `mvn -q -DskipTests compile`.
- [ ] Run `mvn -q "-Dtest=!CodeAgentApplicationTests" test`.
- [ ] Run `mvn test` and document any environment-only failures.
- [ ] Inspect `git diff` and confirm unrelated working-tree changes were preserved.
