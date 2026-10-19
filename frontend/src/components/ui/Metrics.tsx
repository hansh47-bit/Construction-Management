import type { ReactNode } from 'react'

export function SubMetric({
  label,
  value,
  icon,
  accent = 'text-slate-950',
}: {
  label: string
  value: string
  icon: ReactNode
  accent?: string
}) {
  return (
    <article className="rounded-md border border-slate-200 bg-white p-4 shadow-sm">
      <div className="flex items-center justify-between gap-2">
        <p className="text-sm font-medium text-slate-500">{label}</p>
        <span className="text-slate-400 [&>svg]:h-4 [&>svg]:w-4">{icon}</span>
      </div>
      <p className={`mt-3 text-lg font-semibold ${accent}`}>{value}</p>
    </article>
  )
}

export function MetricPanel({
  label,
  value,
  helper,
  accent = 'text-slate-950',
}: {
  label: string
  value: string
  helper: string
  accent?: string
}) {
  return (
    <article className="rounded-md border border-slate-200 bg-white p-4 shadow-sm">
      <p className="text-sm font-medium text-slate-500">{label}</p>
      <p className={`mt-2 text-xl font-semibold ${accent}`}>{value}</p>
      <p className="mt-1 text-sm text-slate-500">{helper}</p>
    </article>
  )
}

