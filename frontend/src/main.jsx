import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './index.css'
import App from './App.jsx'
import { installHttpErrorInterceptor } from './api/setupHttp.js'
import { initSentry } from './monitoring/sentry.js'

installHttpErrorInterceptor()
initSentry()

createRoot(document.getElementById('root')).render(
  <>
    <App />
  </>,
)
