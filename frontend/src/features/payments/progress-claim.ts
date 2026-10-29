import type { PurchaseOrderPaymentStatus } from '../../types/domain'

export function claimEntryDefaults(purchaseOrder?: PurchaseOrderPaymentStatus) {
  const claims = purchaseOrder?.claims ?? []
  const lastDegree = claims.reduce((last, claim) => Math.max(last, claim.degree), 0)
  const committedAmount = claims
    .filter(claim => claim.approval_status === 'REQUESTED' || claim.approval_status === 'APPROVED')
    .reduce((total, claim) => total + claim.claim_amount, 0)
  return {
    degree: String(lastDegree + 1),
    claim_amount: String(Math.max(0, (purchaseOrder?.current_po_amount ?? 0) - committedAmount)),
  }
}
