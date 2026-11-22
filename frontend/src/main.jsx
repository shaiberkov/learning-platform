import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './index.css'
import './Utils/httpSetup.js'
import App from './App.jsx'
import { installAuthInterceptor } from './Utils/authInterceptor.js'

installAuthInterceptor()

createRoot(document.getElementById('root')).render(
  <>
    <App />
  </>,
)
