# Agent Core Responsibility Refactor Design

## Goal

Clarify the responsibilities in `agents/core` while preserving the existing
facade methods, controller endpoints, and `AgentEvent` event names.

## Current Problem

`Agent` currently owns model streaming, ReAct loop control, tool execution,
permission decisions, approval suspension, pending-state storage, Flux
lifecycle handling, and conversation-message mutation. These concerns change
for different reasons and make testing and future persistence difficult.

## Design

Keep `AgentFacade` as the public application boundary. Move the internal
responsibilities into focused services:

- `AgentLoopExecutor`: owns the ReAct loop, round/tool limits, and transitions
  between model responses and tool processing.
- `ModelStreamAdapter`: converts LangChain4j streaming callbacks into
  `AgentEvent` values and handles sink cancellation checks.
- `ToolCallProcessor`: owns permission hooks, tool dispatch, post-tool hooks,
  and tool-result event creation.
- `ApprovalManager`: owns pending approval state, request expiry, approval
  resume, and cleanup.
- `ConversationContext`: owns durable conversation messages and creates a
  request-local copy when a temporary reminder is injected.

`Agent` remains a compatibility-oriented coordinator that delegates to these
services, so existing callers do not change during the refactor.

## Compatibility

- Keep `AgentFacade` public methods unchanged.
- Keep `AgentEvent` type names unchanged.
- Preserve Hook event order and existing permission behavior.
- Preserve the current maximum loop/tool limits and five-minute approval TTL.
- Todo reminders must not be appended to durable conversation history.

## Testing

Add focused unit tests for conversation-copy behavior, approval expiry and
cleanup, tool-processing outcomes, and loop termination. Run all non-network
tests and compile verification after each extraction.
