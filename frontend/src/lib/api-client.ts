const configuredApiBaseUrl = import.meta.env.VITE_API_BASE_URL ?? ''
const openedFromAnotherComputer = !['localhost', '127.0.0.1', '::1'].includes(window.location.hostname)
const configuredLocalApi = /^https?:\/\/(localhost|127\.0\.0\.1)(?::\d+)?$/i.test(configuredApiBaseUrl)

// 외부 Tunnel로 접속한 브라우저에서 localhost는 방문자 자신의 PC를 뜻한다.
// 이 경우 Vite의 /api 프록시를 거쳐, 프로그램을 실행 중인 PC의 백엔드로 연결한다.
const apiBaseUrl = openedFromAnotherComputer && configuredLocalApi ? '' : configuredApiBaseUrl

export function apiUrl(path: string) {
  return `${apiBaseUrl}${path}`
}
