import { createApp } from 'vue'
import App from './App.vue'
import './assets/styles/main.css'

const scrollToHome = () => {
  window.scrollTo({ top: 0, left: 0, behavior: 'auto' })
  document.documentElement.scrollTop = 0
  document.body.scrollTop = 0
}

if ('scrollRestoration' in window.history) {
  window.history.scrollRestoration = 'manual'
}

scrollToHome()
window.addEventListener('load', scrollToHome, { once: true })

createApp(App).mount('#app')
