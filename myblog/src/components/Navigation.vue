<template>
  <!--
    floating pill 导航（参考 taozhiyy 风格）：
    顶部完全透明浮在 hero 上；滚出 hero 后加 pill 背景；
    下滑收起，上滑显示；移动端有完整 drawer 交互。
  -->
  <nav
    class="navigation"
    :class="{
      'is-raised': isRaised,
      'is-hidden': isHidden,
    }"
    aria-label="Primary"
  >
    <a
      href="#hero-cinema"
      class="nav-logo"
      aria-label="返回首页"
      @click="onNavClick({ label: '首页', hash: '#hero-cinema' }, $event)"
    >
      <img
        class="nav-logo-img"
        :src="navAvatar"
        alt=""
        loading="eager"
        decoding="async"
        @error="onAvatarError"
      />
      <span class="nav-logo-label">shennn</span>
    </a>

    <ul class="nav-links">
      <li v-for="item in navLinks" :key="item.label">
        <a
          :href="item.path ?? item.hash"
          class="nav-hover-btn"
          :class="{ 'is-active': item.path !== undefined && item.path === route.path }"
          @click="onNavClick(item, $event)"
        >
          {{ item.label }}
        </a>
      </li>
    </ul>

    <div class="nav-right">
      <button
        ref="menuBtnRef"
        type="button"
        class="nav-menu-btn"
        aria-label="打开导航菜单"
        :aria-expanded="mobileOpen"
        aria-controls="site-mobile-nav"
        @click="openMobile"
      >
        <span class="nav-menu-btn-bar" />
        <span class="nav-menu-btn-bar" />
        <span class="nav-menu-btn-bar" />
      </button>
    </div>
  </nav>

  <!-- Mobile drawer：Teleport 到 body，避免被父级 transform 影响 -->
  <Teleport to="body" v-if="drawerMounted">
    <div class="nav-mobile-root" :aria-hidden="!drawerVisible">
      <button
        type="button"
        class="nav-mobile-backdrop"
        :class="{ 'is-open': drawerVisible }"
        :tabindex="drawerVisible ? 0 : -1"
        aria-label="关闭导航菜单"
        @click="closeMobile"
      />
      <div
        id="site-mobile-nav"
        class="nav-mobile-drawer"
        :class="{ 'is-open': drawerVisible }"
        role="dialog"
        aria-modal="true"
        aria-label="站点导航"
        :aria-hidden="!drawerVisible"
      >
        <div class="nav-mobile-drawer-head">
          <span class="nav-mobile-drawer-title">菜单</span>
          <button
            ref="closeBtnRef"
            type="button"
            class="nav-mobile-close"
            aria-label="关闭菜单"
            @click="closeMobile"
          >
            <span aria-hidden="true">×</span>
          </button>
        </div>
        <nav class="nav-mobile-links">
          <a
            v-for="item in navLinks"
            :key="item.label"
            :href="item.path ?? item.hash"
            class="nav-mobile-link"
            @click="onNavClick(item, $event)"
          >
            {{ item.label }}
          </a>
        </nav>
      </div>
    </div>
  </Teleport>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { usePublicContent } from '@/composables/usePublicContent'

/* ============ 导航头像：与"关于我"头像一致，未配置或加载失败时回退 404 默认图 ============ */
const FALLBACK_AVATAR = '/assets/404.png'
const { content } = usePublicContent()
const avatarLoadFailed = ref(false)
const navAvatar = computed(() => {
  const avatarUrl = content.value.about?.profile?.avatar_url
  return avatarUrl && !avatarLoadFailed.value ? avatarUrl : FALLBACK_AVATAR
})
function onAvatarError() {
  avatarLoadFailed.value = true
}

/* ============ 导航链接配置 ============
 * hash：首页区块锚点；path：独立路由页面。
 */
interface NavItem {
  label: string
  hash?: string
  path?: string
}

const navLinks: NavItem[] = [
  { label: '首页', hash: '#hero-cinema' },
  { label: '关于', hash: '#influencer' },
  { label: '技能', hash: '#skills' },
  { label: '项目', hash: '#work' },
  { label: '足迹', hash: '#hobbies' },
  { label: '爱好', hash: '#game' },
  { label: 'Vibe Coding', hash: '#aicoding' },
  { label: 'MyLab', path: '/mylab' },
]

/* ============ 导航点击：同页平滑滚动 / 跨页路由跳转 ============ */
const route = useRoute()
const router = useRouter()

function onNavClick(item: NavItem, event: MouseEvent) {
  closeMobile()
  event.preventDefault()
  if (item.path) {
    router.push(item.path)
    return
  }
  if (!item.hash) return
  if (route.path === '/') {
    // 已在首页：原生平滑滚动到目标区块
    document.querySelector(item.hash)?.scrollIntoView({ behavior: 'smooth' })
  } else {
    router.push({ path: '/', hash: item.hash })
  }
}

/* ============ 滚动方向检测：下滑收起 / 上滑显示 ============
 * 对齐 taozhiyy Navbar.jsx 第 178-191 行的核心逻辑：
 *  - scrollY === 0 时：nav 一定显示，且完全透明
 *  - 滚出 hero 后：自动加 pill 背景（is-raised），并按方向切换显隐
 *  - 下滑收、上滑显
 */
const TOP_THRESHOLD = 8
const DIRECTION_DEAD_ZONE = 4

const isHidden = ref(false)
const isRaised = ref(false)
let lastScrollY = 0
let ticking = false

function updateNavState() {
  const currentY = window.scrollY

  if (currentY <= TOP_THRESHOLD) {
    isHidden.value = false
    isRaised.value = false
    lastScrollY = currentY
    ticking = false
    return
  }

  // 滚出顶部：始终抬起（加 pill 背景），避免无背景 nav 看不见链接
  if (!isRaised.value) isRaised.value = true

  const delta = currentY - lastScrollY

  if (Math.abs(delta) < DIRECTION_DEAD_ZONE) {
    ticking = false
    return
  }

  if (delta > 0) {
    isHidden.value = true // 向下 → 收起
  } else if (delta < 0) {
    isHidden.value = false // 向上 → 显示
  }

  lastScrollY = currentY
  ticking = false
}

function onScroll() {
  if (ticking) return
  ticking = true
  window.requestAnimationFrame(updateNavState)
}

onMounted(() => {
  lastScrollY = window.scrollY
  window.addEventListener('scroll', onScroll, { passive: true })
})

onBeforeUnmount(() => {
  window.removeEventListener('scroll', onScroll)
})

/* ============ Mobile drawer ============
 * 对齐 taozhiyy：ESC 关闭、焦点管理、body 锁滚动、220ms 离场。
 */
const DRAWER_MS = 220

const menuBtnRef = ref<HTMLButtonElement | null>(null)
const closeBtnRef = ref<HTMLButtonElement | null>(null)

const mobileOpen = ref(false)
const drawerMounted = ref(false)
const drawerVisible = ref(false)

function openMobile() {
  mobileOpen.value = true
  drawerMounted.value = true
  document.body.style.overflow = 'hidden'
  // 双 rAF：等节点挂载完再加 .is-open，让 transition 真正生效
  requestAnimationFrame(() => {
    requestAnimationFrame(() => {
      drawerVisible.value = true
    })
  })
}

function closeMobile() {
  if (!drawerMounted.value) return
  drawerVisible.value = false
  // 焦点回到汉堡按钮（如果还存在）
  menuBtnRef.value?.focus()
  window.setTimeout(() => {
    mobileOpen.value = false
    drawerMounted.value = false
    document.body.style.overflow = ''
  }, DRAWER_MS)
}

function onKeydown(event: KeyboardEvent) {
  if (event.key === 'Escape' && drawerMounted.value) {
    closeMobile()
  }
}

onMounted(() => {
  document.addEventListener('keydown', onKeydown)
})

onBeforeUnmount(() => {
  document.removeEventListener('keydown', onKeydown)
  // 卸载时确保 body 滚动锁被解开
  document.body.style.overflow = ''
})

// drawerVisible 变 true 时，把焦点送到关闭按钮（无障碍）
watch(drawerVisible, (visible) => {
  if (!visible) return
  window.setTimeout(() => closeBtnRef.value?.focus(), 40)
})
</script>

<style scoped>
/* ============ 容器：floating pill ============ */
.navigation {
  position: fixed;
  /* floating pill：上 + 左右内缩（呼应 taozhiyy `inset-x-N top-N`） */
  top: 0.5rem;
  left: 0.75rem;
  right: 0.75rem;
  z-index: 60;
  height: 56px;

  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0 clamp(0.75rem, 1.6vw, 1.25rem);

  /* 默认（顶部）状态：完全透明，无阴影，无边框 */
  background: transparent;
  backdrop-filter: none;
  -webkit-backdrop-filter: none;
  border: 1px solid transparent;
  border-radius: 999px;
  color: rgba(255, 250, 242, 0.94);

  transition:
    transform 0.32s cubic-bezier(0.4, 0, 0.2, 1),
    background-color 0.4s ease,
    border-color 0.4s ease,
    box-shadow 0.4s ease,
    backdrop-filter 0.4s ease,
    -webkit-backdrop-filter 0.4s ease,
    top 0.4s ease,
    left 0.4s ease,
    right 0.4s ease;
  will-change: transform;
}

@media (min-width: 640px) {
  .navigation {
    top: 0.875rem;
    left: 1.25rem;
    right: 1.25rem;
    height: 60px;
  }
}

@media (min-width: 1024px) {
  .navigation {
    top: 1rem;
    left: 1.5rem;
    right: 1.5rem;
    height: 64px;
  }
}

/* 滚出 hero：加 pill 背景（清澈海水 + 蓝色描边 + 阴影） */
.navigation.is-raised {
  background-color: rgba(255, 255, 255, 0.88);
  backdrop-filter: blur(14px) saturate(150%);
  -webkit-backdrop-filter: blur(14px) saturate(150%);
  border-color: rgba(91, 164, 230, 0.35);
  box-shadow:
    0 8px 28px rgba(27, 58, 75, 0.12),
    0 2px 6px rgba(27, 58, 75, 0.08),
    inset 0 1px 0 rgba(255, 255, 255, 0.8);
  color: rgba(27, 58, 75, 0.92);
}

/* 向下滚动：nav 整体滑出（+ 淡出，参考 GSAP 同时改 y/opacity） */
.navigation.is-hidden {
  transform: translateY(calc(-100% - 1.25rem));
  opacity: 0;
  pointer-events: none;
  transition:
    transform 0.32s cubic-bezier(0.4, 0, 0.2, 1),
    opacity 0.32s cubic-bezier(0.4, 0, 0.2, 1),
    background-color 0.4s ease,
    border-color 0.4s ease,
    box-shadow 0.4s ease,
    backdrop-filter 0.4s ease,
    -webkit-backdrop-filter 0.4s ease,
    top 0.4s ease,
    left 0.4s ease,
    right 0.4s ease;
}

/* ============ Logo（圆形头像 + 文字标签） ============ */
.nav-logo {
  display: inline-flex;
  align-items: center;
  gap: 0.55rem;
  height: 100%;
  flex-shrink: 0;
  text-decoration: none;
  color: inherit;
}

.nav-logo-img {
  display: block;
  /* 头像高度 ≈ 容器高度的 70%，居中 */
  height: 70%;
  aspect-ratio: 1 / 1;
  border-radius: 50%;
  object-fit: cover;
  /* 天空蓝描边 + 微光晕：pill 抬起时与底色相称；透明时仍可见 */
  border: 1.5px solid rgba(91, 164, 230, 0.6);
  box-shadow:
    0 0 0 1px rgba(27, 58, 75, 0.12),
    0 4px 14px rgba(27, 58, 75, 0.22),
    inset 0 0 0 1px rgba(255, 255, 255, 0.3);
  transition:
    border-color 0.4s ease,
    box-shadow 0.4s ease,
    transform 0.2s ease;
}

.nav-logo:hover .nav-logo-img {
  transform: scale(1.04);
  border-color: rgba(91, 164, 230, 0.9);
}

/* 文字标签：紧贴头像右侧，垂直居中 */
.nav-logo-label {
  font-family: var(--font-display);
  font-size: 1.05rem;
  font-weight: 700;
  letter-spacing: 0.04em;
  line-height: 1;
  /* 默认：顶部透明态——清澈蓝色调，与海天背景融合 */
  color: rgba(255, 255, 255, 0.96);
  text-shadow:
    0 1px 0 rgba(27, 58, 75, 0.5),
    0 0 18px rgba(91, 164, 230, 0.25);
  transition: color 0.4s ease, text-shadow 0.4s ease;
}

/* pill 抬起：文字切到深海色，去掉海面光晕 */
.navigation.is-raised .nav-logo-label {
  color: rgba(27, 58, 75, 0.92);
  text-shadow: none;
}

/* pill 抬起：头像描边换成天空蓝，去掉暗环 */
.navigation.is-raised .nav-logo-img {
  border-color: rgba(91, 164, 230, 0.75);
  box-shadow:
    0 0 0 1px rgba(255, 255, 255, 0.9),
    0 4px 12px rgba(27, 58, 75, 0.15),
    inset 0 0 0 1px rgba(255, 255, 255, 0.7);
}

/* ============ 链接（推到右侧） ============ */
.nav-links {
  display: flex;
  align-items: center;
  gap: clamp(0.9rem, 1.8vw, 1.5rem);
  /* 关键：把整组链接推到右侧 */
  margin-left: auto;
  margin-right: clamp(0.4rem, 1vw, 0.8rem);
  list-style: none;
}

/* 对齐 taozhiyy .nav-hover-btn：after 下划线 scaleX(0→1) 扫开 */
.nav-hover-btn {
  position: relative;
  display: inline-block;
  font-family: var(--font-body);
  font-size: 0.78rem;
  font-weight: 500;
  letter-spacing: 0.04em;
  color: rgba(255, 255, 255, 0.9);
  text-decoration: none;
  white-space: nowrap;
  padding: 0.35rem 0;
  transition: color 0.2s ease;
}

.nav-hover-btn::after {
  content: '';
  position: absolute;
  left: 0;
  bottom: -2px;
  height: 1.5px;
  width: 100%;
  background: rgba(91, 164, 230, 0.95);
  transform: scaleX(0);
  transform-origin: bottom right;
  transition: transform 0.3s cubic-bezier(0.65, 0.05, 0.36, 1);
}

.nav-hover-btn:hover {
  color: rgba(255, 255, 255, 1);
}

/* 当前所在路由页面（如 myLab）高亮 */
.nav-hover-btn.is-active {
  color: rgba(255, 255, 255, 1);
}
.nav-hover-btn.is-active::after {
  transform: scaleX(1);
  transform-origin: bottom left;
}

.nav-hover-btn:hover::after {
  transform: scaleX(1);
  transform-origin: bottom left;
}

/* pill 抬起：链接色切到深色，hover 下划线换成天空蓝 */
.navigation.is-raised .nav-hover-btn {
  color: rgba(27, 58, 75, 0.82);
}
.navigation.is-raised .nav-hover-btn:hover,
.navigation.is-raised .nav-hover-btn.is-active {
  color: rgba(27, 58, 75, 0.98);
}
.navigation.is-raised .nav-hover-btn::after {
  background: rgba(91, 164, 230, 0.9);
}

/* ============ 右侧：汉堡按钮 ============ */
.nav-right {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  flex-shrink: 0;
}

/* 汉堡按钮：移动端才显示 */
.nav-menu-btn {
  display: none;
  position: relative;
  align-items: center;
  justify-content: center;
  flex-direction: column;
  gap: 5px;
  width: 38px;
  height: 38px;
  padding: 0;
  border: 1px solid rgba(255, 255, 255, 0.45);
  border-radius: 12px;
  background: rgba(27, 58, 75, 0.25);
  color: rgba(255, 255, 255, 0.95);
  cursor: pointer;
  transition:
    border-color 0.3s ease,
    background 0.3s ease,
    color 0.3s ease;
}
.nav-menu-btn:hover {
  background: rgba(27, 58, 75, 0.4);
  border-color: rgba(255, 255, 255, 0.7);
}

.nav-menu-btn-bar {
  display: block;
  width: 18px;
  height: 1.5px;
  border-radius: 999px;
  background: currentColor;
  transition: transform 0.2s ease;
}

.navigation.is-raised .nav-menu-btn {
  border-color: rgba(91, 164, 230, 0.4);
  background: rgba(91, 164, 230, 0.1);
  color: rgba(27, 58, 75, 0.9);
}
.navigation.is-raised .nav-menu-btn:hover {
  background: rgba(91, 164, 230, 0.2);
  border-color: rgba(91, 164, 230, 0.6);
}

/* ============ 响应式：移动端只显示头像 + 汉堡 ============ */
@media (max-width: 900px) {
  .nav-links {
    display: none;
  }
}

@media (max-width: 720px) {
  .nav-menu-btn {
    display: inline-flex;
  }
  .nav-logo-label {
    /* 移动端：导航链接收进 drawer，标签也隐藏，只保留头像 */
    display: none;
  }
  .nav-logo-img {
    /* 移动端头像稍小一点，让 nav 不显得拥挤 */
    height: 64%;
  }
}

@media (max-width: 480px) {
  .navigation {
    left: 0.5rem;
    right: 0.5rem;
    padding: 0 0.75rem;
  }
  .nav-logo-img {
    height: 60%;
  }
}

/* ============ 减少动画偏好 ============ */
@media (prefers-reduced-motion: reduce) {
  .navigation {
    transition: none !important;
  }
}
</style>

<!--
  mobile drawer 的样式需要脱离 scoped（被 Teleport 到 body，
  scoped 的 hash 选择器无法命中 body 下的节点）。
  这里用非 scoped 块（带 :deep / global）保证选择器全局生效。
-->
<style>
.nav-mobile-root {
  position: fixed;
  inset: 0;
  z-index: 70;
  pointer-events: none;
}
.nav-mobile-root[aria-hidden='false'] {
  pointer-events: auto;
}

.nav-mobile-backdrop {
  position: absolute;
  inset: 0;
  border: none;
  padding: 0;
  background: rgba(20, 18, 16, 0.55);
  backdrop-filter: blur(6px);
  -webkit-backdrop-filter: blur(6px);
  opacity: 0;
  transition: opacity 0.22s ease;
  cursor: pointer;
}
.nav-mobile-backdrop.is-open {
  opacity: 1;
}

.nav-mobile-drawer {
  position: absolute;
  top: 0;
  right: 0;
  bottom: 0;
  width: min(82vw, 320px);
  padding: 1.25rem 1.25rem 1.5rem;
  background: linear-gradient(
    160deg,
    rgba(255, 255, 255, 0.98) 0%,
    rgba(232, 244, 253, 0.98) 100%
  );
  border-left: 1px solid rgba(91, 164, 230, 0.3);
  box-shadow: -16px 0 40px rgba(27, 58, 75, 0.12);
  transform: translateX(100%);
  transition: transform 0.22s cubic-bezier(0.4, 0, 0.2, 1);
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}
.nav-mobile-drawer.is-open {
  transform: translateX(0);
}

.nav-mobile-drawer-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 0.5rem;
}
.nav-mobile-drawer-title {
  font-family: var(--font-display);
  font-size: 0.95rem;
  letter-spacing: 0.08em;
  color: rgba(27, 58, 75, 0.82);
}
.nav-mobile-close {
  width: 32px;
  height: 32px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: 1px solid rgba(27, 58, 75, 0.2);
  border-radius: 999px;
  background: transparent;
  color: rgba(27, 58, 75, 0.9);
  font-size: 1.1rem;
  line-height: 1;
  cursor: pointer;
  transition: background 0.2s ease, border-color 0.2s ease;
}
.nav-mobile-close:hover,
.nav-mobile-close:focus-visible {
  background: rgba(91, 164, 230, 0.15);
  border-color: rgba(91, 164, 230, 0.5);
  outline: none;
}

.nav-mobile-links {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}
.nav-mobile-link {
  display: flex;
  align-items: center;
  min-height: 44px;
  padding: 0 0.75rem;
  border-radius: 12px;
  font-family: var(--font-body);
  font-size: 0.95rem;
  font-weight: 500;
  letter-spacing: 0.04em;
  color: rgba(27, 58, 75, 0.88);
  text-decoration: none;
  transition:
    background 0.18s ease,
    color 0.18s ease,
    transform 0.18s ease;
}
.nav-mobile-link:hover,
.nav-mobile-link:focus-visible {
  background: rgba(91, 164, 230, 0.12);
  color: rgba(91, 164, 230, 1);
  outline: none;
}
.nav-mobile-link:active {
  transform: scale(0.98);
}
</style>