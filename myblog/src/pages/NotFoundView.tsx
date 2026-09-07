import { Link } from 'react-router-dom'
import styles from './NotFoundView.module.css'

/* 404 页（standalone 路由，无导航/页脚外壳）：由旧 NotFoundView.vue 平移 */
export default function NotFoundView() {
  return (
    <main
      className={styles['not-found']}
      aria-labelledby="not-found-title"
    >
      <p className={styles['not-found__code']}>
        404
      </p>
      <h1 id="not-found-title">
        页面不存在
      </h1>
      <Link to="/">
        返回首页
      </Link>
    </main>
  )
}
