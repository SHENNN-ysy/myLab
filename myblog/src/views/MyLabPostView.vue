<template>
  <main class="post-page">
    <div v-if="post" class="post-container">
      <!-- ============ 主栏：头图 + 标题 + 正文 ============ -->
      <article class="post-main">
        <div class="post-hero" :class="{ 'is-loaded': heroLoaded }">
          <img
            v-if="post.image"
            :src="post.image"
            :alt="post.title"
            decoding="async"
            @load="heroLoaded = true"
          />
        </div>

        <RouterLink class="post-back" to="/mylab">← 返回上一级</RouterLink>

        <h1 class="post-title">{{ post.title }}</h1>

        <div class="post-meta">
          <span class="post-date">
            <svg
              xmlns="http://www.w3.org/2000/svg"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
              stroke-linecap="round"
              stroke-linejoin="round"
              aria-hidden="true"
            >
              <path d="M8 2v4" />
              <path d="M16 2v4" />
              <rect width="18" height="18" x="3" y="4" rx="2" />
              <path d="M3 10h18" />
            </svg>
            写作时间: {{ post.date }}
          </span>
          <span v-for="tag in post.tags" :key="tag" class="post-tag"># {{ tag }}</span>
        </div>

        <p class="post-summary">{{ post.summary }}</p>

        <section
          v-for="(section, index) in post.sections"
          :id="`sec-${index}`"
          :key="section.heading"
          class="post-section"
        >
          <h2 class="post-heading">
            <span class="post-heading-num">{{ index + 1 }}.</span>
            {{ section.heading }}
          </h2>
          <p v-for="paragraph in section.paragraphs" :key="paragraph">{{ paragraph }}</p>
        </section>
      </article>

      <!-- ============ 右侧小面板：RECOMMENDED + Table of Contents ============ -->
      <aside class="post-aside">
        <div class="aside-panel">
          <h3 class="aside-label">Recommended</h3>
          <RouterLink
            v-for="item in recommended"
            :key="item.id"
            class="aside-rec-item"
            :to="`/mylab/post/${item.id}`"
          >
            <span class="aside-rec-title">{{ item.title }}</span>
            <time class="aside-rec-date" :datetime="item.date">{{ item.date }}</time>
          </RouterLink>
        </div>

        <div class="aside-panel">
          <h3 class="aside-label">Table of Contents</h3>
          <button
            v-for="(section, index) in post.sections"
            :key="section.heading"
            type="button"
            class="aside-toc-item"
            @click="scrollToSection(index)"
          >
            {{ index + 1 }}. {{ section.heading }}
          </button>
        </div>
      </aside>
    </div>

    <!-- ============ 记录不存在 ============ -->
    <div v-else class="post-missing">
      <p>没有找到这条记录。</p>
      <RouterLink class="post-back" to="/mylab">← 返回 MyLab</RouterLink>
    </div>
  </main>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { labPosts } from '../data/labPosts'

const route = useRoute()

const post = computed(() => labPosts.find((p) => p.id === route.params.id) ?? null)

/* 头图骨架：切换文章时重置加载状态 */
const heroLoaded = ref(false)
watch(
  () => route.params.id,
  () => {
    heroLoaded.value = false
  }
)

/* RECOMMENDED：同标签优先，其余按日期新到旧补足，取 3 条 */
const recommended = computed(() => {
  if (!post.value) return []
  const current = post.value
  const others = labPosts.filter((p) => p.id !== current.id)
  const shared = others.filter((p) => p.tags.some((t) => current.tags.includes(t)))
  const rest = others.filter((p) => !shared.includes(p))
  return [...shared, ...rest].slice(0, 3)
})

function scrollToSection(index: number) {
  document.getElementById(`sec-${index}`)?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}
</script>

<style scoped>
.post-page {
  min-height: 100vh;
  padding: clamp(6rem, 12vh, 8rem) 1.5rem 6rem;
}

.post-container {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 300px;
  gap: 2.5rem;
  max-width: var(--max-w);
  margin: 0 auto;
  align-items: start;
}

/* ============ 主栏 ============ */
.post-main {
  min-width: 0;
}

/* 头图：骨架扫光占位 + 加载后淡入（与 myLab 卡片一致） */
.post-hero {
  position: relative;
  aspect-ratio: 16 / 8;
  overflow: hidden;
  border-radius: 20px;
  background: linear-gradient(135deg, var(--bg-alt) 0%, rgba(91, 164, 230, 0.16) 100%);
}

.post-hero::before {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(
    100deg,
    transparent 20%,
    rgba(255, 255, 255, 0.55) 50%,
    transparent 80%
  );
  transform: translateX(-100%);
  animation: postShimmer 1.8s ease-in-out infinite;
}

@keyframes postShimmer {
  to {
    transform: translateX(100%);
  }
}

.post-hero img {
  position: relative;
  z-index: 1;
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
  opacity: 0;
  transition: opacity 0.5s ease;
}

.post-hero.is-loaded img {
  opacity: 1;
}

.post-hero.is-loaded::before {
  animation: none;
  opacity: 0;
}

.post-back {
  display: inline-block;
  margin: 1.8rem 0 1.2rem;
  font-family: var(--font-mono);
  font-size: 0.8rem;
  letter-spacing: 0.08em;
  color: var(--ink-muted);
  text-decoration: none;
  transition: color 0.25s ease;
}

.post-back:hover {
  color: var(--accent-dark);
}

.post-title {
  font-family: var(--font-display);
  font-size: clamp(1.9rem, 4vw, 2.8rem);
  font-weight: 900;
  line-height: 1.2;
  letter-spacing: -0.02em;
  color: var(--ink);
  margin-bottom: 1rem;
}

.post-meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.6rem 1rem;
  margin-bottom: 2rem;
}

.post-date {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  font-family: var(--font-mono);
  font-size: 0.78rem;
  color: var(--ink-muted);
}

.post-date svg {
  width: 14px;
  height: 14px;
  color: var(--accent);
}

.post-tag {
  font-size: 0.75rem;
  font-weight: 700;
  color: var(--accent-dark);
}

.post-summary {
  margin-bottom: 2.2rem;
  font-size: 1rem;
  font-weight: 300;
  line-height: 1.9;
  color: var(--ink-light);
}

.post-section {
  margin-bottom: 2.2rem;
}

.post-heading {
  margin-bottom: 0.9rem;
  font-size: 1.25rem;
  font-weight: 800;
  color: var(--ink);
}

.post-heading-num {
  color: var(--accent);
  margin-right: 0.3rem;
}

.post-section p {
  margin-bottom: 0.9rem;
  font-size: 0.95rem;
  font-weight: 300;
  line-height: 1.9;
  color: var(--ink-light);
}

/* ============ 右侧小面板 ============ */
.post-aside {
  position: sticky;
  top: 6rem;
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.aside-panel {
  padding: 1.3rem 1.4rem;
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  border: 1px solid var(--border);
  border-radius: 20px;
  box-shadow: 0 4px 18px rgba(27, 58, 75, 0.06);
}

.aside-label {
  margin-bottom: 0.9rem;
  padding-left: 0.6rem;
  border-left: 3px solid var(--accent);
  font-family: var(--font-mono);
  font-size: 0.78rem;
  font-weight: 700;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  color: var(--ink);
}

.aside-rec-item {
  display: block;
  padding: 0.55rem 0;
  text-decoration: none;
  border-bottom: 1px dashed var(--border);
}

.aside-rec-item:last-child {
  border-bottom: none;
}

.aside-rec-title {
  display: block;
  font-size: 0.85rem;
  font-weight: 700;
  line-height: 1.5;
  color: var(--ink);
  transition: color 0.25s ease;
}

.aside-rec-item:hover .aside-rec-title {
  color: var(--accent-dark);
}

.aside-rec-date {
  display: block;
  margin-top: 0.2rem;
  font-family: var(--font-mono);
  font-size: 0.68rem;
  color: var(--ink-muted);
}

.aside-toc-item {
  display: block;
  width: 100%;
  padding: 0.4rem 0;
  border: none;
  background: none;
  text-align: left;
  font-size: 0.85rem;
  line-height: 1.5;
  color: var(--ink-light);
  cursor: pointer;
  transition: color 0.25s ease;
}

.aside-toc-item:hover {
  color: var(--accent-dark);
}

/* ============ 记录不存在 ============ */
.post-missing {
  padding: 8rem 0;
  text-align: center;
  color: var(--ink-muted);
}

/* ============ 响应式 ============ */
@media (max-width: 900px) {
  .post-container {
    grid-template-columns: 1fr;
  }

  .post-aside {
    position: static;
  }
}

@media (prefers-reduced-motion: reduce) {
  .post-hero::before {
    animation: none;
  }
}
</style>
