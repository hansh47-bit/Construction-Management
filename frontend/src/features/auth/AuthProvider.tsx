import { AuthContext as Context, useAuth, positionLabels } from './auth'
import type { User } from './auth'
import { useCallback, useEffect, useLayoutEffect, useState } from 'react'
import type { ReactNode } from 'react'
import { apiJson, clearApiSession, jsonRequest, refreshCsrf, setRequestPermissions } from '../../lib/api-client'
export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null)
  const [loading, setLoading] = useState(true)
  const [notice, setNotice] = useState('')
  const [passwordOpen, setPasswordOpen] = useState(false)
  const reset = useCallback(() => { clearApiSession(); setUser(null); setPasswordOpen(false) }, [])
  const refresh = useCallback(async () => { try { setUser(await apiJson<User>('/api/v1/auth/me')) } catch { reset() } }, [reset])
  useEffect(() => { let live = true; void apiJson<User>('/api/v1/auth/me').then(u => { if (live) setUser(u) }).catch(() => { if (live) setUser(null) }).finally(() => { if (live) setLoading(false) }); return () => { live = false } }, [])
  useEffect(() => {
    const expired = () => { reset(); setNotice('로그인이 만료되었습니다. 다시 로그인하세요.') }
    const forbidden = (event: Event) => { setNotice((event as CustomEvent<string>).detail); void refresh() }
    window.addEventListener('auth:expired', expired); window.addEventListener('auth:forbidden', forbidden)
    return () => { window.removeEventListener('auth:expired', expired); window.removeEventListener('auth:forbidden', forbidden) }
  }, [reset, refresh])
  async function login(id: string, password: string) { await refreshCsrf(); const next = await apiJson<User>('/api/v1/auth/login', jsonRequest('POST', { login_id: id, password })); await refreshCsrf(); setNotice(''); setUser(next) }
  async function logout() { await apiJson('/api/v1/auth/logout', { method: 'POST' }); reset(); await refreshCsrf(); setNotice('로그아웃되었습니다.') }
  async function changePassword(current: string, next: string, confirm: string) { await apiJson('/api/v1/auth/password', jsonRequest('PUT', { current_password: current, new_password: next, confirm_password: confirm })); reset(); await refreshCsrf(); setNotice('비밀번호가 변경되었습니다. 다시 로그인하세요.') }
  useLayoutEffect(() => { setRequestPermissions(user ? key => user.permissions.includes(key) : null) }, [user])
  return <Context value={{ user, loading, notice, can: key => !!user?.permissions.includes(key), login, logout, refresh, changePassword, passwordOpen, setPasswordOpen }}>{children}</Context>
}
export function UserMenu() {
  const { user, logout, setPasswordOpen } = useAuth()
  return <div className="flex flex-wrap items-center justify-end gap-2 text-xs"><span className="font-bold">{user?.name} · {user?.position ? positionLabels[user.position] : '어드민'}</span><button type="button" onClick={() => setPasswordOpen(true)} className="rounded border px-2 py-1">내 비밀번호 변경</button><button type="button" onClick={() => void logout()} className="rounded border px-2 py-1">로그아웃</button></div>
}
