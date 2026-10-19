import { Search, Trash2 } from 'lucide-react'
import type { Project } from '../../types/domain'
import { formatDate } from '../../lib/format'

export function ProjectList({
  projects,
  keyword,
  setKeyword,
  onSelect,
  onDelete,
}: {
  projects: Project[]
  keyword: string
  setKeyword: (value: string) => void
  onSelect: (projectId: string) => void
  onDelete: (project: Project) => void
}) {
  return (
    <section className="rounded-md border border-slate-200 bg-white shadow-sm">
      <div className="flex flex-col gap-3 border-b border-slate-200 px-5 py-4 lg:flex-row lg:items-center lg:justify-between">
        <div>
          <h2 className="text-lg font-semibold text-slate-950">프로젝트 목록</h2>
          <p className="mt-1 text-sm text-slate-500">프로젝트를 선택하면 통합현황 상세 화면으로 이동합니다.</p>
        </div>
        <div className="flex w-full items-center gap-2 rounded-md border border-slate-300 bg-white px-3 py-2 lg:w-80">
          <Search className="h-4 w-4 text-slate-400" aria-hidden="true" />
          <input
            value={keyword}
            onChange={(event) => setKeyword(event.target.value)}
            placeholder="프로젝트명 또는 프로젝트번호"
            className="w-full border-0 bg-transparent text-sm outline-none"
          />
        </div>
      </div>
      <div className="overflow-x-auto">
        <table className="min-w-[1320px] w-full text-left text-sm">
          <thead className="bg-slate-50 text-xs font-semibold uppercase text-slate-500">
            <tr>
              <th className="px-4 py-3">프로젝트명(프로젝트번호)</th>
              <th className="px-4 py-3">고객/발주처</th>
              <th className="px-4 py-3">현장주소</th>
              <th className="px-4 py-3">계약일</th>
              <th className="px-4 py-3">착공일</th>
              <th className="px-4 py-3">준공예정일</th>
              <th className="px-4 py-3">담당 팀장/담당자</th>
              <th className="px-4 py-3">프로젝트 상태</th>
              <th className="px-4 py-3">비고</th>
              <th className="px-4 py-3 text-center">삭제</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100">
            {projects.length === 0 ? (
              <tr>
                <td colSpan={10} className="px-4 py-8 text-center text-slate-500">
                  등록된 프로젝트가 없습니다.
                </td>
              </tr>
            ) : (
              projects.map((project) => (
                <tr
                  key={project.project_id}
                  onClick={() => onSelect(project.project_id)}
                  className="cursor-pointer hover:bg-cyan-50"
                >
                  <td className="px-4 py-3">
                    <p className="font-semibold text-slate-950">{project.project_name}</p>
                    <p className="mt-1 text-xs text-slate-500">{project.project_code}</p>
                  </td>
                  <td className="px-4 py-3 text-slate-600">{project.client_name ?? '-'}</td>
                  <td className="px-4 py-3 text-slate-600">{project.site_address ?? '-'}</td>
                  <td className="px-4 py-3 text-slate-600">{formatDate(project.contract_date)}</td>
                  <td className="px-4 py-3 text-slate-600">{formatDate(project.start_date)}</td>
                  <td className="px-4 py-3 text-slate-600">{formatDate(project.end_date)}</td>
                  <td className="px-4 py-3 text-slate-600">
                    <p>{project.team_leader_id ?? '-'}</p>
                    <p className="mt-1 text-xs text-slate-500">{project.manager_id ?? '-'}</p>
                  </td>
                  <td className="px-4 py-3">
                    <span className="rounded-full bg-slate-100 px-2.5 py-1 text-xs font-semibold text-slate-700">
                      {project.status}
                    </span>
                  </td>
                  <td className="px-4 py-3 text-slate-600">{project.note ?? '-'}</td>
                  <td className="px-4 py-3 text-center">
                    <button
                      type="button"
                      onClick={(event) => {
                        event.stopPropagation()
                        onDelete(project)
                      }}
                      disabled={project.status === '정산완료'}
                      className="inline-flex flex-col items-center rounded-md border border-rose-200 px-2.5 py-1.5 text-xs font-semibold text-rose-700 hover:bg-rose-50 disabled:cursor-not-allowed disabled:border-slate-200 disabled:text-slate-400 disabled:hover:bg-transparent"
                      aria-label={`${project.project_name} 삭제`}
                    >
                      <Trash2 className="h-3.5 w-3.5" aria-hidden="true" />
                      <span className="mt-0.5">삭제</span>
                    </button>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>
    </section>
  )
}

