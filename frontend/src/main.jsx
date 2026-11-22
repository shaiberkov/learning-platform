import { createRoot } from 'react-dom/client'
import { registerSW } from 'virtual:pwa-register'
import './index.css'
import './i18n/index.js'
import App from './App.jsx'

// Service worker (production builds): offline app shell, cached assets and Web Push.
// A new version activates on the next load; no prompt, the app has no unsaved global state.
registerSW({ immediate: true })

createRoot(document.getElementById('root')).render(
  <>
    <App />
  </>,
)
