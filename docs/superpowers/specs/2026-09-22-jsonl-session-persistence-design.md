# JSONL Session Persistence Design

## Goal

Persist agent conversations as one append-only `.jsonl` file per session so a
conversation can be resumed without rewriting its full history.

## Decisions

- The client may provide `sessionId` to `/agent/loop` and
  `/agent/loop/approve`.
- If `/agent/loop` receives no `sessionId`, the service creates a new opaque
  session id and returns it in the event stream before other agent events.
- A session is stored as `${agent.sessions.directory}/${sessionId}.jsonl`.
- Each line is one normalized, self-contained message record. Records contain
  the message kind and only fields needed to reconstruct the LangChain4j
  message; arbitrary Java polymorphic serialization is not used.
- Adding a message serializes one record and appends it with UTF-8 and a
  trailing newline. The store serializes writes per session so concurrent
  requests cannot interleave records.
- Loading reads valid complete lines in order. An incomplete final line is
  ignored so a process interruption cannot make prior records unrecoverable.
- Session ids are validated as a single safe filename component; path
  traversal and separators are rejected.
- Existing in-memory request snapshots remain unchanged: temporary reminders
  are never persisted.

## Components

- `JsonlConversationStore`: file-backed load and append operations.
- `ConversationMessageCodec`: conversion between normalized JSON records and
  LangChain4j `ChatMessage` instances.
- `ConversationContext`: owns loaded messages and delegates durable additions
  to the store.
- `AgentLoopExecutor` / `AgentController`: create or resume contexts and
  propagate the session id through approval continuation.

## Error handling

Invalid session ids fail the request with a client-visible error. Missing
session files represent an empty new session. Malformed complete records fail
loading rather than silently changing the conversation; an incomplete final
line is ignored.

## Testing

Tests cover empty/new sessions, append-only file growth, reload order,
message-kind round trips, invalid ids, incomplete tails, and the no-id/new-id
HTTP flow. The full Maven test suite remains the final verification.
