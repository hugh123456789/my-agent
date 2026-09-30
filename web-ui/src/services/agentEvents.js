const KNOWN_EVENT_TYPES = new Set([
  'session',
  'text',
  'thinking',
  'tool_call',
  'tool_result',
  'permission_required',
  'permission_denied',
  'reminder',
  'complete',
  'error',
])

export function normalizeAgentPayload(data) {
  if (typeof data !== 'string') return data && typeof data === 'object' ? data : {}
  try {
    return JSON.parse(data)
  } catch {
    return { text: data }
  }
}

export function resolveAgentEventType(event, payload) {
  const declaredType = payload?.type
  if (KNOWN_EVENT_TYPES.has(declaredType)) return declaredType
  if (KNOWN_EVENT_TYPES.has(event)) return event
  if (payload?.sessionId || payload?.session_id) return 'session'
  if (payload?.requestId) return 'permission_required'
  if (payload?.toolName && Object.hasOwn(payload, 'content')) return 'tool_result'
  if (payload?.name && (Object.hasOwn(payload, 'arguments') || Object.hasOwn(payload, 'partialArguments'))) return 'tool_call'
  if (payload?.toolName && payload?.reason) return 'permission_denied'
  if (payload?.error || payload?.message) return 'error'
  if (payload?.response) return 'complete'
  return 'unknown'
}

export function serializeActivityDetail(value) {
  if (typeof value === 'string') return value
  if (value === undefined || value === null) return ''
  try {
    return JSON.stringify(value)
  } catch {
    return String(value)
  }
}

export function formatAgentError(payload, fallback = 'The agent stream ended unexpectedly.') {
  const value = payload?.message || payload?.error || payload?.text || payload
  if (value && typeof value === 'object') return value.message || serializeActivityDetail(value)
  return value ? String(value) : fallback
}
