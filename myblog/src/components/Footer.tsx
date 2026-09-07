/* 页脚：版权信息 + 回到顶部（等价 Footer.vue） */
import styles from './Footer.module.css'

export default function Footer() {
  // 平滑滚动回页面顶部
  const scrollToTop = () => {
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }

  return (
    <footer className={styles.footer}>
      <span className={styles.copy}>&copy; 2026 旅行者 · shennn</span>
      <button
        type="button"
        className={styles['back-top']}
        onClick={scrollToTop}
      >
        ↑ 回到顶部
      </button>
    </footer>
  )
}
