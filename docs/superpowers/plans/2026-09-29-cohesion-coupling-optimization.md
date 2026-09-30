# Cohesion and Coupling Optimization Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development to implement this plan task-by-task.

**Goal:** Improve cohesion and reduce coupling in the Agent core while preserving the existing HTTP, SSE, tool, and JSONL contracts.

**Architecture:** Keep `AgentLoopExecutor` as the orchestration boundary, inject its collaborators, and introduce focused ports for conversation persistence and shared loop policy. Separate Todo state/tool handling from Hook registration. Preserve LangChain4j and Spring at the integration edges.

**Tech Stack:** Java 21, Spring Boot, Reactor, LangChain4j, Jackson, JUnit 5, Mockito, Maven.

**Spec:** `docs/superpowers/specs/2026-09-28-agent-architecture-optimization-design.md`

## Global Constraints

- Preserve `GET /agent/loop` and `GET /agent/loop/approve` parameters and SSE event names.
- Preserve existing tool names: `bash`, `todo_write`, `task`, and date/time tools.
- Preserve the append-only JSONL session format.
- Do not change LangChain4j or add unrelated frontend changes.
- Production changes require a focused test before implementation and a full `mvn test` verification.

## Review Focus

- A custom conversation repository must receive every appended message exactly once.
- A mutable conversation message list must not be exposed to callers.
- Approval and tool-processing collaborators must be replaceable in `AgentLoopExecutor` tests.
- Main and child Agent loops must retain their distinct streaming/synchronous behavior without duplicated policy bugs.
- Todo Hook registration must not make the Todo tool responsible for unrelated lifecycle wiring.

### Task 1: Conversation persistence boundary

**Files:**
- Create: `src/main/java/com/agent/codeagent/agents/core/ConversationRepository.java`
- Modify: `src/main/java/com/agent/codeagent/agents/core/JsonlConversationStore.java`
- Modify: `src/main/java/com/agent/codeagent/agents/core/ConversationContext.java`
- Modify: `src/main/java/com/agent/codeagent/agents/core/SessionService.java`
- Test: `src/test/java/com/agent/codeagent/agents/core/SessionServiceTest.java`
- Test: `src/test/java/com/agent/codeagent/agents/core/ConversationContextTest.java`

**Interfaces:**
- `ConversationRepository.load(String): List<ChatMessage>`
- `ConversationRepository.append(String, ChatMessage): void`
- `JsonlConversationStore` implements the repository.
- `ConversationContext.messages()` returns an immutable snapshot.

- [ ] Add tests for repository substitution, append-once behavior, and immutable message snapshots.
- [ ] Run the focused tests and confirm they fail for the missing boundary/behavior.
- [ ] Implement the repository interface and update context/session wiring without changing JSONL records.
- [ ] Run focused tests, then the existing session and persistence tests.

### Task 2: Injectable Agent collaborators

**Files:**
- Modify: `src/main/java/com/agent/codeagent/agents/core/AgentLoopExecutor.java`
- Modify: `src/main/java/com/agent/codeagent/agents/core/ToolCallProcessor.java`
- Modify: `src/main/java/com/agent/codeagent/agents/core/ApprovalManager.java`
- Modify: `src/test/java/com/agent/codeagent/agents/core/AgentLoopExecutorTest.java`

**Interfaces:**
- `AgentLoopExecutor` receives `ToolCallProcessor` and `ApprovalManager` through its constructor.
- Existing production behavior and public methods remain unchanged.

- [ ] Add a constructor-level test proving supplied processor and approval manager instances are used.
- [ ] Run the focused test and confirm it fails against internal construction.
- [ ] Inject the collaborators and preserve approval timeout/cleanup behavior.
- [ ] Run Agent loop, approval, and lifecycle tests.

### Task 3: Shared loop policy for main and child agents

**Files:**
- Create: `src/main/java/com/agent/codeagent/agents/core/AgentLoopLimits.java`
- Modify: `src/main/java/com/agent/codeagent/agents/core/AgentLoopExecutor.java`
- Modify: `src/main/java/com/agent/codeagent/agents/core/SubAgentRunner.java`
- Modify: `src/main/java/com/agent/codeagent/common/ToolConfig.java`
- Test: `src/test/java/com/agent/codeagent/agents/core/SubAgentRunnerTest.java`

**Interfaces:**
- `AgentLoopLimits` exposes the existing maximum rounds and tool calls as immutable values.
- Main and child loops consume the shared limits; streaming and synchronous execution remain separate.

- [ ] Add tests that both loop variants stop at the configured limits and still execute allowed tools.
- [ ] Run focused tests and confirm the shared policy behavior is not yet present.
- [ ] Extract only the duplicated policy/constants, not the incompatible streaming control flow.
- [ ] Run all core loop and sub-agent tests.

### Task 4: Todo tool and Hook separation

**Files:**
- Create: `src/main/java/com/agent/codeagent/tools/TodoHookRegistrar.java`
- Modify: `src/main/java/com/agent/codeagent/tools/TodoTool.java`
- Modify: `src/main/java/com/agent/codeagent/tools/TodoStore.java`
- Modify: `src/main/java/com/agent/codeagent/common/ToolConfig.java`
- Test: `src/test/java/com/agent/codeagent/tools/TodoToolTest.java`
- Test: `src/test/java/com/agent/codeagent/tools/TodoReminderHookTest.java`

**Interfaces:**
- `TodoTool` handles tool input and delegates state operations to `TodoStore`.
- `TodoHookRegistrar` implements `HookRegistrar` and owns reminder/permission lifecycle registration.
- Existing `todo_write` output and statuses remain unchanged.

- [ ] Add tests proving tool behavior works without registering hooks and Hook registration still produces reminders/reset behavior.
- [ ] Run focused tests and confirm the separated registration is absent.
- [ ] Move lifecycle wiring into `TodoHookRegistrar`, keeping tool output compatible.
- [ ] Run all Todo, Hook, and tool configuration tests.

### Task 5: Integration and architecture verification

**Files:**
- Modify only files required to resolve integration conflicts.
- Test: existing full test suite.

- [ ] Review the combined diff for overlapping responsibilities and accidental public contract changes.
- [ ] Run `mvn test` from the repository root.
- [ ] Run `mvn package -DskipTests` to verify compilation/package assembly.
- [ ] Report any environment-only warnings separately from code failures.
