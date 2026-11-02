import { AuthContext as Context, useAuth, positionLabels } from './auth'
import type { User } from './auth'
import { useCallback, useEffect, useLayoutEffect, useRef, useState } from 'react'
import type { ReactNode } from 'react'
import { ApiError, apiJson, clearApiSession, jsonRequest, setRequestPermissions } from '../../lib/api-client'
export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null)
  const [loading, setLoading] = useState(true)
  const [notice, setNotice] = useState('')
  const [passwordOpen, setPasswordOpen] = useState(false)
  const version = useRef(0)
  const logoutRequest = useRef<Promise<void> | null>(null)
  const reset = useCallback(() => {
    version.current++
    clearApiSession()
    setUser(null)
    setPasswordOpen(false)
  }, [])
  const refresh = useCallback(async () => {
    const current = version.current
    try {
      const next = await apiJson<User>('/api/v1/auth/me')
      if (current === version.current) setUser(next)
    } catch (error) {
      if (current === version.current && error instanceof ApiError && error.status === 401) reset()
    }
  }, [reset])
  useEffect(() => {
    let live = true
    const current = version.current
    void apiJson<User>('/api/v1/auth/me')
      .then(next => { if (live && current === version.current) setUser(next) })
      .catch(error => {
        if (live && current === version.current && error instanceof ApiError && error.status === 401) reset()
      })
      .finally(() => { if (live) setLoading(false) })
    return () => { live = false }
  }, [reset])
  useEffect(() => {
    const expired = () => { reset(); setNotice('로그인이 만료되었습니다. 다시 로그인하세요.') }
    const forbidden = (event: Event) => { setNotice((event as CustomEvent<string>).detail); void refresh() }
    window.addEventListener('auth:expired', expired); window.addEventListener('auth:forbidden', forbidden)
    return () => { window.removeEventListener('auth:expired', expired); window.removeEventListener('auth:forbidden', forbidden) }
  }, [reset, refresh])
  async function login(id: string, password: string) {
    if (logoutRequest.current) await logoutRequest.current
    reset()
    const current = version.current
    setNotice('')
    const next = await apiJson<User>('/api/v1/auth/login', jsonRequest('POST', { login_id: id, password }))
    if (current === version.current) setUser(next)
  }
  async function logout() {
    if (logoutRequest.current) return logoutRequest.current
    const pending = (async () => {
      try {
        await apiJson('/api/v1/auth/logout', { method: 'POST' })
      } catch (error) {
        if (!(error instanceof ApiError && error.status === 401)) {
          setNotice(error instanceof Error ? error.message : '로그아웃하지 못했습니다.')
          throw error
        }
      }
      reset()
      // The next login creates a new session; logout does not create an anonymous cookie.
      setNotice('로그아웃되었습니다.')
    })()
    logoutRequest.current = pending
    try { await pending } finally {
      if (logoutRequest.current === pending) logoutRequest.current = null
    }
  }
  async function changePassword(current: string, next: string, confirm: string) {
    await apiJson('/api/v1/auth/password', jsonRequest('PUT', { current_password: current, new_password: next, confirm_password: confirm }))
    reset()
    setNotice('비밀번호가 변경되었습니다. 다시 로그인하세요.')
  }
  useLayoutEffect(() => { setRequestPermissions(user ? key => user.permissions.includes(key) : null) }, [user])
  return <Context value={{ user, loading, notice, can: key => !!user?.permissions.includes(key), login, logout, refresh, changePassword, passwordOpen, setPasswordOpen }}>{children}</Context>
}
export function UserMenu() {
  const { user, logout, setPasswordOpen } = useAuth()
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  return <div className="flex flex-wrap items-center justify-end gap-2 text-xs"><span className="font-bold">{user?.name} · {user?.position ? positionLabels[user.position] : '어드민'}</span><button type="button" disabled={busy} onClick={() => setPasswordOpen(true)} className="rounded border px-2 py-1">내 비밀번호 변경</button><button type="button" disabled={busy} onClick={() => { setBusy(true); setError(''); void logout().catch(e => setError(e instanceof Error ? e.message : String(e))).finally(() => setBusy(false)) }} className="rounded border px-2 py-1 disabled:opacity-50">{busy ? '로그아웃 중…' : '로그아웃'}</button>{error && <span role="alert" className="text-rose-700">{error}</span>}</div>
}
