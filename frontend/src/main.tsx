import { AuthenticatedApp } from './features/auth/AuthenticatedApp'
import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './index.css'
import { AuthProvider } from './features/auth/AuthProvider'

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <AuthProvider><AuthenticatedApp /></AuthProvider>
  </StrictMode>,
)
