import { useCallback, useEffect, useLayoutEffect, useRef, useState } from 'react'
import { confirmAction } from '../../lib/feedback'
import { useDraftGuard } from '../../lib/use-live-refresh'
import { useWorkspaceState } from '../../lib/use-workspace-state'
import type { FormEvent } from 'react'
import type { Project, ProjectForm } from '../../types/domain'
import { projectApi } from './api'

const initialForm: ProjectForm = { project_name: '', client_name: '', site_address: '', contract_date: '', start_date: '', end_date: '', team_leader_id: '', manager_id: '', target_profit_rate: '0', note: '' }

export function useProjects({ onError, onDeleted }: { onError: (message: string | null) => void; onDeleted: (projectId: string) => void }) {
  const [projects, setProjects] = useState<Project[]>([])
  const [keyword, setKeyword] = useWorkspaceState<string>('search', '', undefined, true)
  const [isModalOpen, setIsModalOpen] = useState(false)
  const [form, setForm] = useState<ProjectForm>(initialForm)
  const currentSearch = useRef(keyword)
  useLayoutEffect(() => { currentSearch.current = keyword }, [keyword])
  const sequence = useRef(0)
  useDraftGuard(isModalOpen && JSON.stringify(form) !== JSON.stringify(initialForm))

  const refreshProjects = useCallback(async (search = currentSearch.current) => {
    const request = ++sequence.current
    try {
      const response = await projectApi.list(search)
      if (!response.ok) throw new Error(`프로젝트 목록 조회 실패: ${response.status}`)
      const data: Project[] = await response.json()
      if (request !== sequence.current || search !== currentSearch.current) return
      setProjects(data)
      onError(null)
    } catch (error) { onError(error instanceof Error ? error.message : '프로젝트 목록을 불러오지 못했습니다.') }
  }, [onError])

  useEffect(() => { void refreshProjects() }, [refreshProjects])
  useEffect(() => { const handle = window.setTimeout(() => void refreshProjects(keyword), 200); return () => window.clearTimeout(handle) }, [keyword, refreshProjects])

  const submitProject = useCallback(async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    const response = await projectApi.create({ ...form, target_profit_rate: Number(form.target_profit_rate || 0) })
    if (!response.ok) { onError(`프로젝트 등록 실패: ${response.status}`); return }
    await response.json(); setForm(initialForm); setIsModalOpen(false); await refreshProjects(keyword); onError(null)
  }, [form, keyword, onError, refreshProjects])

  const deleteProject = useCallback(async (project: Project) => {
    if (!await confirmAction({ title: '프로젝트 영구 삭제', message: `“${project.project_name}” 프로젝트와 연결된 계약·예산·발주·기성·지급·결재·변경·현장기록·첨부파일을 모두 영구 삭제할까요?\n삭제 후에는 복구할 수 없습니다.`, confirmLabel: '영구 삭제', destructive: true })) return
    const response = await projectApi.remove(project.project_id)
    if (!response.ok) {
      const body = await response.json().catch(() => null) as { detail?: string; message?: string; error?: string } | null
      onError(body?.detail ?? body?.message ?? body?.error ?? `프로젝트 삭제 실패: ${response.status}`)
      return
    }
    onDeleted(project.project_id); onError(null); await refreshProjects(keyword)
  }, [keyword, onDeleted, onError, refreshProjects])

  return { projects, keyword, setKeyword, isModalOpen, setIsModalOpen, form, setForm, refreshProjects, submitProject, deleteProject }
}
