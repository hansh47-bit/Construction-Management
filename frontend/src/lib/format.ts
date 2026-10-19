const currency = new Intl.NumberFormat('ko-KR')

export function formatWon(value: number | undefined) {
  return `${currency.format(Math.round(value ?? 0))} 원`
}

export function formatDate(value: string | undefined) {
  return value || '-'
}

export function formatDateTime(value: string | undefined) {
  if (!value) return '-'
  const [date, time] = value.replace('T', ' ').split(' ')
  return time ? `${formatDate(date)} ${time.slice(0, 5)}` : formatDate(date)
}

export function formatNumber(value: number) {
  return currency.format(Math.round(value || 0))
}

export function toNumber(value: string) {
  return Number(value.replaceAll(',', '') || 0)
}

export function varianceClass(value: number) {
  if (value > 0) return 'text-rose-700'
  if (value < 0) return 'text-blue-700'
  return 'text-slate-900'
}

const workflowStatusLabels: Record<string, string> = {
  DRAFT: '초안',
  PENDING: '대기',
  REQUESTED: '결재 대기',
  APPROVED: '승인',
  REJECTED: '반려',
  CANCELLED: '취소',
  COMPLETED: '완료',
  READY: '지급요청 가능',
  PAID: '지급 완료',
  WAITING_FOR_PAYMENT: '지급 대기',
  PAYMENT_APPROVAL_REQUESTED: '지급결재 대기',
}

export function formatWorkflowStatus(value: string | undefined) {
  if (!value) return '-'
  return workflowStatusLabels[value] ?? '상태 확인 필요'
}
