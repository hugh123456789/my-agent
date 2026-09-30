const API_ROOT = '/agent'

function parseData(data) {
  if (!data) return null
  try {
    return JSON.parse(data)
  } catch {
    return data
  }
}

export function parseSseFrame(frame) {
  const lines = frame.split(/\r?\n/)
  let event = 'message'
  const data = []

  for (const line of lines) {
    if (line.startsWith('event:')) event = line.slice(6).trim()
    if (line.startsWith('data:')) data.push(line.slice(5).trimStart())
  }

  if (!data.length) return null
  return { event, data: parseData(data.join('\n')) }
}

export async function consumeSse(url, { onEvent, onError, onAbort, onComplete } = {}, signal) {
  try {
    const response = await fetch(url, {
      headers: { Accept: 'text/event-stream' },
      signal,
    })

    if (!response.ok) throw new Error(`Agent request failed (${response.status})`)
    if (!response.body) throw new Error('Agent stream is unavailable')

    const reader = response.body.getReader()
    const decoder = new TextDecoder()
    let buffer = ''

    while (true) {
      const { value, done } = await reader.read()
      buffer += decoder.decode(value || new Uint8Array(), { stream: !done })

      const frames = buffer.split(/\r?\n\r?\n/)
      buffer = frames.pop() || ''
      for (const frame of frames) {
        const parsed = parseSseFrame(frame)
        if (parsed) onEvent?.(parsed)
      }

      if (done) break
    }

    const lastFrame = parseSseFrame(buffer)
    if (lastFrame) onEvent?.(lastFrame)
    onComplete?.({ reason: 'eof' })
  } catch (error) {
    if (error.name === 'AbortError') {
      onAbort?.()
      return
    }
    onError?.(error)
  }
}

function streamRequest(path, params, callbacks) {
  const controller = new AbortController()
  const query = new URLSearchParams(
    Object.entries(params).filter(([, value]) => value !== undefined && value !== null && value !== ''),
  )
  consumeSse(`${API_ROOT}${path}?${query}`, callbacks, controller.signal)
  return { abort: () => controller.abort() }
}

export function createAgentStream({ sessionId, message, ...callbacks }) {
  return streamRequest('/loop', { sessionId, message }, callbacks)
}

export function approveAgentTool({ requestId, approved, ...callbacks }) {
  return streamRequest('/loop/approve', { requestId, approved }, callbacks)
}
