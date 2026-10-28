import { useEffect, useState } from 'react'
import { apiJson } from '../../lib/api-client'
import type { Position } from '../auth/auth'
import type { FormEvent } from 'react'
import { X } from 'lucide-react'
import type { ProjectForm } from '../../types/domain'
import { Field } from '../../components/ui/Field'

export function ProjectCreationModal({
  form,
  setForm,
  onClose,
  onSubmit,
}: {
  form: ProjectForm
  setForm: (form: ProjectForm) => void
  onClose: () => void
  onSubmit: (event: FormEvent<HTMLFormElement>) => void
}) {
  const [users, setUsers] = useState<{ id: string; name: string; position: Position }[]>([])
  useEffect(() => { void apiJson<typeof users>('/api/v1/users/options').then(setUsers).catch(() => {}) }, [])
  function update(field: keyof ProjectForm, value: string) {
    setForm({ ...form, [field]: value })
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/45 p-4">
      <form onSubmit={onSubmit} className="max-h-[92vh] w-full max-w-4xl overflow-auto rounded-md bg-white shadow-xl">
        <div className="sticky top-0 flex items-center justify-between border-b border-slate-200 bg-white px-5 py-4">
          <div>
            <h2 className="text-lg font-semibold text-slate-950">신규 프로젝트 등록</h2>
            <p className="mt-1 text-sm text-slate-500">프로젝트번호는 등록 시 자동 생성됩니다.</p>
          </div>
          <button type="button" onClick={onClose} className="rounded-md p-2 text-slate-500 hover:bg-slate-100" aria-label="닫기">
            <X className="h-5 w-5" aria-hidden="true" />
          </button>
        </div>

        <div className="grid gap-4 p-5 md:grid-cols-2">
          <Field label="프로젝트명" required>
            <input required value={form.project_name} onChange={(e) => update('project_name', e.target.value)} placeholder="성가수녀원 리뉴얼공사" className="input" />
          </Field>
          <Field label="고객/발주처">
            <input value={form.client_name} onChange={(e) => update('client_name', e.target.value)} className="input" />
          </Field>
          <Field label="현장주소">
            <input value={form.site_address} onChange={(e) => update('site_address', e.target.value)} className="input" />
          </Field>
          <Field label="계약일">
            <input type="date" value={form.contract_date} onChange={(e) => update('contract_date', e.target.value)} className="input" />
          </Field>
          <Field label="착공일">
            <input type="date" value={form.start_date} onChange={(e) => update('start_date', e.target.value)} className="input" />
          </Field>
          <Field label="준공예정일">
            <input type="date" value={form.end_date} onChange={(e) => update('end_date', e.target.value)} className="input" />
          </Field>
          <Field label="담당 팀장">
            <select value={form.team_leader_id} onChange={e => update('team_leader_id', e.target.value)} className="input"><option value="">선택 안 함</option>{users.filter(u => u.position === 'TEAM_LEADER').map(u => <option key={u.id} value={u.id}>{u.name}</option>)}</select>
          </Field>
          <Field label="담당자">
            <select value={form.manager_id} onChange={e => update('manager_id', e.target.value)} className="input"><option value="">선택 안 함</option>{users.map(u => <option key={u.id} value={u.id}>{u.name}</option>)}</select>
          </Field>
          <Field label="비고">
            <input value={form.note} onChange={(e) => update('note', e.target.value)} className="input" />
          </Field>
        </div>

        <div className="sticky bottom-0 flex justify-end gap-2 border-t border-slate-200 bg-white px-5 py-4">
          <button type="button" onClick={onClose} className="rounded-md border border-slate-300 px-4 py-2 text-sm font-semibold text-slate-700 hover:bg-slate-50">
            취소
          </button>
          <button type="submit" className="rounded-md bg-cyan-700 px-4 py-2 text-sm font-semibold text-white hover:bg-cyan-800">
            등록
          </button>
        </div>
      </form>
    </div>
  )
}


