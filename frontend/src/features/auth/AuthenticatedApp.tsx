import App from '../../App'
import { useAuth } from './auth'
import { LoginPage } from './LoginPage'
import { PasswordChangePage } from './PasswordChangePage'
import { AdminApp } from '../admin/AdminApp'
export function AuthenticatedApp() {
 const { user, loading, passwordOpen } = useAuth()
 if (loading) return <main className="grid min-h-screen place-items-center">로그인 상태 확인 중…</main>
 if (!user) return <LoginPage />
 if (user.must_change_password || passwordOpen) return <PasswordChangePage key={user.id} />
 return user.account_type === 'ADMIN' ? <AdminApp key={user.id} /> : <App key={user.id} />
}
