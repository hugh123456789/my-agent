import { describe, it } from 'node:test'
import assert from 'node:assert/strict'
import { articles } from './articleData.js'

describe('article cards', () => {
  it('provides a local image for every article', () => {
    assert.equal(articles.length, 3)
    for (const article of articles) {
      assert.match(article.image, /^\/article-images\/.+\.svg$/)
    }
  })
})
