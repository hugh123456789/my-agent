# `sub_task` Nested Agent Loop Design

## Goal

Add a `task` tool that synchronously runs a nested Agent Loop with a fresh
message list. The nested Agent returns its final text as the parent tool
result, while sharing the parent process, working directory, permissions, and
lifecycle hooks.

## Requirements

- The parent Agent exposes a `task` tool.
- Calling `task` runs a synchronous nested Agent Loop.
- The nested loop starts with a new in-memory `messages[]` containing only the
  child prompt; it does not load or append to a parent session.
- The nested Agent can use the five existing base tools: `bash`,
  `getCurrentDateTime`, `getCurrentDate`, `getCurrentTime`, and `todo_write`.
- The nested Agent cannot see or call `task`.
- The nested Agent shares the existing `ToolCallProcessor` and `HookRegistry`,
  so permission checks and tool lifecycle hooks are applied consistently.
- The nested Agent shares the current process and working directory. Tool
  effects such as file writes remain visible to the parent.
- Only the nested Agent's final text is returned to the parent as the `task`
  tool result.
- Child messages are not persisted. The parent conversation persists the
  `task` call and its returned result through the existing conversation flow.

## Design

`SubTaskTool` is a normal registered tool in the main dispatcher. It delegates
to `SubAgentRunner`, which owns a synchronous child loop. The runner receives
the child prompt, child tool dispatcher, shared model, shared hook registry,
and shared tool-call processor. Each model response is appended only to a
local `ConversationContext` without a session store.

The child loop uses the synchronous `ChatModel` API. On a tool-call response it
executes each request through the shared `ToolCallProcessor`, appends the
result to the local messages, and requests the next round. A non-tool response
returns its text. The same round and tool-call limits as the main loop prevent
unbounded execution.

The child tool dispatcher is an independent dispatcher populated with the five
base tools. The `task` registration is deliberately confined to the main
dispatcher, preventing recursive nested task calls.

Because the parent `task` invocation is synchronous, a child permission result
of `AskUser` cannot suspend for an external approval request. The runner
returns a descriptive error result for that child task; existing parent-level
approval behavior remains unchanged.

## Error handling

- Model failures become a descriptive tool result and do not corrupt the
  parent conversation.
- Unknown or failed child tools use the existing tool processor result format.
- Child round/tool limits return a descriptive error result.
- Child messages are discarded after the tool returns.

## Testing

Add focused tests for:

1. Main dispatcher contains `task`; child dispatcher contains the five base
   tools and excludes `task`.
2. A child loop can execute a tool and then return a final answer.
3. Child messages are independent from and do not persist into the parent
   session.
4. Child tool execution goes through permission and post-tool hooks.
5. Child limits and model/tool errors become tool results.

