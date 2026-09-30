# Agent Error Handling and Observability Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Deliver safe, recoverable Agent failures with stable SSE errors, bounded retry, durable-session continuation, and traceable operational telemetry.

**Architecture:** Create a run-scoped error domain and route all terminal failures through one `RunFailureHandler`. Preserve `AgentLoopExecutor` as the ReAct orchestrator, while model callbacks, tool outcomes, approvals, and persistence report typed failures to that handler. The Vue stream client renders only the server-supplied recovery action; Micrometer/OTel instrumentation correlates the run across logs, traces, metrics, and alerts.

**Tech Stack:** Java 21, Spring Boot 4.1, Reactor, LangChain4j, JUnit 5/Mockito, Vue 3/Vite, Vitest, Spring Boot Actuator, Micrometer Tracing, OpenTelemetry exporter.

**Spec:** `docs/superpowers/specs/2026-09-30-agent-error-handling-observability-design.md`

## Global Constraints

- Never return a Java exception, stack trace, prompt, secret, or complete tool arguments/output to SSE/HTTP consumers.
- Every terminal SSE error includes `code`, `sessionId`, `runId`, and `traceId`.
- Retry at most once, only for recognized transient model failures before any text/thinking output or tool execution.
- A non-idempotent write tool is never automatically replayed.
- `CONTINUE_SESSION` uses the existing `sessionId` and a new explicit continuation message; it never resubmits the original message automatically.
- Keep existing durable JSONL conversation behavior and public `/agent/loop` / `/agent/loop/approve` endpoints compatible unless a versioned endpoint is introduced.
- Observability data contains only allowlisted metadata, lengths, hashes, or redacted summaries.

## Review Focus

- A provider may invoke `onError` after a partial token callback; assert this never schedules a retry and emits one terminal error.
- An SSE client can cancel while a run is awaiting approval; assert pending approval cleanup emits no later error to the cancelled client.
- JSONL append may fail after a model completion; assert the client receives a safe persistence code and no duplicate completion.
- A retryable write-tool transport failure with the same `operationId` must not execute the external mutation twice.
- A browser may receive malformed or legacy error data; assert it displays a generic safe message without guessing a recovery action.

---

## File Structure

- Create `src/main/java/com/agent/codeagent/agents/error/ErrorCode.java`: stable error-code catalog and metadata.
- Create `src/main/java/com/agent/codeagent/agents/error/RecoveryAction.java`: client recovery action enum.
- Create `src/main/java/com/agent/codeagent/agents/error/AgentError.java`: safe SSE/HTTP failure DTO implementing `AgentEvent`.
- Create `src/main/java/com/agent/codeagent/agents/execution/RunContext.java`: per-run IDs, phase, output/tool state, and retry count.
- Create `src/main/java/com/agent/codeagent/agents/execution/RetryPolicy.java`: pure retry eligibility and jittered delay calculation.
- Create `src/main/java/com/agent/codeagent/agents/execution/RunFailureHandler.java`: one terminal failure path, error mapping, telemetry, and sink completion.
- Create `src/main/java/com/agent/codeagent/agents/tools/ToolOutcome.java` and `ToolSafety.java`: structured tool success/failure and retry-safety metadata.
- Create `src/main/java/com/agent/codeagent/agents/observability/AgentObservability.java`: logs, Micrometer counters/timers, and trace attributes.
- Modify `src/main/java/com/agent/codeagent/agents/api/AgentEvent.java`: remove raw-throwable error event and add safe event types only.
- Modify `src/main/java/com/agent/codeagent/agents/execution/AgentLoopExecutor.java`, `ModelStreamAdapter.java`, `agents/tools/ToolCallProcessor.java`, `agents/approval/ApprovalManager.java`, and conversation classes: propagate `RunContext`, typed outcomes, and centralized failures.
- Modify `src/main/java/com/agent/codeagent/common/AgentRuntimeConfig.java`, `pom.xml`, and `src/main/resources/application.yaml`: configure Actuator, tracing, and retry/telemetry settings.
- Modify `web-ui/src/services/agentEvents.js` and `agentApi.js`; create `web-ui/src/services/agentRecovery.js`: normalize safe errors and own recovery-state transitions.
- Modify the Agent-chat view when it is introduced or identified; keep recovery UI separate from transport parsing.
- Add focused JUnit tests next to the owned backend package and Vitest tests under `web-ui/src/services/`.

### Task 1: Safe error domain and SSE serialization

**Files:**
- Create: `src/main/java/com/agent/codeagent/agents/error/ErrorCode.java`
- Create: `src/main/java/com/agent/codeagent/agents/error/RecoveryAction.java`
- Create: `src/main/java/com/agent/codeagent/agents/error/AgentError.java`
- Modify: `src/main/java/com/agent/codeagent/agents/api/AgentEvent.java`
- Test: `src/test/java/com/agent/codeagent/agents/api/AgentEventSerializationTest.java`
- Test: `src/test/java/com/agent/codeagent/agents/error/AgentErrorTest.java`

**Interfaces:**
- Consumes: existing `AgentEvent` sealed interface and Jackson SSE serialization.
- Produces: `AgentError(ErrorCode code, String message, boolean recoverable, RecoveryAction action, String sessionId, String runId, String traceId, Long retryAfterMs)` implementing `AgentEvent`; `ErrorCode` metadata determines default safe copy and action.

- [ ] **Step 1: Write failing serialization and catalog tests**

Assert `MODEL_TIMEOUT_001` serializes as `MODEL-TIMEOUT-001`, `AgentError.type()` is `error`, all correlation IDs are present, and no constructor/JSON property accepts a `Throwable`.

- [ ] **Step 2: Run the focused tests to verify they fail**

Run: `./mvnw.cmd -Dtest=AgentEventSerializationTest,AgentErrorTest test`

Expected: FAIL because `AgentError`, `ErrorCode`, and `RecoveryAction` do not exist.

- [ ] **Step 3: Implement the error contract**

Create the catalog with every code from the spec, `RecoveryAction` values `RETRY`, `CONTINUE_SESSION`, `REAUTHORIZE`, `START_NEW_SESSION`, and `CONTACT_SUPPORT`, and an immutable safe `AgentError`. Replace `AgentEvent.Error(Throwable)` with the safe error record.

- [ ] **Step 4: Run the focused tests to verify they pass**

Run: `./mvnw.cmd -Dtest=AgentEventSerializationTest,AgentErrorTest test`

Expected: PASS.

- [ ] **Step 5: Commit the contract**

```bash
git add src/main/java/com/agent/codeagent/agents/error src/main/java/com/agent/codeagent/agents/api/AgentEvent.java src/test/java/com/agent/codeagent/agents/api/AgentEventSerializationTest.java src/test/java/com/agent/codeagent/agents/error/AgentErrorTest.java
git commit -m "feat: add safe agent error contract"
```

### Task 2: Run context, retry policy, and centralized terminal failure handling

**Files:**
- Create: `src/main/java/com/agent/codeagent/agents/execution/RunContext.java`
- Create: `src/main/java/com/agent/codeagent/agents/execution/RetryPolicy.java`
- Create: `src/main/java/com/agent/codeagent/agents/execution/RunFailureHandler.java`
- Modify: `src/main/java/com/agent/codeagent/agents/execution/ModelStreamAdapter.java`
- Test: `src/test/java/com/agent/codeagent/agents/execution/RetryPolicyTest.java`
- Test: `src/test/java/com/agent/codeagent/agents/execution/RunFailureHandlerTest.java`
- Test: `src/test/java/com/agent/codeagent/agents/execution/ModelStreamAdapterTest.java`

**Interfaces:**
- Consumes: `AgentError`, `ErrorCode`, `RecoveryAction`, Reactor `FluxSink<AgentEvent>`.
- Produces: `RunContext.start(String sessionId, String traceId)`, `RunContext.markOutputEmitted()`, `RunContext.markToolStarted()`, `RetryPolicy.shouldRetry(RunContext, Throwable)`, and `RunFailureHandler.fail(RunContext, FluxSink<AgentEvent>, Throwable)`.

- [ ] **Step 1: Write failing state-machine tests**

Test one retry for a timeout before output/tool start, no retry after partial output, no retry after a tool starts, bounded jittered delay, and exactly one safe terminal event when `onError` follows a partial response.

- [ ] **Step 2: Run the focused tests to verify they fail**

Run: `./mvnw.cmd -Dtest=RetryPolicyTest,RunFailureHandlerTest,ModelStreamAdapterTest test`

Expected: FAIL because run context and centralized handler do not exist.

- [ ] **Step 3: Implement run state and failure ownership**

Implement a terminal-once `RunContext`; make `RetryPolicy` recognize only explicitly mapped model/network/rate-limit exceptions; have `ModelStreamAdapter` mark output/thinking state and report callback errors to `RunFailureHandler` instead of creating an error event itself. Keep delay generation injectable so tests are deterministic.

- [ ] **Step 4: Run the focused tests to verify they pass**

Run: `./mvnw.cmd -Dtest=RetryPolicyTest,RunFailureHandlerTest,ModelStreamAdapterTest test`

Expected: PASS.

- [ ] **Step 5: Commit the run failure primitives**

```bash
git add src/main/java/com/agent/codeagent/agents/execution src/test/java/com/agent/codeagent/agents/execution
git commit -m "feat: centralize agent run failures"
```

### Task 3: Integrate failure handling into the ReAct loop, session store, and approval lifecycle

**Files:**
- Modify: `src/main/java/com/agent/codeagent/agents/execution/AgentLoopExecutor.java`
- Modify: `src/main/java/com/agent/codeagent/agents/conversation/SessionService.java`
- Modify: `src/main/java/com/agent/codeagent/agents/conversation/ConversationContext.java`
- Modify: `src/main/java/com/agent/codeagent/agents/conversation/JsonlConversationStore.java`
- Modify: `src/main/java/com/agent/codeagent/agents/approval/ApprovalManager.java`
- Test: `src/test/java/com/agent/codeagent/agents/execution/AgentLoopExecutorTest.java`
- Test: `src/test/java/com/agent/codeagent/agents/conversation/JsonlConversationStoreTest.java`
- Test: `src/test/java/com/agent/codeagent/agents/approval/ApprovalManagerTest.java`

**Interfaces:**
- Consumes: Task 2 `RunContext`, `RetryPolicy`, and `RunFailureHandler`.
- Produces: a run-aware `AgentLoopExecutor.execute(String sessionId, String userMessage)` that emits one `SessionStarted`, then normal events or one safe terminal error.

- [ ] **Step 1: Write failing integration tests**

Cover malformed session IDs, JSONL load failure, append failure after model completion, unknown/expired approval ID, approval expiry, loop-limit failure, and model retry followed by successful completion. Assert terminal errors contain the run/session/trace IDs and no raw exception message.

- [ ] **Step 2: Run the focused tests to verify they fail**

Run: `./mvnw.cmd -Dtest=AgentLoopExecutorTest,JsonlConversationStoreTest,ApprovalManagerTest test`

Expected: FAIL because the loop has direct error emission and persistence/approval failures lack safe mapping.

- [ ] **Step 3: Wire the centralized handler through runtime boundaries**

Create the `RunContext` before session opening, obtain trace ID from the active observation, retry only the model request when `RetryPolicy` permits, route `SessionService`/store/approval/loop-limit failures to `RunFailureHandler`, and ensure `complete` is emitted only after persistence succeeds. Preserve cancellation cleanup without emitting to a cancelled sink.

- [ ] **Step 4: Run the focused tests to verify they pass**

Run: `./mvnw.cmd -Dtest=AgentLoopExecutorTest,JsonlConversationStoreTest,ApprovalManagerTest test`

Expected: PASS.

- [ ] **Step 5: Commit loop integration**

```bash
git add src/main/java/com/agent/codeagent/agents/execution/AgentLoopExecutor.java src/main/java/com/agent/codeagent/agents/conversation src/main/java/com/agent/codeagent/agents/approval src/test/java/com/agent/codeagent/agents/execution/AgentLoopExecutorTest.java src/test/java/com/agent/codeagent/agents/conversation/JsonlConversationStoreTest.java src/test/java/com/agent/codeagent/agents/approval/ApprovalManagerTest.java
git commit -m "feat: recover agent loop failures safely"
```

### Task 4: Structured tool outcomes and idempotent-write boundary

**Files:**
- Create: `src/main/java/com/agent/codeagent/agents/tools/ToolOutcome.java`
- Create: `src/main/java/com/agent/codeagent/agents/tools/ToolSafety.java`
- Modify: `src/main/java/com/agent/codeagent/agents/tools/ToolCallProcessor.java`
- Modify: `src/main/java/com/agent/codeagent/agents/tool/ToolDispatcher.java`
- Modify: tool registration/configuration classes under `src/main/java/com/agent/codeagent/common/`
- Test: `src/test/java/com/agent/codeagent/agents/tools/ToolCallProcessorTest.java`
- Test: `src/test/java/com/agent/codeagent/agents/tool/ToolDispatcherTest.java`

**Interfaces:**
- Consumes: Task 1 `ErrorCode`, Task 2 `RunContext` and retry policy.
- Produces: `ToolOutcome.success(String sanitizedContent)`, `ToolOutcome.failure(ErrorCode code, String sanitizedContent, ToolSafety safety, String operationId)`, and per-tool safety registration.

- [ ] **Step 1: Write failing tool outcome tests**

Assert validation errors map to `TOOL-VALIDATION-001`, transient read-only failures are retry candidates, a non-idempotent write failure is not, and a duplicate `operationId` returns the recorded result without a second dispatch.

- [ ] **Step 2: Run the focused tests to verify they fail**

Run: `./mvnw.cmd -Dtest=ToolCallProcessorTest,ToolDispatcherTest test`

Expected: FAIL because tools return raw strings and have no safety metadata.

- [ ] **Step 3: Implement typed tool outcomes and de-duplication seam**

Change `ToolCallProcessor` to preserve sanitized model context while returning a typed outcome. Add dispatcher support for an optional `operationId` and register all current tools as `READ_ONLY` or `NON_IDEMPOTENT_WRITE` explicitly; add `IDEMPOTENT_WRITE` only where a concrete backend dedupe implementation exists.

- [ ] **Step 4: Run the focused tests to verify they pass**

Run: `./mvnw.cmd -Dtest=ToolCallProcessorTest,ToolDispatcherTest test`

Expected: PASS.

- [ ] **Step 5: Commit tool safety behavior**

```bash
git add src/main/java/com/agent/codeagent/agents/tools src/main/java/com/agent/codeagent/agents/tool src/main/java/com/agent/codeagent/common src/test/java/com/agent/codeagent/agents/tools src/test/java/com/agent/codeagent/agents/tool
git commit -m "feat: classify tool failures and retry safety"
```

### Task 5: Add observability dependencies, telemetry, and operational configuration

**Files:**
- Modify: `pom.xml`
- Modify: `src/main/resources/application.yaml`
- Modify: `src/main/java/com/agent/codeagent/common/AgentRuntimeConfig.java`
- Create: `src/main/java/com/agent/codeagent/agents/observability/AgentObservability.java`
- Test: `src/test/java/com/agent/codeagent/agents/observability/AgentObservabilityTest.java`
- Test: `src/test/java/com/agent/codeagent/common/AgentRuntimeConfigTest.java`
- Create: `docs/operations/agent-observability.md`

**Interfaces:**
- Consumes: run events from Tasks 2–4 and Spring `MeterRegistry` / Micrometer observation APIs.
- Produces: `AgentObservability.runStarted(RunContext)`, `runCompleted(RunContext)`, `runFailed(RunContext, ErrorCode, Throwable)`, `modelAttempt(...)`, `toolFinished(...)`, `approvalChanged(...)`, and `sessionStoreFinished(...)`.

- [ ] **Step 1: Write failing telemetry and configuration tests**

Verify configured retry maximum is one, model-failure counters include `error_code/provider/model`, durations are recorded, and log-safe attributes exclude raw message, arguments, and exception text.

- [ ] **Step 2: Run the focused tests to verify they fail**

Run: `./mvnw.cmd -Dtest=AgentObservabilityTest,AgentRuntimeConfigTest test`

Expected: FAIL because telemetry facade and runtime properties do not exist.

- [ ] **Step 3: Add dependencies and implement the observability facade**

Add Actuator, Micrometer Tracing, and the selected OpenTelemetry exporter at compatible Spring Boot-managed versions. Implement counters/timers from the spec, create spans/attributes around run, model attempts, tools, and persistence, and configure JSON structured logging with allowlisted fields. Document PromQL alert rules and dashboard panels in `docs/operations/agent-observability.md`.

- [ ] **Step 4: Run the focused tests to verify they pass**

Run: `./mvnw.cmd -Dtest=AgentObservabilityTest,AgentRuntimeConfigTest test`

Expected: PASS.

- [ ] **Step 5: Commit observability support**

```bash
git add pom.xml src/main/resources/application.yaml src/main/java/com/agent/codeagent/common/AgentRuntimeConfig.java src/main/java/com/agent/codeagent/agents/observability src/test/java/com/agent/codeagent/common/AgentRuntimeConfigTest.java src/test/java/com/agent/codeagent/agents/observability docs/operations/agent-observability.md
git commit -m "feat: observe agent runs and failures"
```

### Task 6: Frontend error normalization and recovery state

**Files:**
- Modify: `web-ui/src/services/agentEvents.js`
- Modify: `web-ui/src/services/agentApi.js`
- Create: `web-ui/src/services/agentRecovery.js`
- Test: `web-ui/src/services/agentEvents.test.js`
- Test: `web-ui/src/services/agentRecovery.test.js`

**Interfaces:**
- Consumes: Task 1 SSE `AgentError` schema.
- Produces: `normalizeAgentError(payload)`, `createRecoveryState()`, `reduceRecovery(state, event)`, and `buildRecoveryRequest(state)` returning either a retry request or a same-session continuation request.

- [ ] **Step 1: Write failing frontend tests**

Assert a structured `RETRY` error retains IDs and retry delay, a `CONTINUE_SESSION` error preserves generated text and creates a continuation request with the same `sessionId`, malformed/legacy errors render a safe generic message with no guessed action, and abort does not become an infrastructure error.

- [ ] **Step 2: Run the focused tests to verify they fail**

Run: `npm --prefix web-ui test -- --run src/services/agentEvents.test.js src/services/agentRecovery.test.js`

Expected: FAIL because recovery state and safe error normalization do not exist.

- [ ] **Step 3: Implement transport-safe recovery state**

Normalize only the declared `code`, `message`, `recoverable`, `action`, correlation IDs, and `retryAfterMs`. Store original message only in local recovery state; never automatically invoke a retry/continue request. Keep parsing separate from UI components so a later chat screen can bind directly to the reducer.

- [ ] **Step 4: Run the focused tests to verify they pass**

Run: `npm --prefix web-ui test -- --run src/services/agentEvents.test.js src/services/agentRecovery.test.js`

Expected: PASS.

- [ ] **Step 5: Commit frontend recovery primitives**

```bash
git add web-ui/src/services/agentEvents.js web-ui/src/services/agentApi.js web-ui/src/services/agentRecovery.js web-ui/src/services/agentEvents.test.js web-ui/src/services/agentRecovery.test.js
git commit -m "feat: add agent recovery state"
```

### Task 7: End-to-end verification and rollout artifacts

**Files:**
- Modify: backend tests created in Tasks 1–5 as needed for end-to-end SSE coverage.
- Modify: `web-ui/src/services/agentRecovery.test.js`
- Modify: `docs/operations/agent-observability.md`

**Interfaces:**
- Consumes: all prior task interfaces.
- Produces: verified recovery matrix and documented observe-only rollout criteria.

- [ ] **Step 1: Write end-to-end failure-matrix tests**

Exercise model timeout before output (one retry), model timeout after partial output (continue only), truncated response, JSONL append failure, approval expiry, tool validation failure, non-idempotent tool failure, and unknown exception. Assert exactly one terminal event and correlation IDs in every failed case.

- [ ] **Step 2: Run the failure-matrix tests to verify they fail before final fixes**

Run: `./mvnw.cmd test`

Expected: FAIL only if integration gaps remain; record each gap before changing production code.

- [ ] **Step 3: Close only verified integration gaps**

Make the smallest changes required for all matrix cases to satisfy the global constraints. Update the operations document with exact initial alert thresholds, cancellation labels, dashboard groups, and the one-week observe-only calibration checklist.

- [ ] **Step 4: Run full backend and frontend verification**

Run: `./mvnw.cmd test`

Expected: BUILD SUCCESS with zero failures and zero errors.

Run: `npm --prefix web-ui test -- --run`

Expected: all frontend tests pass.

- [ ] **Step 5: Commit the verification and rollout material**

```bash
git add src/test web-ui/src/services docs/operations/agent-observability.md
git commit -m "test: verify agent error recovery matrix"
```

## Plan Self-Review

- Spec coverage: Tasks 1–4 implement the public error contract, recovery state machine, session/approval/tool failures, and idempotency boundary; Task 5 implements logs, traces, metrics, alerts, and dashboards; Task 6 implements client recovery behavior; Task 7 verifies the acceptance matrix and rollout.
- Type consistency: `AgentError`, `ErrorCode`, `RecoveryAction`, `RunContext`, `RetryPolicy`, `RunFailureHandler`, `ToolOutcome`, and `ToolSafety` are each introduced before a later task consumes them.
- Review Focus coverage: partial-output retry and terminal-once behavior are Task 2 tests; cancelled approvals are Task 3 tests; append failure is Task 3/7; duplicate write operation ID is Task 4; malformed browser payload is Task 6.
- Proportion: implementation decisions are captured as interfaces, explicit tests, and configuration values; no production bodies are transcribed.
