import assert from 'node:assert/strict'
import { test } from 'node:test'
import { claimEntryDefaults } from '../src/features/payments/progress-claim.ts'

const claim = (degree, claim_amount, approval_status = 'APPROVED') => ({ degree, claim_amount, approval_status })
const order = (claims = [], current_po_amount = 10000000) => ({ current_po_amount, claims })

test('a new purchase order starts at the first degree with its full remaining amount', () => {
  assert.deepEqual(claimEntryDefaults(order()), { degree: '1', claim_amount: '10000000' })
  assert.deepEqual(claimEntryDefaults(), { degree: '1', claim_amount: '0' })
})

test('approved and requested claims are both deducted before proposing the next claim', () => {
  assert.deepEqual(claimEntryDefaults(order([claim(1, 4000000)])), { degree: '2', claim_amount: '6000000' })
  assert.deepEqual(claimEntryDefaults(order([claim(1, 4000000), claim(2, 2000000, 'REQUESTED')])), { degree: '3', claim_amount: '4000000' })
})

test('the next degree follows the maximum historical degree rather than the claim count', () => {
  assert.deepEqual(claimEntryDefaults(order([claim(7, 2000000, 'REJECTED'), claim(1, 4000000), claim(3, 1000000, 'CANCELLED')])), { degree: '8', claim_amount: '6000000' })
})

test('exhausted and legacy over-requested orders never propose a negative amount', () => {
  assert.deepEqual(claimEntryDefaults(order([claim(1, 10000000)])), { degree: '2', claim_amount: '0' })
  assert.deepEqual(claimEntryDefaults(order([claim(1, 8000000), claim(2, 4000000, 'REQUESTED')])), { degree: '3', claim_amount: '0' })
})

test('purchase orders have independent degrees and use the current changed purchase amount', () => {
  const first = order([claim(1, 4000000)])
  const second = order([claim(1, 1000000), claim(2, 2000000)], 12000000)
  const before = structuredClone([first, second])
  assert.deepEqual(claimEntryDefaults(first), { degree: '2', claim_amount: '6000000' })
  assert.deepEqual(claimEntryDefaults(second), { degree: '3', claim_amount: '9000000' })
  assert.deepEqual([first, second], before)
})

test('actual payments do not turn an already claimed amount into new claim capacity', () => {
  const first = { ...order([claim(1, 4000000)]), actual_paid_total: 2000000, total_unpaid_balance: 8000000 }
  const paid = { ...first, actual_paid_total: 4000000, total_unpaid_balance: 6000000 }
  assert.deepEqual(claimEntryDefaults(first), { degree: '2', claim_amount: '6000000' })
  assert.deepEqual(claimEntryDefaults(paid), claimEntryDefaults(first))
})
