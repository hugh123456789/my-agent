import { describe, it } from 'node:test'
import assert from 'node:assert/strict'
import { getDitherInk } from './dither.js'

describe('dither image processing', () => {
  it('uses the Bayer threshold for stable paper-and-ink decisions', () => {
    assert.equal(getDitherInk(1, 0, 0), true)
    assert.equal(getDitherInk(0.02, 0, 0), false)
    assert.equal(getDitherInk(0.6, 1, 0), true)
    assert.equal(getDitherInk(0.6, 0, 1), false)
  })
})
