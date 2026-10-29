import { useEffect, useRef, useState } from 'react'
import type { ReactNode } from 'react'
import { useAuth } from '../../features/auth/auth'
import { clearDrafts, FEEDBACK_EVENT, hasUnsavedDraft, PENDING_EVENT, pendingLabels, SESSION_RESET_EVENT, setConfirmationHandler } from '../../lib/feedback'
import type { Confirmation, Feedback } from '../../lib/feedback'

export function FeedbackProvider({ children }: { children: ReactNode }) {
  const { user } = useAuth()
  return <FeedbackSession key={user?.id ?? 'signed-out'}>{children}</FeedbackSession>
}

function FeedbackSession({ children }: { children: ReactNode }) {
  const [messages, setMessages] = useState<Feedback[]>([])
  const [completions, setCompletions] = useState<Feedback[]>([])
  const [pending, setPending] = useState<string[]>([])
  const [confirmation, setConfirmation] = useState<Confirmation | null>(null)
  const resolveConfirmation = useRef<((value: boolean) => void) | null>(null)
  const dialog = useRef<HTMLDialogElement>(null)
  const timers = useRef(new Set<ReturnType<typeof setTimeout>>())
  const completion = !confirmation && pending.length === 0 ? completions[0] : undefined

  function finishConfirmation(result: boolean) {
    resolveConfirmation.current?.(result)
    resolveConfirmation.current = null
    setConfirmation(null)
  }

  useEffect(() => {
    const activeTimers = timers.current
    function receive(event: Event) {
      const message = (event as CustomEvent<Feedback>).detail
      if (message.kind === 'success' && message.dialogTitle) {
        setCompletions(current => [...current, message])
        return
      }
      setMessages(current => [...current.slice(-3), message])
      if (message.kind !== 'error') {
        const timer = setTimeout(() => { setMessages(current => current.filter(item => item.id !== message.id)); timers.current.delete(timer) }, 7000)
        timers.current.add(timer)
      }
    }
    const updatePending = () => setPending(pendingLabels())
    const reset = () => { finishConfirmation(false); setMessages([]); setCompletions([]); clearDrafts() }
    const beforeUnload = (event: BeforeUnloadEvent) => {
      if (hasUnsavedDraft() || pendingLabels().length) { event.preventDefault(); event.returnValue = '' }
    }
    setConfirmationHandler(options => {
      if (resolveConfirmation.current) return Promise.resolve(false)
      return new Promise<boolean>(resolve => { resolveConfirmation.current = resolve; setConfirmation(options) })
    })
    window.addEventListener(FEEDBACK_EVENT, receive)
    window.addEventListener(PENDING_EVENT, updatePending)
    window.addEventListener(SESSION_RESET_EVENT, reset)
    window.addEventListener('beforeunload', beforeUnload)
    return () => {
      setConfirmationHandler(null)
      resolveConfirmation.current?.(false)
      window.removeEventListener(FEEDBACK_EVENT, receive)
      window.removeEventListener(PENDING_EVENT, updatePending)
      window.removeEventListener(SESSION_RESET_EVENT, reset)
      window.removeEventListener('beforeunload', beforeUnload)
      for (const timer of activeTimers) clearTimeout(timer)
      clearDrafts()
    }
  }, [])

  useEffect(() => {
    if ((confirmation || completion) && dialog.current && !dialog.current.open) dialog.current.showModal()
  }, [confirmation, completion])

  return <>
    <fieldset disabled={pending.length > 0 || !!confirmation || !!completion} aria-busy={pending.length > 0} className="m-0 min-w-0 border-0 p-0">{children}</fieldset>
    <div className="pointer-events-none fixed right-4 top-4 z-[100] flex w-[min(430px,calc(100vw-2rem))] flex-col gap-2" aria-live="polite" aria-atomic="false">
      {messages.map(message => <div key={message.id} role={message.kind === 'error' ? 'alert' : 'status'} className={`pointer-events-auto flex items-start gap-3 rounded-xl border p-4 shadow-lg ${message.kind === 'error' ? 'border-rose-300 bg-rose-50 text-rose-950' : message.kind === 'info' ? 'border-blue-300 bg-blue-50 text-blue-950' : 'border-emerald-300 bg-emerald-50 text-emerald-950'}`}>
        <p className="min-w-0 flex-1 whitespace-pre-wrap text-sm font-semibold">{message.message}</p>
        <button type="button" aria-label="알림 닫기" className="shrink-0 rounded px-1" onClick={() => setMessages(current => current.filter(item => item.id !== message.id))}>✕</button>
      </div>)}
    </div>
    {pending.length > 0 && !confirmation && <div role="status" className="fixed bottom-5 left-1/2 z-[90] -translate-x-1/2 rounded-full bg-slate-950 px-6 py-3 text-sm font-bold text-white shadow-xl"><span className="mr-2 inline-block animate-spin" aria-hidden="true">◌</span>{pending[0]} 잠시 기다려주세요.</div>}
    {(confirmation || completion) && <dialog ref={dialog} aria-labelledby="action-confirm-title" aria-describedby="action-confirm-description" className="m-auto w-[min(480px,calc(100vw-2rem))] rounded-2xl border-0 bg-white p-6 text-slate-950 shadow-2xl backdrop:bg-slate-950/50" onCancel={event => { event.preventDefault(); if (confirmation) finishConfirmation(false); else setCompletions(current => current.slice(1)) }}>
      <h2 id="action-confirm-title" className="text-xl font-bold">{confirmation?.title ?? completion?.dialogTitle}</h2>
      <p id="action-confirm-description" className="mt-4 whitespace-pre-wrap text-sm leading-7 text-slate-600">{confirmation?.message ?? completion?.message}</p>
      <div className="mt-6 flex justify-end gap-2">
        {confirmation ? <>
          <button type="button" autoFocus className="rounded-lg border border-slate-300 px-5 py-2.5 font-semibold" onClick={() => finishConfirmation(false)}>취소</button>
          <button type="button" className={`rounded-lg px-5 py-2.5 font-bold text-white ${confirmation.destructive ? 'bg-rose-700' : 'bg-blue-700'}`} onClick={() => finishConfirmation(true)}>{confirmation.confirmLabel ?? '확인'}</button>
        </> : <button type="button" autoFocus className="rounded-lg bg-blue-700 px-5 py-2.5 font-bold text-white" onClick={() => setCompletions(current => current.slice(1))}>확인</button>}
      </div>
    </dialog>}
  </>
}
