import { useState } from 'react'
import { UserMenu } from '../auth/AuthProvider'
import { useAuth } from '../auth/auth'
import { AccountManagementPage } from './AccountManagementPage'
import { PositionPermissionPage } from './PositionPermissionPage'
import { AuditLogPage } from './AuditLogPage'
export function AdminApp() {
 const [page, setPage] = useState('accounts'); const { notice } = useAuth()
 return <main className="min-h-screen bg-slate-100 text-slate-900"><header className="border-b bg-white"><div className="mx-auto flex max-w-7xl flex-wrap items-center justify-between gap-4 p-4"><p className="text-lg font-black">아이엔씨 ERP <span className="text-blue-700">시스템 관리</span></p><UserMenu /></div><nav className="mx-auto flex max-w-7xl gap-1 overflow-x-auto px-4">{[['accounts', '계정관리'], ['permissions', '직책별 권한'], ['audit', '감사로그']].map(([p, label]) => <button key={p} onClick={() => setPage(p)} className={`shrink-0 border-b-2 px-4 py-3 font-semibold ${page === p ? 'border-blue-700 text-blue-700' : 'border-transparent text-slate-500'}`}>{label}</button>)}</nav></header><div className="mx-auto max-w-7xl px-4 py-6 sm:px-6">{notice && <p role="status" className="mb-4 rounded bg-amber-50 p-3 text-sm">{notice}</p>}{page === 'accounts' ? <AccountManagementPage /> : page === 'permissions' ? <PositionPermissionPage /> : <AuditLogPage />}</div></main>
}
