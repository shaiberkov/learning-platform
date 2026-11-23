import { createRoot } from 'react-dom/client'
import { registerSW } from 'virtual:pwa-register'
import './index.css'
import './Utils/httpSetup.js'
import './i18n/index.js'
import App from './App.jsx'
import { installHttpErrorInterceptor } from './api/setupHttp.js'
import { initSentry } from './monitoring/sentry.js'

installHttpErrorInterceptor()
initSentry()

// Service worker (production builds): offline app shell, cached assets and Web Push.
// A new version activates on the next load; no prompt, the app has no unsaved global state.
registerSW({ immediate: true })

createRoot(document.getElementById('root')).render(
  <>
    <App />
  </>,
)
