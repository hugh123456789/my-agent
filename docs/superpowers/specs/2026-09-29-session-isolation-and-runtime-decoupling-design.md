# Session Isolation and Runtime Decoupling Design

## Goal

Remove cross-session Todo leakage and make conversation persistence a true runtime dependency inversion, without changing the public HTTP or CLI Agent interfaces.

## Scope

This design changes backend code only. The Vue application and its API helper files are explicitly out of scope.

## Architecture

`TodoStore` becomes a session-aware state repository. Callers access a `TodoSession` using the current `sessionId`; the session owns its tasks, sequence counter, and reminder counter. `TodoTool` receives the session identity from the tool-processing path, while sub-agents use an isolated ephemeral Todo session. The existing hook registry remains global and stateless with respect to Todo data.

`ConversationRepository` is supplied as a Spring bean. `SessionService` accepts only that interface and creates `ConversationContext` instances with it; `JsonlConversationStore` stays the default implementation but is instantiated by configuration, not by `SessionService`.

`PromptBuilder` is removed and its one-use prompt composition becomes a private method of `AgentLoopExecutor`. The existing `ApprovalManager` expiry callback continues to be wired by configuration; changing its lifecycle is out of scope because it is not necessary to remove the observed session leakage.

## Data Flow

1. `AgentLoopExecutor` opens a `ConversationContext` and starts an Agent loop with its session ID.
2. Each tool invocation passes that ID to `ToolCallProcessor`.
3. `TodoTool` reads or updates only the associated Todo session.
4. `TodoHookRegistrar` uses the same ID to record calls and emit reminders for that session only.
5. A child Agent loop is given a unique ephemeral Todo session ID, so it cannot mutate the parent or another child’s tasks.

## Error Handling

Unknown or blank session IDs retain current behavior: the session service creates a new UUID. Todo validation errors remain tool-result strings. Todo state is in-memory and is discarded when the process stops; persistence is not added in this change.

## Testing

Tests must demonstrate that two distinct sessions do not share Todo tasks or reminder counters, that a child session does not observe the parent’s Todo state, and that `SessionService` uses an injected `ConversationRepository`. Existing backend tests must remain green.
