export type FeedbackKind = 'success' | 'error' | 'info'
export type Feedback = { id: number; kind: FeedbackKind; message: string; dialogTitle?: string }
export type Confirmation = { title: string; message: string; confirmLabel?: string; destructive?: boolean }
export const FEEDBACK_EVENT = 'inc:feedback'
export const PENDING_EVENT = 'inc:pending'
export const DATA_CHANGED_EVENT = 'inc:data-changed'
export const SESSION_RESET_EVENT = 'inc:session-reset'

let nextId = 0
let confirmationHandler: ((options: Confirmation) => Promise<boolean>) | null = null
const pending = new Map<number, string>()
const drafts = new Set<string>()
const runningActions = new Map<string, { id: number; promise: Promise<unknown> }>()

export function notify(message: string, kind: FeedbackKind = 'success', dialogTitle?: string) {
  window.dispatchEvent(new CustomEvent<Feedback>(FEEDBACK_EVENT, { detail: { id: ++nextId, kind, message, dialogTitle } }))
}
export function setConfirmationHandler(handler: typeof confirmationHandler) { confirmationHandler = handler }
export function confirmAction(options: Confirmation): Promise<boolean> {
  return confirmationHandler ? confirmationHandler(options) : Promise.resolve(window.confirm(`${options.title}\n\n${options.message}`))
}
export function beginPending(label: string) {
  const id = ++nextId
  pending.set(id, label)
  window.dispatchEvent(new Event(PENDING_EVENT))
  return id
}
export function endPending(id: number) { pending.delete(id); window.dispatchEvent(new Event(PENDING_EVENT)) }
export function pendingLabels() { return [...pending.values()] }
export function hasPendingMutation() { return pending.size > 0 }
export function announceDataChanged(path: string) {
  const projectId = path.match(/^\/api\/v1\/projects\/([^/]+)/)?.[1]
  window.dispatchEvent(new CustomEvent(DATA_CHANGED_EVENT, { detail: { path, projectId } }))
}
export function setDraftDirty(key: string, dirty: boolean) { if (dirty) drafts.add(key); else drafts.delete(key) }
export function hasUnsavedDraft() { return drafts.size > 0 }
export function clearDrafts() { drafts.clear() }
export function resetFeedbackSession() {
  drafts.clear()
  pending.clear()
  runningActions.clear()
  window.dispatchEvent(new Event(PENDING_EVENT))
}
export function userAction<Args extends unknown[], Result>(label: string, operation: (...args: Args) => Promise<Result>, fallback: Result) {
  return async (...args: Args): Promise<Result> => {
    const running = runningActions.get(label)
    if (running) return running.promise as Promise<Result>
    const id = beginPending(`${label} 처리 중…`)
    const task = (async () => {
      try { return await operation(...args) }
      catch (error) {
        if (!(error instanceof DOMException && error.name === 'AbortError')) notify(`${label} 처리 결과를 확인하지 못했습니다.\n연결 상태와 최신 화면을 확인해주세요.`, 'error')
        return fallback
      } finally { endPending(id); if (runningActions.get(label)?.id === id) runningActions.delete(label) }
    })()
    runningActions.set(label, { id, promise: task })
    return task
  }
}

export function mutationFeedback(path: string, method: string): { pending: string; success: string; dialogTitle?: string } | null {
  if (['GET', 'HEAD', 'OPTIONS'].includes(method) || !path.startsWith('/api/v1/') || path.startsWith('/api/v1/auth/')) return null
  const deleted = method === 'DELETE'
  let noun = '내용'
  let verb = deleted ? '삭제' : '저장'
  if (path.startsWith('/api/v1/approvals/')) {
    noun = '결재'
    verb = path.endsWith('/approve') ? '승인' : path.endsWith('/reject') ? '반려' : '승인취소'
  } else if (/\/(request-approval|request)$/.test(path) || path.endsWith('/payment-requests')) {
    noun = path.endsWith('/payment-requests') ? '지급 결재' : '결재'
    verb = '요청'
  } else if (path.endsWith('/payments')) { noun = '실제 지급 내역'; verb = '등록' }
  else if (path.includes('/contract-files') || path.includes('/attachments') || path.includes('/photos')) { noun = '첨부파일'; verb = deleted ? '삭제' : '등록' }
  else if (path.endsWith('/executive-correction')) { noun = '대표 금액 정정'; verb = '반영' }
  else if (path.endsWith('/start-construction')) { noun = '공사 시작'; verb = '처리' }
  else if (path.endsWith('/synchronize-preparation-status')) { noun = '프로젝트 상태'; verb = '갱신' }
  else if (path.includes('/contracts') || path.endsWith('/contract')) noun = '계약'
  else if (path.includes('/execution-budget')) noun = '실행예산'
  else if (path.includes('/comparisons')) noun = '업체 비교견적'
  else if (path.includes('/progress-claims')) { noun = '기성'; verb = deleted ? '삭제' : '등록' }
  else if (path.includes('/daily-logs')) { noun = '현장 작업일지'; if (deleted) verb = '삭제·보관' }
  else if (path.includes('/meeting-minutes')) { noun = '현장 회의록'; if (deleted) verb = '삭제·보관' }
  else if (path.includes('/revisions') || path.includes('/change-orders')) noun = '변경 내용'
  else if (path.startsWith('/api/v1/admin/')) {
    noun = path.includes('/positions/') ? '직책별 권한' : '계정'
    verb = path.endsWith('/reset-password') ? '비밀번호 초기화' : path.endsWith('/unlock') ? '잠금 해제' : deleted ? '삭제' : '저장'
  } else if (path === '/api/v1/projects') { noun = '프로젝트'; verb = '등록' }
  else if (/^\/api\/v1\/projects\/[^/]+$/.test(path)) noun = '프로젝트'
  const particle = (verb.charCodeAt(verb.length - 1) - 0xac00) % 28 === 0 ? '가' : '이'
  const dialogTitle = verb === '저장' || verb === '요청' || verb === '등록' && noun !== '첨부파일' ? `${noun} ${verb} 완료` : undefined
  return { pending: `${noun} ${verb} 처리 중…`, success: `${noun} ${verb}${particle} 완료되었습니다.`, dialogTitle }
}
