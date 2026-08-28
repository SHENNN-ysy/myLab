<template>
  <main class="post-page">
    <div
      v-if="post"
      class="post-container"
    >
      <!-- ============ 主栏：头图 + 标题 + 正文 ============ -->
      <article class="post-main">
        <div
          class="post-hero"
          :class="{ 'is-loaded': heroLoaded || !detailHero }"
        >
          <img
            v-if="detailHero"
            :src="detailHero"
            :alt="post.title"
            decoding="async"
            @load="heroLoaded = true"
          >
        </div>

        <h1 class="post-title">
          {{ post.title }}
        </h1>

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
              <rect
                width="18"
                height="18"
                x="3"
                y="4"
                rx="2"
              />
              <path d="M3 10h18" />
            </svg>
            写作时间: {{ post.date }}
          </span>
          <span
            class="post-date"
            title="浏览量"
          >
            <svg
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
              aria-hidden="true"
            >
              <path d="M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7S2 12 2 12Z" />
              <circle
                cx="12"
                cy="12"
                r="3"
              />
            </svg>
            {{ formatNumber(postEngagement.view_count) }} 次浏览
          </span>
          <span
            v-for="tag in post.tags"
            :key="tag"
            class="post-tag"
          ># {{ tag }}</span>
        </div>

        <p class="post-summary">
          {{ post.summary }}
        </p>

        <p
          v-if="markdownLoading"
          class="post-content-state"
        >
          正在加载正文…
        </p>
        <p
          v-else-if="markdownError && !post.sections.length"
          class="post-content-state is-error"
        >
          {{ markdownError }}
        </p>
        <div
          v-else-if="markdownHtml"
          class="markdown-body"
          v-html="markdownHtml"
        />

        <section
          v-for="(section, index) in post.sections"
          v-else
          :id="`sec-${index}`"
          :key="section.heading"
          class="post-section"
        >
          <h2 class="post-heading">
            <span class="post-heading-num">{{ index + 1 }}.</span>
            {{ section.heading }}
          </h2>
          <p
            v-for="paragraph in section.paragraphs"
            :key="paragraph"
          >
            {{ paragraph }}
          </p>
        </section>

        <div class="post-actions">
          <RouterLink
            class="post-back"
            to="/mylab"
          >
            ← 返回上一级
          </RouterLink>
          <button
            type="button"
            class="like-button"
            :class="{ 'is-liked': postEngagement.liked }"
            :aria-pressed="postEngagement.liked"
            :disabled="likePending"
            @click="toggleLike"
          >
            <svg
              viewBox="0 0 24 24"
              :fill="postEngagement.liked ? 'currentColor' : 'none'"
              stroke="currentColor"
              stroke-width="2"
              aria-hidden="true"
            >
              <path d="M20.8 4.6a5.5 5.5 0 0 0-7.8 0L12 5.6l-1-1a5.5 5.5 0 0 0-7.8 7.8l1 1L12 21l7.8-7.6 1-1a5.5 5.5 0 0 0 0-7.8Z" />
            </svg>
            {{ postEngagement.liked ? '已点赞' : '点赞' }}
            <strong>{{ formatNumber(postEngagement.like_count) }}</strong>
          </button>
          <span
            v-if="interactionError"
            class="engagement-error"
            role="status"
          >{{ interactionError }}</span>
        </div>
      </article>

      <!-- ============ 右侧小面板：RECOMMENDED + Table of Contents ============ -->
      <aside class="post-aside">
        <div class="aside-panel">
          <h3 class="aside-label">
            Recommended
          </h3>
          <RouterLink
            v-for="item in recommended"
            :key="item.id"
            class="aside-rec-item"
            :to="`/mylab/post/${item.id}`"
          >
            <span class="aside-rec-title">{{ item.title }}</span>
            <time
              class="aside-rec-date"
              :datetime="item.date"
            >{{ item.date }}</time>
          </RouterLink>
        </div>

        <div class="aside-panel">
          <h3 class="aside-label">
            Table of Contents
          </h3>
          <button
            v-for="item in tocItems"
            :key="item.id"
            type="button"
            class="aside-toc-item"
            :class="`toc-level-${item.level}`"
            @click="scrollToSection(item.id)"
          >
            {{ item.text }}
          </button>
        </div>
      </aside>
    </div>

    <!-- ============ 记录不存在 ============ -->
    <div
      v-else
      class="post-missing"
    >
      <p>没有找到这条记录。</p>
      <RouterLink
        class="post-back"
        to="/mylab"
      >
        ← 返回 MyLab
      </RouterLink>
    </div>
  </main>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useLabPosts } from '../composables/useLabPosts'
import { loadPublicMylabDetail } from '../composables/usePublicContent'
import { recordContentView, setContentLiked, useEngagement } from '../composables/useEngagement'
import { renderMarkdown, type MarkdownHeading } from '../utils/markdown'

const route = useRoute()
const { labPosts } = useLabPosts()

const post = computed(() => labPosts.value.find((p) => p.id === route.params.id) ?? null)
const detailHero = computed(() => post.value?.detailImage ?? post.value?.image)
const { engagement: postEngagement } = useEngagement(() => post.value?.id ?? '')
const likePending = ref(false)
const interactionError = ref('')
const numberFormatter = new Intl.NumberFormat('zh-CN')
const formatNumber = (value: number) => numberFormatter.format(value)
const markdownHtml = ref('')
const markdownHeadings = ref<MarkdownHeading[]>([])
const markdownLoading = ref(false)
const markdownError = ref('')

/* 头图骨架：切换文章时重置加载状态 */
const heroLoaded = ref(false)
watch(
  () => route.params.id,
  () => {
    heroLoaded.value = false
  }
)

watch(
  () => post.value?.id,
  async (postKey, _previousKey, onCleanup) => {
    interactionError.value = ''
    if (!postKey) return
    const controller = new AbortController()
    onCleanup(() => controller.abort())
    try {
      await recordContentView(postKey, controller.signal)
    } catch (error) {
      if ((error as Error).name !== 'AbortError') interactionError.value = '互动统计暂不可用'
    }
  },
  { immediate: true },
)

watch(
  () => post.value?.id,
  async (postKey, _previousKey, onCleanup) => {
    markdownHtml.value = ''
    markdownHeadings.value = []
    markdownError.value = ''
    if (!postKey) return

    const controller = new AbortController()
    onCleanup(() => controller.abort())
    markdownLoading.value = true
    try {
      const detail = await loadPublicMylabDetail(postKey, controller.signal)
      const markdown = detail.markdown_content || ''
      if (!markdown.trim()) throw new Error('正文为空')
      const rendered = renderMarkdown(markdown)
      markdownHtml.value = rendered.html
      markdownHeadings.value = rendered.headings
    } catch (error) {
      if ((error as Error).name !== 'AbortError') {
        markdownError.value = '暂时无法加载这篇文章的正文。'
      }
    } finally {
      if (!controller.signal.aborted) markdownLoading.value = false
    }
  },
  { immediate: true },
)

/* RECOMMENDED：同标签优先，其余按日期新到旧补足，取 3 条 */
const recommended = computed(() => {
  if (!post.value) return []
  const current = post.value
  const others = labPosts.value.filter((p) => p.id !== current.id)
  const shared = others.filter((p) => p.tags.some((t) => current.tags.includes(t)))
  const rest = others.filter((p) => !shared.includes(p))
  return [...shared, ...rest].slice(0, 3)
})

const tocItems = computed<MarkdownHeading[]>(() => {
  if (markdownHeadings.value.length) return markdownHeadings.value
  return (post.value?.sections ?? []).map((section, index) => ({
    id: `sec-${index}`,
    text: `${index + 1}. ${section.heading}`,
    level: 2,
  }))
})

function scrollToSection(id: string) {
  document.getElementById(id)?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

async function toggleLike() {
  const postKey = post.value?.id
  if (!postKey || likePending.value) return
  likePending.value = true
  interactionError.value = ''
  try {
    await setContentLiked(postKey, !postEngagement.value.liked)
  } catch {
    interactionError.value = '点赞失败，请稍后再试'
  } finally {
    likePending.value = false
  }
}
</script>

<style scoped>
.post-page {
  position: relative;
  min-height: 100vh;
  padding: clamp(6rem, 12vh, 8rem) 1.5rem 6rem;
}

/* 顶部蓝色渐变带：与 MyLab 列表页页头同款（含柔光），
   给透明态的白色导航提供深色衬底，避免滑到顶时导航隐入浅色背景 */
.post-page::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: clamp(11rem, 26vh, 17rem);
  pointer-events: none;
  background:
    radial-gradient(circle at 18% 82%, rgba(255, 255, 255, 0.18), transparent 34%),
    radial-gradient(circle at 86% 18%, rgba(91, 164, 230, 0.25), transparent 38%),
    linear-gradient(180deg, #1B4965 0%, #2D6A8F 54%, #5BA4E6 82%, var(--bg) 100%);
}

.post-container {
  /* 参与定位层叠，确保内容压在 ::before 渐变带之上 */
  position: relative;
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
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 2.5rem;
  padding: 0.65rem 1rem;
  border: 1px solid var(--border);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.8);
  font-family: var(--font-mono);
  font-size: 0.8rem;
  font-weight: 700;
  letter-spacing: 0.08em;
  color: var(--ink-light);
  text-decoration: none;
  transition: color 0.2s, border-color 0.2s, background 0.2s, transform 0.2s;
}

.post-back:hover {
  color: var(--accent-dark);
  border-color: var(--accent);
  background: var(--accent-light);
  transform: translateY(-1px);
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

.post-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 1rem;
  margin-top: 2.8rem;
  padding-top: 1.5rem;
  border-top: 1px solid var(--border);
}

.like-button {
  display: inline-flex;
  align-items: center;
  gap: 0.55rem;
  padding: 0.65rem 1rem;
  color: var(--ink-light);
  background: rgba(255, 255, 255, 0.8);
  border: 1px solid var(--border);
  border-radius: 999px;
  font: 700 0.78rem var(--font-body);
  cursor: pointer;
  transition: color 0.2s, border-color 0.2s, background 0.2s, transform 0.2s;
}

.like-button:hover:not(:disabled) { transform: translateY(-1px); border-color: #ef7f8d; color: #d84d61; }
.like-button.is-liked { color: #d84d61; border-color: rgba(216, 77, 97, 0.35); background: rgba(255, 235, 239, 0.9); }
.like-button:disabled { cursor: wait; opacity: 0.65; }
.like-button svg { width: 17px; height: 17px; }
.like-button strong { font-family: var(--font-mono); }
.engagement-error { flex-basis: 100%; color: #c55b5b; font-size: 0.75rem; text-align: right; }

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

.post-content-state {
  padding: 2rem 0;
  color: var(--ink-muted);
  font-size: 0.9rem;
}

.post-content-state.is-error {
  color: #c55b5b;
}

.markdown-body {
  color: var(--ink-light);
  font-size: 0.95rem;
  font-weight: 300;
  line-height: 1.9;
}

.markdown-body :deep(h1),
.markdown-body :deep(h2),
.markdown-body :deep(h3),
.markdown-body :deep(h4) {
  scroll-margin-top: 6rem;
  margin: 2.2rem 0 0.9rem;
  color: var(--ink);
  font-weight: 800;
  line-height: 1.35;
}

.markdown-body :deep(h1) { font-size: 1.65rem; }
.markdown-body :deep(h2) { font-size: 1.35rem; }
.markdown-body :deep(h3) { font-size: 1.15rem; }
.markdown-body :deep(p),
.markdown-body :deep(ul),
.markdown-body :deep(blockquote) { margin-bottom: 1rem; }
.markdown-body :deep(ul) { padding-left: 1.4rem; }
.markdown-body :deep(img) { display: block; max-width: 100%; margin: 1.5rem auto; border-radius: 14px; }
.markdown-body :deep(a) { color: var(--accent-dark); text-underline-offset: 0.2em; }
.markdown-body :deep(blockquote) { padding: 0.8rem 1rem; border-left: 3px solid var(--accent); background: var(--accent-light); }
.markdown-body :deep(code) { padding: 0.12rem 0.35rem; border-radius: 5px; background: var(--bg-alt); font-family: var(--font-mono); font-size: 0.88em; }
.markdown-body :deep(pre) { overflow-x: auto; margin: 1.4rem 0; padding: 1rem 1.2rem; border-radius: 12px; background: #183142; color: #e8f1f5; }
.markdown-body :deep(pre code) { padding: 0; background: transparent; color: inherit; }

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

.aside-toc-item.toc-level-3 { padding-left: 0.75rem; font-size: 0.8rem; }
.aside-toc-item.toc-level-4,
.aside-toc-item.toc-level-5,
.aside-toc-item.toc-level-6 { padding-left: 1.2rem; font-size: 0.76rem; }

/* ============ 记录不存在 ============ */
.post-missing {
  /* 参与定位层叠，确保内容压在 ::before 渐变带之上 */
  position: relative;
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
