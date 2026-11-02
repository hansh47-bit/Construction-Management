import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import test from 'node:test'

const html = readFileSync(new URL('../index.html', import.meta.url), 'utf8')
const documentTag = html.match(/<html\b[^>]*>/i)?.[0] ?? ''

test('the Korean application declares Korean and UTF-8', () => {
  assert.match(documentTag, /\blang=["']ko["']/i)
  assert.match(html, /<meta\b[^>]*\bcharset=["']UTF-8["']/i)
  assert.ok(html.includes('<title>INC 공사 통합관리</title>'))
})

test('automatic translation is disabled at the document root for every account', () => {
  assert.match(documentTag, /\btranslate=["']no["']/i)
  assert.match(documentTag, /\bclass=["'][^"']*\bnotranslate\b[^"']*["']/i)
})

test('Chromium translation is also disabled through its page-level directive', () => {
  assert.match(html, /<meta\b[^>]*\bname=["']google["'][^>]*\bcontent=["']notranslate["'][^>]*>/i)
})
