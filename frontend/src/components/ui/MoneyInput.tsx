import { formatNumber } from '../../lib/format'

export function MoneyInput({ value, onChange, disabled = false }: { value: number; onChange: (value: string) => void; disabled?: boolean }) {
  return (
    <div className="flex items-center gap-2 rounded-md border border-slate-300 bg-white px-3 py-2 focus-within:border-cyan-700 focus-within:ring-2 focus-within:ring-cyan-700/15">
      <input
        disabled={disabled}
        value={formatNumber(value)}
        onChange={(event) => onChange(event.target.value)}
        inputMode="numeric"
        className="w-full border-0 bg-transparent text-sm outline-none disabled:cursor-not-allowed"
      />
      <span className="shrink-0 text-xs font-semibold text-slate-400">원</span>
    </div>
  )
}

