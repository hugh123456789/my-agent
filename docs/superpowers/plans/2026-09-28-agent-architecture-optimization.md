# Agent Architecture Optimization Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Improve cohesion and reduce coupling across model configuration, tool execution, Agent orchestration, hooks, event boundaries, and Todo behavior while preserving the `/agent/loop` and `/agent/loop/approve` contracts.

**Architecture:** Keep `AgentController -> AgentFacade -> AgentLoopExecutor` as the application path. Extract infrastructure-heavy responsibilities into focused collaborators: typed model properties, a Bash executor, a session service, a prompt builder, and Todo state/reminder components. Keep LangChain4j behind the current model integration boundary unless a small application DTO removes meaningful coupling without changing the SSE protocol.

**Tech Stack:** Java 21, Spring Boot 4.1, LangChain4j 1.3, Reactor Flux, JUnit 5, Mockito, Maven.

**Spec:** `docs/superpowers/specs/2026-09-28-agent-architecture-optimization-design.md`

## Global Constraints

- Preserve `GET /agent/loop` and `GET /agent/loop/approve` request parameters and event names.
- Keep LangChain4j as the model integration layer.
- Preserve existing tool names and main/sub-agent dispatcher separation.
- Preserve the append-only JSONL session format.
- A failed Hook must not prevent later Hooks from running, but must be logged.
- A Bash process must be terminated after its configured timeout and must not leave reader threads or child processes behind.
- Do not add new HTTP endpoints or perform unrelated frontend changes.

## Review Focus

- An unused provider or missing API key must not prevent the selected model from initializing; covered by Task 1 configuration tests.
- A Bash process that emits output and then hangs must still terminate at the configured timeout; covered by Task 2 timeout tests.
- A failed Hook must be observable while later callbacks still execute; covered by Task 5 registry tests.
- A canceled or expired approval must not leave a pending stream or complete it twice; covered by Task 3 approval/lifecycle tests.
- Existing SSE event serialization must remain compatible after event mapping; covered by Task 4 controller/event tests.

### Task 1: Typed and Selective Model Configuration

**Files:**
- Create: `src/main/java/com/agent/codeagent/common/ModelProperties.java`
- Modify: `src/main/java/com/agent/codeagent/common/ModelConfig.java`
- Modify: `src/main/resources/application.yaml`
- Test: `src/test/java/com/agent/codeagent/common/ModelConfigTest.java`

**Interfaces:**
- Consumes: provider settings from `agent.model` and `agent.models.*` configuration properties.
- Produces: named `ChatModel` / `StreamingChatModel` Beans for the selected main model and the sub-agent model role.

- [ ] **Step 1: Write failing configuration tests**
  - Assert that selecting `deepseek` creates the named DeepSeek model beans.
  - Assert that unrelated provider beans are not created.
  - Assert that required key/base URL/model settings are read from typed properties.
- [ ] **Step 2: Run `mvn '-Dtest=com.agent.codeagent.common.ModelConfigTest' test` and verify the tests fail for the current eager configuration.**
- [ ] **Step 3: Implement `@ConfigurationProperties(prefix = "agent")` in `ModelProperties` and conditionally create only selected model Beans in `ModelConfig`.**
- [ ] **Step 4: Move defaults and the selected provider into `application.yaml`; keep model Bean names consumed by `AgentLoopExecutor` and `SubAgentRunner`.**
- [ ] **Step 5: Run the focused test and verify PASS.**

### Task 2: Dedicated Bash Execution with Enforced Timeout

**Files:**
- Create: `src/main/java/com/agent/codeagent/tools/BashTool.java`
- Modify: `src/main/java/com/agent/codeagent/common/ToolConfig.java`
- Modify: `src/test/java/com/agent/codeagent/common/ToolConfigTest.java`
- Create: `src/test/java/com/agent/codeagent/tools/BashToolTest.java`

**Interfaces:**
- Consumes: a JSON or raw command argument and an injected platform process runner/configuration.
- Produces: `String execute(String arguments)` with the current Bash tool output/error contract.

- [ ] **Step 1: Write failing tests for JSON argument parsing, raw command compatibility, normal output, and a hanging process.**
- [ ] **Step 2: Run `mvn '-Dtest=com.agent.codeagent.tools.BashToolTest' test` and verify the timeout test fails or the class is missing.**
- [ ] **Step 3: Implement `BashTool` with separate argument parsing, asynchronous stdout/stderr capture, a 30-second timeout, forced process termination, and reader cleanup.**
- [ ] **Step 4: Change `ToolConfig.registerBash` to delegate to `BashTool`; keep the `bash` tool schema and name unchanged.**
- [ ] **Step 5: Run Bash and ToolConfig tests and verify PASS.**

### Task 3: Session and Agent Loop Boundary Extraction

**Files:**
- Create: `src/main/java/com/agent/codeagent/agents/core/SessionService.java`
- Create: `src/main/java/com/agent/codeagent/agents/core/PromptBuilder.java`
- Modify: `src/main/java/com/agent/codeagent/agents/core/AgentLoopExecutor.java`
- Modify: `src/test/java/com/agent/codeagent/agents/core/AgentLoopExecutorTest.java`
- Create: `src/test/java/com/agent/codeagent/agents/core/SessionServiceTest.java`
- Create: `src/test/java/com/agent/codeagent/agents/core/PromptBuilderTest.java`

**Interfaces:**
- `SessionService.open(String sessionId): ConversationContext`
- `PromptBuilder.build(String skillsPrompt, String reminder): String`
- `AgentLoopExecutor` consumes both collaborators and remains responsible for round/tool/approval orchestration.

- [ ] **Step 1: Write failing tests for generated/preserved session IDs, JSONL-backed context loading, and prompt/reminder composition.**
- [ ] **Step 2: Run the focused tests and verify failure before adding the collaborators.**
- [ ] **Step 3: Implement `SessionService` around the existing `JsonlConversationStore` and move session ID resolution into it.**
- [ ] **Step 4: Implement `PromptBuilder` and replace the static prompt composition path in `AgentLoopExecutor`.**
- [ ] **Step 5: Inject both collaborators into `AgentLoopExecutor` and remove its direct session/prompt construction responsibilities.**
- [ ] **Step 6: Run Agent loop, session, prompt, approval, and sub-agent tests and verify PASS.**

### Task 4: Application Event Boundary

**Files:**
- Create: `src/main/java/com/agent/codeagent/agents/core/CompletionEvent.java` if needed by the existing response mapping.
- Modify: `src/main/java/com/agent/codeagent/agents/core/AgentEvent.java`
- Modify: `src/main/java/com/agent/codeagent/agents/core/ModelStreamAdapter.java`
- Modify: `src/test/java/com/agent/codeagent/agents/core/AgentEventSerializationTest.java`
- Modify: `src/test/java/com/agent/codeagent/agents/core/ModelStreamAdapterTest.java`

**Interfaces:**
- Keep the serialized `type` values and existing SSE fields stable.
- Map LangChain4j response objects to application-owned event data at the adapter boundary where this reduces coupling without changing clients.

- [ ] **Step 1: Add serialization tests that pin all current event type names and completion payload fields.**
- [ ] **Step 2: Run the event tests and verify the new boundary assertions fail if the mapping is absent.**
- [ ] **Step 3: Introduce only the smallest application-owned DTO needed to remove direct LangChain4j response exposure.**
- [ ] **Step 4: Update `ModelStreamAdapter` and event serialization while preserving the public SSE contract.**
- [ ] **Step 5: Run event, controller, and loop tests and verify PASS.**

### Task 5: Hook Observability and Todo Responsibility Split

**Files:**
- Modify: `src/main/java/com/agent/codeagent/agents/hook/core/HookRegistry.java`
- Create: `src/main/java/com/agent/codeagent/tools/TodoStore.java`
- Create: `src/main/java/com/agent/codeagent/tools/TodoReminderHook.java`
- Modify: `src/main/java/com/agent/codeagent/tools/TodoTool.java`
- Modify: `src/test/java/com/agent/codeagent/agents/hook/core/HookRegistryTest.java`
- Modify: `src/test/java/com/agent/codeagent/tools/TodoToolTest.java`
- Create: `src/test/java/com/agent/codeagent/tools/TodoReminderHookTest.java`

**Interfaces:**
- `HookRegistry` keeps callback ordering and failure isolation, and logs event/registrar failures.
- `TodoTool` keeps the `todo_write` tool contract.
- `TodoStore` owns task state and formatting; `TodoReminderHook` owns round/loop reminder behavior.

- [ ] **Step 1: Write failing tests for logged Hook failure followed by later callback execution, and for Todo state/reminder behavior.**
- [ ] **Step 2: Run the focused tests and verify failure.**
- [ ] **Step 3: Add structured warning logging to `HookRegistry` without changing callback output propagation.**
- [ ] **Step 4: Extract Todo state/formatting and reminder lifecycle into focused collaborators while preserving output text and statuses.**
- [ ] **Step 5: Run Hook and Todo tests and verify PASS.**

### Task 6: Final Integration and Verification

**Files:**
- Modify only files required by preceding tasks.
- Test: existing Maven test suite and package build.

- [ ] **Step 1: Run `mvn test`.**
- [ ] **Step 2: If the Spring context test fails, distinguish model/configuration failures from environment-only loopback failures and fix code-level failures.**
- [ ] **Step 3: Run `mvn -DskipTests package`.**
- [ ] **Step 4: Verify only `/agent/loop` and `/agent/loop/approve` remain in `AgentController`, and no old direct streaming endpoint references remain.**
- [ ] **Step 5: Review the final diff for unrelated changes and preserve existing user work.**
