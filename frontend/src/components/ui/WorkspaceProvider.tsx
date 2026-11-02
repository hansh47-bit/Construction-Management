import { useEffect, useLayoutEffect, useRef, useState, useSyncExternalStore } from 'react'
import type { ReactNode } from 'react'
import { WorkspaceContext } from '../../lib/workspace-context'
import { initialViewUrl, readSavedView, readScroll, saveScroll, saveView, subscribeView } from '../../lib/workspace-navigation'

export function WorkspaceProvider({ userId, children }: { userId: string; children: ReactNode }) {
  const [ready, setReady] = useState(false)
  useLayoutEffect(() => {
    const url = initialViewUrl(window.location.href, userId, readSavedView(userId))
    window.history.replaceState(null, '', url)
    saveView(userId, url)
    // Children must not read another account's URL before this authentication boundary normalizes it.
    // eslint-disable-next-line react/set-state-in-effect
    setReady(true)
  }, [userId])
  return <WorkspaceContext value={userId}>{ready ? <><ScrollRestoration userId={userId} />{children}</> : <p role="status">이전에 보던 화면을 불러오는 중…</p>}</WorkspaceContext>
}

function ScrollRestoration({ userId }: { userId: string }) {
  const href = useSyncExternalStore(subscribeView, () => window.location.href)
  const currentHref = useRef(href)
  useEffect(() => {
    const previous = window.history.scrollRestoration
    window.history.scrollRestoration = 'manual'
    const save = () => saveScroll(userId, currentHref.current)
    window.addEventListener('beforeunload', save)
    return () => { save(); window.history.scrollRestoration = previous; window.removeEventListener('beforeunload', save) }
  }, [userId])
  useLayoutEffect(() => {
    currentHref.current = href
    const top = readScroll(userId, href)
    let attempts = 0
    const restore = () => { window.scrollTo({ top, behavior: 'instant' }); if (++attempts >= 12 || window.scrollY >= top - 2) clearInterval(timer) }
    const timer = setInterval(restore, 120)
    const stop = () => clearInterval(timer)
    window.addEventListener('wheel', stop, { passive: true })
    window.addEventListener('touchstart', stop, { passive: true })
    restore()
    return () => {
      clearInterval(timer)
      window.removeEventListener('wheel', stop)
      window.removeEventListener('touchstart', stop)
      saveScroll(userId, href)
    }
  }, [href, userId])
  return null
}
