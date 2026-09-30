# JSONL Session Persistence Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Persist each agent session in an append-only JSONL file and create a new session when the client omits `sessionId`.

**Architecture:** A focused file-backed store loads normalized message records and appends one record per durable message. The loop executor creates/resumes a context and keeps the session id in approval continuations; the controller exposes it through SSE and accepts it on resume.

**Tech Stack:** Java 21, Spring Boot, Jackson, LangChain4j, JUnit 5, Maven.

**Spec:** `docs/superpowers/specs/2026-09-22-jsonl-session-persistence-design.md`

## Global Constraints

- One session maps to one `${sessionId}.jsonl` file.
- Each durable message is one UTF-8 JSON line with a trailing newline.
- Appending must not rewrite existing bytes and must be serialized per session.
- Temporary reminders are request-local and are never persisted.
- Omitted `sessionId` starts a new session.

## Review Focus

- User, assistant/tool-call, and tool-result records must round-trip without losing fields.
- A truncated final line must not discard earlier records.
- Concurrent appends for one session must not interleave JSON records.
- Path separators and traversal segments must be rejected.
- Approval continuation must append to and reload the same session.

### Task 1: Message codec and JSONL store

**Files:**
- Create: `src/main/java/com/agent/codeagent/agents/core/ConversationMessageCodec.java`
- Create: `src/main/java/com/agent/codeagent/agents/core/JsonlConversationStore.java`
- Test: `src/test/java/com/agent/codeagent/agents/core/JsonlConversationStoreTest.java`

**Interfaces:** `JsonlConversationStore.load(String): List<ChatMessage>` and `append(String, ChatMessage): void`; the codec converts durable LangChain4j message kinds to/from Jackson JSON objects.

- [ ] Write failing tests for append-only growth, reload order, round trips, invalid ids, and incomplete tails.
- [ ] Run `mvn -q -Dtest=JsonlConversationStoreTest test` and confirm the expected missing-class/API failures.
- [ ] Implement normalized records and per-session locking with `Files.newBufferedWriter(..., CREATE, APPEND)`.
- [ ] Run the focused test and confirm it passes.

### Task 2: Durable conversation context

**Files:**
- Modify: `src/main/java/com/agent/codeagent/agents/core/ConversationContext.java`
- Modify: `src/test/java/com/agent/codeagent/agents/core/ConversationContextTest.java`

**Interfaces:** Add construction with a session id and store, load messages on creation, and make `add` append after updating the in-memory list.

- [ ] Write a failing test proving a new context reloads previously appended messages and reminders remain request-local.
- [ ] Run the focused test and confirm failure before production changes.
- [ ] Implement store-backed context construction while preserving the existing no-arg constructor for compatibility.
- [ ] Run the focused test and confirm it passes.

### Task 3: Session lifecycle and approval propagation

**Files:**
- Modify: `src/main/java/com/agent/codeagent/agents/core/AgentLoopExecutor.java`
- Modify: `src/main/java/com/agent/codeagent/agents/core/ApprovalManager.java`
- Modify: `src/main/java/com/agent/codeagent/agents/core/AgentEvent.java`
- Modify: `src/main/java/com/agent/codeagent/agents/core/Agent.java`
- Modify: `src/main/java/com/agent/codeagent/agents/core/AgentFacade.java`
- Test: `src/test/java/com/agent/codeagent/agents/core/AgentLoopExecutorTest.java`

**Interfaces:** `execute(String sessionId, String userMessage)` creates an id when null/blank and emits it; `approve(String requestId, boolean approved)` uses the stored context/session continuation.

- [ ] Write failing tests for no-id session creation and same-session approval continuation.
- [ ] Run focused tests and confirm failure.
- [ ] Inject/configure the store, create or load contexts, and carry session metadata through approval state.
- [ ] Run focused tests and confirm pass.

### Task 4: HTTP contract and configuration

**Files:**
- Modify: `src/main/java/com/agent/codeagent/web/AgentController.java`
- Modify: `src/main/resources/application.yaml`
- Modify: `src/test/java/com/agent/codeagent/CodeAgentApplicationTests.java`

- [ ] Add optional `sessionId` request parameters and expose the generated id as a session event/header-compatible event.
- [ ] Configure the default storage directory and test application context startup.
- [ ] Run `mvn -q test` and inspect the complete result.
