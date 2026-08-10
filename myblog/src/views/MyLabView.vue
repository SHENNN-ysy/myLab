<template>
  <main class="mylab">
    <!-- ============ Hero：标题 + 记录总数 ============ -->
    <section class="mylab-hero" aria-label="MyLab 页头">
      <h1 class="mylab-title">MyLab</h1>
      <p class="mylab-count">
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
          <path
            d="M9.937 15.5A2 2 0 0 0 8.5 14.063l-6.135-1.582a.5.5 0 0 1 0-.962L8.5 9.936A2 2 0 0 0 9.937 8.5l1.582-6.135a.5.5 0 0 1 .963 0L14.063 8.5A2 2 0 0 0 15.5 9.937l6.135 1.581a.5.5 0 0 1 0 .964L15.5 14.063a2 2 0 0 0-1.437 1.437l-1.582 6.135a.5.5 0 0 1-.963 0z"
          />
          <path d="M20 3v4" />
          <path d="M22 5h-4" />
        </svg>
        总计 {{ labPosts.length }} 篇研究记录
      </p>
    </section>

    <!-- ============ 控制区：搜索 + 标签汇总 + 视图切换 ============ -->
    <section class="mylab-controls" aria-label="筛选与视图控制">
      <div class="mylab-search">
        <input
          v-model="keyword"
          type="text"
          placeholder="搜寻被封存的知识..."
          aria-label="搜索记录"
        />
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
          <path d="m21 21-4.34-4.34" />
          <circle cx="11" cy="11" r="8" />
        </svg>
      </div>

      <div class="mylab-filterbar">
        <div class="mylab-tags" role="group" aria-label="按标签筛选">
          <button
            type="button"
            class="mylab-tag-btn"
            :class="{ 'is-active': activeTag === null }"
            @click="activeTag = null"
          >
            全部档案
          </button>
          <button
            v-for="item in tagSummary"
            :key="item.tag"
            type="button"
            class="mylab-tag-btn"
            :class="{ 'is-active': activeTag === item.tag }"
            @click="activeTag = activeTag === item.tag ? null : item.tag"
          >
            {{ item.tag }}
            <span class="mylab-tag-count">{{ item.count }}</span>
          </button>
        </div>

        <div class="mylab-viewtoggle" role="group" aria-label="切换布局">
          <button
            type="button"
            :class="{ 'is-active': viewMode === 'chain' }"
            aria-label="中枢链路视图"
            @click="viewMode = 'chain'"
          >
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
              <path d="M21 12h-8" />
              <path d="M21 6H8" />
              <path d="M21 18h-8" />
              <path d="M3 6v4c0 1.1.9 2 2 2h3" />
              <path d="M3 10v6c0 1.1.9 2 2 2h3" />
            </svg>
            <span>中枢链路</span>
          </button>
          <button
            type="button"
            :class="{ 'is-active': viewMode === 'grid' }"
            aria-label="矩阵网格视图"
            @click="viewMode = 'grid'"
          >
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
              <rect width="7" height="7" x="3" y="3" rx="1" />
              <rect width="7" height="7" x="14" y="3" rx="1" />
              <rect width="7" height="7" x="14" y="14" rx="1" />
              <rect width="7" height="7" x="3" y="14" rx="1" />
            </svg>
            <span>矩阵网格</span>
          </button>
        </div>
      </div>
    </section>

    <!-- ============ 内容区 ============ -->
    <section class="mylab-content" aria-label="记录列表">
      <!-- 中枢链路：中轴时间线，卡片左右交替 -->
      <div v-if="viewMode === 'chain'" key="chain" class="lab-timeline">
        <div
          v-for="(post, index) in filteredPosts"
          :key="post.id"
          class="lab-tl-item"
          :class="{ 'is-right': index % 2 === 1 }"
          :style="{ '--i': index }"
        >
          <span class="lab-tl-node" aria-hidden="true" />
          <div class="lab-tl-card">
            <LabCard :post="post" />
          </div>
        </div>
      </div>

      <!-- 矩阵网格：均分卡片栅格 -->
      <div v-else key="grid" class="lab-grid">
        <div
          v-for="(post, index) in filteredPosts"
          :key="post.id"
          class="lab-grid-cell"
          :style="{ '--i': index }"
        >
          <LabCard :post="post" />
        </div>
      </div>

      <p v-if="filteredPosts.length === 0" class="mylab-empty">
        没有找到匹配的记录，换个关键词或标签试试。
      </p>
    </section>
  </main>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import LabCard from '../components/LabCard.vue'
import { useLabPosts } from '../composables/useLabPosts'

const { content, labPosts } = useLabPosts()

/* ============ 筛选状态 ============ */
const keyword = ref('')
const activeTag = ref<string | null>(null)
const viewMode = ref<'chain' | 'grid'>('chain')

/* 后台已配置标签时按后台顺序展示，并遵循启停状态；静态兜底仍按出现次数汇总。 */
const tagSummary = computed(() => {
  const counts = new Map<string, number>()
  for (const post of labPosts.value) {
    for (const tag of post.tags) {
      counts.set(tag, (counts.get(tag) ?? 0) + 1)
    }
  }
  const managedTags = content.value.mylab?.tags
  if (Array.isArray(managedTags) && managedTags.length > 0 && (content.value.mylab?.cards?.length ?? 0) > 0) {
    return managedTags
      .filter(tag => tag.enabled !== false && Boolean(tag.name))
      .map(tag => ({ tag: tag.name as string, count: counts.get(tag.name as string) ?? 0 }))
  }
  return [...counts.entries()]
    .map(([tag, count]) => ({ tag, count }))
    .sort((a, b) => b.count - a.count)
})

/* 搜索（标题/摘要/标签）+ 标签筛选 */
const filteredPosts = computed(() => {
  const kw = keyword.value.trim().toLowerCase()
  return labPosts.value.filter((post) => {
    if (activeTag.value !== null && !post.tags.includes(activeTag.value)) {
      return false
    }
    if (!kw) return true
    const haystack = `${post.title} ${post.summary} ${post.tags.join(' ')}`.toLowerCase()
    return haystack.includes(kw)
  })
})
</script>

<style scoped>
.mylab {
  min-height: 100vh;
  /* 不设置背景色：body 已有 --bg，背景球层（z-index: -1）需透过 main 可见 */
}

/* ============ Hero ============ */
.mylab-hero {
  position: relative;
  /* 压到背景滚动球体（固定层 z-index: 1）之下，让球体柔光浮在蓝色面板上 */
  z-index: 0;
  overflow: hidden;
  padding: clamp(8rem, 18vh, 11rem) 1.5rem clamp(4.5rem, 10vh, 7rem);
  text-align: center;
  background:
    linear-gradient(180deg, #1B4965 0%, #2D6A8F 54%, #5BA4E6 82%, var(--bg) 100%);
}

.mylab-hero::before {
  content: '';
  position: absolute;
  inset: 0;
  pointer-events: none;
  background:
    radial-gradient(circle at 18% 82%, rgba(255, 255, 255, 0.18), transparent 34%),
    radial-gradient(circle at 86% 18%, rgba(91, 164, 230, 0.25), transparent 38%);
}

.mylab-title {
  position: relative;
  margin-bottom: 1rem;
  font-family: var(--font-display);
  font-size: clamp(2.8rem, 7vw, 4.6rem);
  font-weight: 900;
  letter-spacing: 0.02em;
  color: rgba(255, 255, 255, 0.97);
  text-shadow: 0 14px 40px rgba(0, 0, 0, 0.3);
}

.mylab-count {
  position: relative;
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.92rem;
  font-weight: 500;
  font-style: italic;
  letter-spacing: 0.06em;
  color: rgba(255, 255, 255, 0.85);
}

.mylab-count svg {
  width: 16px;
  height: 16px;
}

/* ============ 控制区 ============ */
.mylab-controls {
  width: min(var(--max-w), calc(100% - 2.5rem));
  margin: 0 auto;
  padding-top: 1.75rem;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 1.75rem;
}

.mylab-search {
  position: relative;
  width: min(100%, 32rem);
}

.mylab-search input {
  width: 100%;
  padding: 1rem 1.5rem 1rem 3.4rem;
  border-radius: 18px;
  border: 1px solid var(--border);
  background: rgba(255, 255, 255, 0.7);
  backdrop-filter: blur(14px);
  -webkit-backdrop-filter: blur(14px);
  box-shadow: 0 10px 30px rgba(27, 58, 75, 0.1);
  font-family: var(--font-body);
  font-size: 0.95rem;
  color: var(--ink);
  outline: none;
  transition:
    border-color 0.25s ease,
    box-shadow 0.25s ease;
}

.mylab-search input::placeholder {
  color: var(--ink-muted);
}

.mylab-search input:focus {
  border-color: rgba(91, 164, 230, 0.7);
  box-shadow:
    0 10px 30px rgba(27, 58, 75, 0.1),
    0 0 0 3px rgba(91, 164, 230, 0.25);
}

.mylab-search svg {
  position: absolute;
  left: 1.25rem;
  top: 50%;
  transform: translateY(-50%);
  width: 20px;
  height: 20px;
  color: var(--ink-muted);
  pointer-events: none;
  transition: color 0.25s ease;
}

.mylab-search:focus-within svg {
  color: var(--accent);
}

/* 标签汇总 + 视图切换：同一根玻璃条 */
.mylab-filterbar {
  width: 100%;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  padding: 0.9rem 1rem;
  border-radius: 24px;
  border: 1px solid var(--border);
  background: rgba(255, 255, 255, 0.55);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
}

.mylab-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
  flex: 1 1 auto;
}

.mylab-tag-btn {
  padding: 0.5rem 0.95rem;
  border: none;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.6);
  font-family: var(--font-body);
  font-size: 0.75rem;
  font-weight: 700;
  letter-spacing: 0.03em;
  color: var(--ink-light);
  cursor: pointer;
  transition:
    background 0.25s ease,
    color 0.25s ease,
    box-shadow 0.25s ease,
    transform 0.2s ease;
}

.mylab-tag-btn:hover {
  background: #fff;
  transform: translateY(-1px);
}

.mylab-tag-btn.is-active {
  background: var(--accent);
  color: #fff;
  box-shadow: 0 6px 16px rgba(91, 164, 230, 0.4);
}

.mylab-tag-count {
  margin-left: 0.3rem;
  opacity: 0.55;
  font-weight: 500;
}

/* 视图切换：分段控件 */
.mylab-viewtoggle {
  display: flex;
  gap: 0.25rem;
  padding: 0.25rem;
  border-radius: 16px;
  background: rgba(232, 244, 253, 0.9);
  box-shadow: inset 0 1px 4px rgba(27, 58, 75, 0.08);
  flex-shrink: 0;
}

.mylab-viewtoggle button {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem 1rem;
  border: none;
  border-radius: 12px;
  background: transparent;
  font-family: var(--font-body);
  font-size: 0.8rem;
  font-weight: 700;
  color: var(--ink-muted);
  cursor: pointer;
  transition:
    background 0.3s ease,
    color 0.3s ease,
    box-shadow 0.3s ease;
}

.mylab-viewtoggle button svg {
  width: 15px;
  height: 15px;
}

.mylab-viewtoggle button:hover {
  color: var(--ink-light);
}

.mylab-viewtoggle button.is-active {
  background: #fff;
  color: var(--accent-dark);
  box-shadow: 0 3px 10px rgba(27, 58, 75, 0.12);
}

/* ============ 内容区 ============ */
.mylab-content {
  width: min(var(--max-w), calc(100% - 2.5rem));
  margin: 0 auto;
  padding: 3rem 0 6rem;
}

/* ---- 中枢链路：中轴时间线 ---- */
.lab-timeline {
  position: relative;
  display: flex;
  flex-direction: column;
  gap: 2.75rem;
}

/* 中轴线 */
.lab-timeline::before {
  content: '';
  position: absolute;
  top: 0;
  bottom: 0;
  left: 50%;
  width: 2px;
  transform: translateX(-50%);
  background: linear-gradient(
    180deg,
    transparent 0%,
    rgba(91, 164, 230, 0.5) 6%,
    rgba(91, 164, 230, 0.5) 94%,
    transparent 100%
  );
}

.lab-tl-item {
  position: relative;
  width: 50%;
  padding-right: 3rem;
  animation: labRise 0.6s cubic-bezier(0.16, 1, 0.3, 1) both;
  animation-delay: calc(var(--i) * 70ms);
}

.lab-tl-item.is-right {
  align-self: flex-end;
  padding-right: 0;
  padding-left: 3rem;
}

/* 卡片宽度对齐矩阵网格（约 384px），并贴近中轴线 */
.lab-tl-card {
  max-width: 24rem;
  margin-left: auto;
}

.lab-tl-item.is-right .lab-tl-card {
  margin-left: 0;
  margin-right: auto;
}

/* 中轴节点 */
.lab-tl-node {
  position: absolute;
  top: 1.9rem;
  right: -0.42rem;
  width: 0.85rem;
  height: 0.85rem;
  border-radius: 50%;
  background: #fff;
  border: 3px solid var(--accent);
  box-shadow:
    0 0 0 4px rgba(91, 164, 230, 0.25),
    0 4px 12px rgba(27, 58, 75, 0.18);
  z-index: 2;
}

.lab-tl-item.is-right .lab-tl-node {
  right: auto;
  left: -0.42rem;
}

/* ---- 矩阵网格 ---- */
.lab-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 1.5rem;
}

.lab-grid-cell {
  animation: labRise 0.6s cubic-bezier(0.16, 1, 0.3, 1) both;
  animation-delay: calc(var(--i) * 50ms);
}

@keyframes labRise {
  from {
    opacity: 0;
    transform: translateY(28px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.mylab-empty {
  padding: 4rem 0;
  text-align: center;
  font-size: 0.95rem;
  color: var(--ink-muted);
}

/* ---- 移动端：时间线贴左 ---- */
@media (max-width: 767px) {
  .mylab-filterbar {
    justify-content: center;
  }

  .mylab-tags {
    justify-content: center;
  }

  .lab-timeline::before {
    left: 0.45rem;
    transform: none;
  }

  .lab-tl-item,
  .lab-tl-item.is-right {
    width: 100%;
    align-self: auto;
    padding-right: 0;
    padding-left: 2rem;
  }

  .lab-tl-node,
  .lab-tl-item.is-right .lab-tl-node {
    right: auto;
    left: 0.03rem;
  }

  /* 移动端与网格单列一致：卡片占满整行 */
  .lab-tl-card {
    max-width: none;
    margin-left: 0;
    margin-right: 0;
  }

  .lab-grid {
    grid-template-columns: 1fr;
  }
}

@media (prefers-reduced-motion: reduce) {
  .lab-tl-item,
  .lab-grid-cell {
    animation: none;
  }
}
</style>
