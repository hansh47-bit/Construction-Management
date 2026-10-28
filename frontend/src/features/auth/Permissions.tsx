import type { ReactNode } from 'react'
import { DraftContext, usePermission } from './auth'
export function DraftPermissions({ access, children }: { access: Record<string, boolean>; children: ReactNode }) { return <DraftContext value={access}>{children}</DraftContext> }
export function PermissionGate({ permission, children }: { permission: string; children: ReactNode }) { const allowed = usePermission(); return allowed(permission) ? children : null }
export function WriteScope({ permission, children }: { permission: string; children: ReactNode }) { const allowed = usePermission(); return <><p className={allowed(permission) ? 'hidden' : 'mb-3 rounded border border-slate-200 bg-slate-50 p-3 text-sm text-slate-600'}>조회 전용입니다. 이 자료의 작성·수정 권한이 없습니다.</p><fieldset disabled={!allowed(permission)} className="min-w-0">{children}</fieldset></> }
