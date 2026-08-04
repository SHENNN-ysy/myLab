import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import './assets/styles/main.css'
import { loadPublicContent, trackPageView } from './composables/usePublicContent'

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

await loadPublicContent()

router.afterEach(to => {
  void trackPageView(to.fullPath)
})

createApp(App).use(router).mount('#app')
