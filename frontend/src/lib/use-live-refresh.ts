import { useEffect, useId, useLayoutEffect, useRef } from 'react'
import { DATA_CHANGED_EVENT, hasPendingMutation, setDraftDirty } from './feedback'

export function useLiveRefresh(refresh: () => void | Promise<unknown>, enabled = true, intervalMs = 8000) {
  const callback = useRef(refresh)
  useLayoutEffect(() => { callback.current = refresh })
  useEffect(() => {
    if (!enabled) return
    let disposed = false
    let running = false
    let queued = false
    let debounce: ReturnType<typeof setTimeout> | undefined
    async function run() {
      if (disposed || document.visibilityState === 'hidden' || navigator.onLine === false) return
      if (running) { queued = true; return }
      if (hasPendingMutation()) { schedule(); return }
      running = true
      try { await callback.current() } catch { /* Keep existing data on a temporary read failure. */ }
      finally { running = false; if (queued && !disposed) { queued = false; schedule() } }
    }
    function schedule() { clearTimeout(debounce); debounce = setTimeout(() => { void run() }, 250) }
    const interval = setInterval(() => { void run() }, intervalMs)
    window.addEventListener('focus', schedule)
    window.addEventListener('online', schedule)
    window.addEventListener(DATA_CHANGED_EVENT, schedule)
    document.addEventListener('visibilitychange', schedule)
    return () => {
      disposed = true
      clearInterval(interval)
      clearTimeout(debounce)
      window.removeEventListener('focus', schedule)
      window.removeEventListener('online', schedule)
      window.removeEventListener(DATA_CHANGED_EVENT, schedule)
      document.removeEventListener('visibilitychange', schedule)
    }
  }, [enabled, intervalMs])
}

export function useDraftGuard(dirty: boolean) {
  const id = useId()
  useLayoutEffect(() => { setDraftDirty(id, dirty); return () => setDraftDirty(id, false) }, [id, dirty])
}
