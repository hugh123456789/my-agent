import { describe, it } from 'node:test'
import assert from 'node:assert/strict'
import { nextTheme, validateEmail } from './homeState.js'

describe('home page state helpers', () => {
  it('cycles between light and dark themes', () => {
    assert.equal(nextTheme('light'), 'dark')
    assert.equal(nextTheme('dark'), 'light')
  })

  it('accepts a valid newsletter email and rejects invalid input', () => {
    assert.equal(validateEmail('hello@example.com'), true)
    assert.equal(validateEmail('not-an-email'), false)
    assert.equal(validateEmail(''), false)
  })
})
