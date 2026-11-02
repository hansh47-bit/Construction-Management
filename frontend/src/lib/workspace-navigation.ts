export const VIEW_CHANGED_EVENT = 'inc:view-changed'
export const VIEW_KEYS = ['page', 'project', 'tab', 'item', 'approval', 'approvalProject', 'approvalStatus', 'approvalType', 'po', 'search', 'adminPage', 'accountSearch', 'accountPosition', 'accountStatus', 'accountPage', 'auditAction', 'auditSearch', 'auditPage', 'revision', 'change', 'dailyDate', 'meeting', 'permissionPosition'] as const
export type ViewKey = typeof VIEW_KEYS[number]
export type ViewValues = Partial<Record<ViewKey, string | null>>

export function subscribeView(listener: () => void) {
  window.addEventListener('popstate', listener)
  window.addEventListener(VIEW_CHANGED_EVENT, listener)
  return () => { window.removeEventListener('popstate', listener); window.removeEventListener(VIEW_CHANGED_EVENT, listener) }
}

export function cleanViewValue(value: string | null): string | null {
  return value !== null && value.length <= 200 && ![...value].some(character => character.charCodeAt(0) < 32 || character.charCodeAt(0) === 127) ? value : null
}
export function viewFromUrl(url: URL): ViewValues {
  return Object.fromEntries(VIEW_KEYS.filter(key => url.searchParams.has(key)).map(key => [key, cleanViewValue(url.searchParams.get(key))]))
}
export function updateViewUrl(href: string, userId: string, values: ViewValues): URL {
  const url = new URL(href)
  for (const [key, value] of Object.entries(values)) {
    if (!VIEW_KEYS.includes(key as ViewKey)) continue
    const clean = cleanViewValue(value ?? null)
    if (clean === null) url.searchParams.delete(key)
    else url.searchParams.set(key, clean)
  }
  url.searchParams.set('viewAccount', userId)
  return url
}
export function initialViewUrl(href: string, userId: string, saved: ViewValues): URL {
  const url = new URL(href)
  const owner = url.searchParams.get('viewAccount')
  const foreign = owner !== null && owner !== userId
  const hasCurrentView = VIEW_KEYS.some(key => url.searchParams.has(key))
  if (foreign || !hasCurrentView) {
    for (const key of VIEW_KEYS) url.searchParams.delete(key)
    return updateViewUrl(url.href, userId, saved)
  }
  return updateViewUrl(url.href, userId, viewFromUrl(url))
}

export function readSavedView(userId: string): ViewValues {
  try {
    const parsed: unknown = JSON.parse(sessionStorage.getItem(`inc:view:${userId}`) ?? '{}')
    if (!parsed || typeof parsed !== 'object' || Array.isArray(parsed)) return {}
    return Object.fromEntries(Object.entries(parsed).filter(([key, value]) => VIEW_KEYS.includes(key as ViewKey) && (value === null || typeof value === 'string' && cleanViewValue(value) !== null)))
  } catch { return {} }
}
export function saveView(userId: string, url: URL) {
  try { sessionStorage.setItem(`inc:view:${userId}`, JSON.stringify(viewFromUrl(url))) } catch { /* URL persistence still works when browser storage is disabled. */ }
}
export function saveScroll(userId: string, href: string, top = window.scrollY) {
  try { sessionStorage.setItem(`inc:scroll:${userId}:${new URL(href).search}`, String(Math.max(0, top))) } catch { /* Optional preference only. */ }
}
export function readScroll(userId: string, href: string) {
  try { const top = Number(sessionStorage.getItem(`inc:scroll:${userId}:${new URL(href).search}`)); return Number.isFinite(top) ? Math.max(0, top) : 0 } catch { return 0 }
}
export function commitView(userId: string, values: ViewValues, replace = false) {
  const url = updateViewUrl(window.location.href, userId, values)
  if (url.href === window.location.href) return
  saveScroll(userId, window.location.href)
  if (!Object.hasOwn(values, 'page') && !Object.hasOwn(values, 'project')) saveScroll(userId, url.href)
  saveView(userId, url)
  window.history[replace ? 'replaceState' : 'pushState'](null, '', url)
  window.dispatchEvent(new Event(VIEW_CHANGED_EVENT))
}
