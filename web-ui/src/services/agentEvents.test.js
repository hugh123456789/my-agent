import { describe, it } from 'node:test'
import assert from 'node:assert/strict'
import {
  formatAgentError,
  normalizeAgentPayload,
  resolveAgentEventType,
  serializeActivityDetail,
} from './agentEvents.js'

describe('agent event normalization', () => {
  it('uses the wire type when it is present', () => {
    assert.equal(resolveAgentEventType('message', { type: 'thinking', text: 'step' }), 'thinking')
  })

  it('keeps unknown structured events inspectable', () => {
    const payload = { type: 'future_event', value: 42 }
    assert.equal(resolveAgentEventType('message', payload), 'unknown')
    assert.equal(serializeActivityDetail(payload), '{"type":"future_event","value":42}')
  })

  it('normalizes plain text without turning it into an object twice', () => {
    assert.deepEqual(normalizeAgentPayload('still working'), { text: 'still working' })
  })

  it('extracts nested backend error messages', () => {
    assert.equal(formatAgentError({ error: { message: 'tool failed' } }), 'tool failed')
  })
})
