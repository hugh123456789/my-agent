# Agent Architecture Optimization Design

## Goal

Improve cohesion and reduce coupling across the Agent application while preserving the current public entry points:

- `GET /agent/loop`
- `GET /agent/loop/approve`

The existing Agent loop behavior, tool permission flow, session persistence, and SSE event contract should remain compatible unless a change is required to fix a verified defect.

## Scope

### 1. Model configuration

- Replace scattered environment-field injection in `ModelConfig` with typed configuration properties.
- Make model activation explicit through configuration.
- Avoid constructing unused provider clients during application startup and tests.
- Keep LangChain4j as the model integration layer.
- Preserve the current named model roles used by the main Agent loop and sub-agent runner.

### 2. Tool composition and execution

- Keep `ToolConfig` focused on assembling tool dispatchers and specifications.
- Extract Bash process execution, argument parsing, output collection, and timeout handling into a dedicated component.
- Ensure timeout enforcement does not depend on a blocking read of the child process output stream.
- Preserve the existing tool names and dispatcher separation between the main Agent and sub-agents.

### 3. Agent loop orchestration

- Keep `AgentLoopExecutor` as the top-level loop coordinator.
- Extract session/context creation and persistence behind a focused service.
- Extract system-prompt and reminder composition behind a focused builder.
- Keep approval state management and tool processing as independent collaborators.
- Keep the controller dependent on the facade rather than on LangChain4j or low-level loop details.

### 4. Hook observability

- Preserve per-hook failure isolation.
- Log hook failures with the event and registrar context instead of silently discarding exceptions.
- Keep hook registration and event semantics compatible.

### 5. Event boundary

- Introduce application-level completion/event data where practical.
- Avoid expanding the public SSE contract unnecessarily.
- Do not perform a speculative rewrite of every LangChain4j response type; migrate only the types that create meaningful boundary coupling.

### 6. Todo responsibilities

- Separate Todo state/formatting from Agent hook reminder behavior where it can be done without changing tool output or reminder text.
- Preserve the `todo_write` tool contract and existing status values.

## Non-goals

- No new HTTP endpoints.
- No change to the `/agent/loop` or `/agent/loop/approve` request parameters or event names unless tests prove the current contract is invalid.
- No replacement of LangChain4j.
- No unrelated frontend redesign or persistence format rewrite.
- No broad refactor of all domain classes solely for stylistic consistency.

## Proposed dependency direction

```text
HTTP Controller
    -> AgentFacade
        -> AgentLoopExecutor
            -> SessionService
            -> PromptBuilder
            -> ToolCallProcessor
            -> ApprovalManager
            -> LangChain4j model port/adapter

ToolConfig -> ToolDispatcher -> Tool implementations
HookRegistry -> Hook registrars
ModelProperties -> enabled LangChain4j model beans
```

Infrastructure details such as Spring, LangChain4j, filesystem persistence, and process execution should remain behind focused collaborators where extraction materially improves testing or replacement.

## Error and lifecycle rules

- A failed Hook must not prevent later Hooks from running, but must be logged.
- A Bash process must be terminated after its configured timeout and must not leave reader threads or child processes behind.
- Approval expiration must complete the suspended stream exactly once.
- Session writes must preserve the existing append-only JSONL format.
- Model initialization failures should identify the selected provider and configuration problem rather than failing because an unrelated provider is unused.

## Verification

- Unit tests for model activation/configuration decisions.
- Unit tests for Bash argument parsing, output capture, timeout, and process cleanup.
- Existing loop, tool, approval, hook, session, and sub-agent tests remain green.
- Tests verify constructor injection and the reduced dependency graph.
- A compile/package verification is required.
- Full test results must distinguish code failures from environment-only failures.
