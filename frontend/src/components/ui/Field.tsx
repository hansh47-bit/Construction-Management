import type { ReactNode } from 'react'

export function Field({ label, required, children }: { label: string; required?: boolean; children: ReactNode }) {
  return (
    <label className="block text-sm font-semibold text-slate-700">
      {label}{required && <span className="text-rose-600"> *</span>}
      <span className="mt-1 block">{children}</span>
    </label>
  )
}
