import { useCallback, useContext, useSyncExternalStore } from 'react'
import type { Dispatch, SetStateAction } from 'react'
import { commitView, subscribeView } from './workspace-navigation.ts'
import type { ViewKey, ViewValues } from './workspace-navigation.ts'
import { WorkspaceContext } from './workspace-context'

export function useWorkspaceUpdate() {
  const userId = useContext(WorkspaceContext)
  if (!userId) throw new Error('WorkspaceProvider missing')
  return useCallback((values: ViewValues, replace = false) => commitView(userId, values, replace), [userId])
}

export function useWorkspaceState<T extends string | null>(key: ViewKey, initial: T, allowed?: readonly string[], replace = false): [T, Dispatch<SetStateAction<T>>] {
  const update = useWorkspaceUpdate()
  const read = () => {
    const url = new URL(window.location.href)
    const value = url.searchParams.get(key)
    return value !== null && value.length <= 200 && (!allowed || allowed.includes(value)) ? value as T : initial
  }
  const value = useSyncExternalStore(subscribeView, read)
  const setValue: Dispatch<SetStateAction<T>> = next => update({ [key]: typeof next === 'function' ? next(read()) : next }, replace)
  return [value, setValue]
}

export function useWorkspaceNumber(key: ViewKey, initial = 0): [number, Dispatch<SetStateAction<number>>] {
  const [stored, setStored] = useWorkspaceState(key, String(initial), undefined, true)
  const value = /^\d{1,5}$/.test(stored) ? Number(stored) : initial
  return [value, next => setStored(String(typeof next === 'function' ? next(value) : next))]
}
