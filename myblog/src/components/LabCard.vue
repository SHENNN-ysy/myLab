<template>
  <!-- myLab 记录卡片：中枢链路 / 矩阵网格两种视图共用，点击进入详情页 -->
  <article
    :id="`lab-post-${post.id}`"
    class="lab-card"
    role="button"
    tabindex="0"
    @click="selectPost"
    @keydown.enter.prevent="selectPost"
    @keydown.space.prevent="selectPost"
  >
    <!-- 头图：加载完成前 / 未配图时显示骨架占位 -->
    <div class="lab-card-media" :class="{ 'is-loaded': imageLoaded }">
      <img
        v-if="post.image"
        :src="post.image"
        :alt="post.title"
        loading="lazy"
        decoding="async"
        @load="imageLoaded = true"
      />
    </div>
    <div class="lab-card-body">
      <div class="lab-card-date">
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
        <time :datetime="post.date">{{ post.date }}</time>
      </div>
      <h3 class="lab-card-title">{{ post.title }}</h3>
      <div class="lab-card-tags">
        <span v-for="tag in displayedTags" :key="tag" class="lab-card-tag">#{{ tag }}</span>
      </div>
      <p class="lab-card-summary">{{ post.summary }}</p>
      <div class="lab-card-engagement" aria-label="内容统计">
        <span title="浏览量">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
            <path d="M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7S2 12 2 12Z" />
            <circle cx="12" cy="12" r="3" />
          </svg>
          {{ formatNumber(engagement.view_count) }}
        </span>
        <span title="点赞数">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
            <path d="M20.8 4.6a5.5 5.5 0 0 0-7.8 0L12 5.6l-1-1a5.5 5.5 0 0 0-7.8 7.8l1 1L12 21l7.8-7.6 1-1a5.5 5.5 0 0 0 0-7.8Z" />
          </svg>
          {{ formatNumber(engagement.like_count) }}
        </span>
      </div>
    </div>
  </article>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import type { LabPost } from '../data/labPosts'
import { queueEngagement, useEngagement } from '../composables/useEngagement'

const props = withDefaults(defineProps<{
  post: LabPost
  navigate?: boolean
  tagLimit?: number
}>(), {
  navigate: true,
  tagLimit: Number.POSITIVE_INFINITY,
})

const emit = defineEmits<{
  select: [post: LabPost]
}>()

const router = useRouter()

const imageLoaded = ref(false)
const displayedTags = computed(() => props.post.tags.slice(0, props.tagLimit))
const { engagement } = useEngagement(() => props.post.id)
const formatNumber = (value: number) => new Intl.NumberFormat('zh-CN').format(value)

watch(() => props.post.id, queueEngagement, { immediate: true })

const selectPost = () => {
  if (props.navigate) {
    router.push(`/mylab/post/${props.post.id}`)
    return
  }
  emit('select', props.post)
}
</script>

<style scoped>
.lab-card {
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  border: 1px solid var(--border);
  border-radius: 20px;
  box-shadow: 0 4px 18px rgba(27, 58, 75, 0.06);
  cursor: pointer;
  transition:
    transform 0.3s cubic-bezier(0.16, 1, 0.3, 1),
    box-shadow 0.3s ease,
    border-color 0.3s ease;
}

.lab-card:hover {
  transform: translateY(-5px);
  border-color: rgba(91, 164, 230, 0.55);
  box-shadow:
    0 16px 36px rgba(27, 58, 75, 0.12),
    0 4px 12px rgba(91, 164, 230, 0.12);
}

/* ---- 头图：16:10，骨架占位 + 加载后淡入 ---- */
.lab-card-media {
  position: relative;
  aspect-ratio: 8 / 5;
  overflow: hidden;
  background: linear-gradient(135deg, var(--bg-alt) 0%, rgba(91, 164, 230, 0.16) 100%);
}

/* 骨架微光扫过动画：图未加载 / 未配图时持续显示 */
.lab-card-media::before {
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
  animation: labShimmer 1.8s ease-in-out infinite;
}

@keyframes labShimmer {
  to {
    transform: translateX(100%);
  }
}

.lab-card-media img {
  position: relative;
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
  opacity: 0;
  transform: scale(1.04);
  transition:
    opacity 0.5s ease,
    transform 0.6s cubic-bezier(0.16, 1, 0.3, 1);
  z-index: 1;
}

.lab-card-media.is-loaded img {
  opacity: 1;
  transform: scale(1);
}

/* 图加载完成后停掉骨架扫光，避免在图上闪 */
.lab-card-media.is-loaded::before {
  animation: none;
  opacity: 0;
}

.lab-card-body {
  display: flex;
  flex-direction: column;
  flex: 1;
  padding: 1.35rem 1.6rem 1.6rem;
}

.lab-card-date {
  display: flex;
  align-items: center;
  gap: 0.45rem;
  margin-bottom: 0.7rem;
  font-family: var(--font-mono);
  font-size: 0.72rem;
  font-weight: 500;
  letter-spacing: 0.05em;
  color: var(--ink-muted);
}

.lab-card-date svg {
  width: 14px;
  height: 14px;
  color: var(--accent);
}

.lab-card-title {
  margin-bottom: 0.8rem;
  font-family: var(--font-body);
  font-size: 1.05rem;
  font-weight: 700;
  line-height: 1.45;
  color: var(--ink);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  transition: color 0.25s ease;
}

.lab-card:hover .lab-card-title {
  color: var(--accent-dark);
}

.lab-card-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 0.45rem;
  margin-bottom: 0.85rem;
}

.lab-card-tag {
  padding: 0.15rem 0.55rem;
  border-radius: 8px;
  font-size: 0.65rem;
  font-weight: 700;
  letter-spacing: 0.03em;
  color: var(--accent-dark);
  background: var(--accent-light);
  border: 1px solid rgba(91, 164, 230, 0.18);
}

.lab-card-summary {
  font-size: 0.8rem;
  font-weight: 400;
  line-height: 1.7;
  color: var(--ink-light);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.lab-card-engagement {
  display: flex;
  align-items: center;
  gap: 1.1rem;
  margin-top: auto;
  padding-top: 1rem;
  color: var(--ink-muted);
  font-family: var(--font-mono);
  font-size: 0.7rem;
}

.lab-card-engagement span { display: inline-flex; align-items: center; gap: 0.35rem; }
.lab-card-engagement svg { width: 15px; height: 15px; color: var(--accent); }

@media (prefers-reduced-motion: reduce) {
  .lab-card-media::before {
    animation: none;
  }
}
</style>
