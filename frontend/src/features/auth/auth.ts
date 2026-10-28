import { createContext, useContext } from 'react'
export type Position = 'CEO' | 'TEAM_LEADER' | 'FINANCE' | 'STAFF'
export type User = { id: string; login_id: string; name: string; account_type: 'ADMIN' | 'USER'; position: Position | null; permissions: string[]; must_change_password: boolean }
export const positionLabels: Record<Position, string> = { CEO: '대표', TEAM_LEADER: '팀장', FINANCE: '재무', STAFF: '직원' }
export type Auth = { user: User | null; loading: boolean; notice: string; can: (key: string) => boolean; login: (id: string, password: string) => Promise<void>; logout: () => Promise<void>; refresh: () => Promise<void>; changePassword: (current: string, next: string, confirm: string) => Promise<void>; passwordOpen: boolean; setPasswordOpen: (value: boolean) => void }
export const AuthContext = createContext<Auth | null>(null)
export function useAuth() { const value = useContext(AuthContext); if (!value) throw new Error('AuthProvider missing'); return value }
export const DraftContext = createContext<Record<string, boolean>>({})
export function usePermission() { const { can } = useAuth(); const draft = useContext(DraftContext); return (key: string) => can(key) && (draft[key] ?? true) }
