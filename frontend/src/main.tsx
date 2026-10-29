import { AuthenticatedApp } from './features/auth/AuthenticatedApp'
import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './index.css'
import { AuthProvider } from './features/auth/AuthProvider'
import { FeedbackProvider } from './components/ui/FeedbackProvider'

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <AuthProvider><FeedbackProvider><AuthenticatedApp /></FeedbackProvider></AuthProvider>
  </StrictMode>,
)
