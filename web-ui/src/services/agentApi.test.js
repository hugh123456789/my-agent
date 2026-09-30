import { describe, it } from 'node:test'
import assert from 'node:assert/strict'
import { consumeSse, parseSseFrame } from './agentApi.js'

describe('parseSseFrame', () => {
  it('parses JSON event data', () => {
    assert.deepEqual(parseSseFrame('event: text\ndata: {"text":"hello"}'), {
      event: 'text',
      data: { text: 'hello' },
    })
  })

  it('keeps plain text event data readable', () => {
    assert.deepEqual(parseSseFrame('event: thinking\ndata: still working'), {
      event: 'thinking',
      data: 'still working',
    })
  })

  it('reports an aborted stream separately from a completed stream', async () => {
    const events = []
    const abort = new DOMException('The operation was aborted.', 'AbortError')
    globalThis.fetch = async () => { throw abort }

    await consumeSse('/agent/loop', {
      onComplete: () => events.push('complete'),
      onAbort: () => events.push('abort'),
      onError: () => events.push('error'),
    }, new AbortController().signal)

    assert.deepEqual(events, ['abort'])
  })

  it('reports stream failures without pretending they completed', async () => {
    const events = []
    globalThis.fetch = async () => { throw new Error('network down') }

    await consumeSse('/agent/loop', {
      onComplete: () => events.push('complete'),
      onAbort: () => events.push('abort'),
      onError: (error) => events.push(error.message),
    }, new AbortController().signal)

    assert.deepEqual(events, ['network down'])
  })
})
