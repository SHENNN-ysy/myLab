<template>
  <section id="hobbies">
    <DinoRunner />
    <div class="container">
      <RevealOnScroll>
        <div class="section-header">
          <span class="section-num">04</span>
          <div class="section-title-group">
            <h2 class="section-title">我的<em>足迹</em></h2>
            <p class="section-desc">用脚步和镜头,在地图上留下这些城市的名字。每个地点背后,都有一次认真的抵达。</p>
          </div>
        </div>
      </RevealOnScroll>

      <div class="hobbies-layout">
        <RevealOnScroll :delay="1">
          <div class="hobbies-left">
            <div class="my-location-bar">
              <span class="my-location-label">我的位置</span>
              <span class="my-location-value">广州</span>
            </div>

            <div class="hobbies-intro">
              <p>点击列表中的任意一项,或在地图上点亮一个标记,可以查看我在那里的足迹。</p>
            </div>

            <div class="hobby-list">
              <button
                v-for="hobby in hobbies"
                :key="hobby.id"
                class="hobby-item"
                :class="{ 'is-active': activeHobby === hobby.id }"
                @mouseenter="setActiveHobby(hobby.id)"
                @focus="setActiveHobby(hobby.id)"
                @click="openHobbyModal(hobby.id)"
              >
                <div class="hobby-item-left">
                  <span class="hobby-bullet" />
                  <span class="hobby-name">{{ hobby.name }}</span>
                </div>
                <span class="hobby-tag">{{ hobby.tag }}</span>
              </button>
            </div>
          </div>
        </RevealOnScroll>

        <RevealOnScroll :delay="2">
          <div class="map-panel">
            <div class="map-stage" />

            <div class="map-markers">
              <div
                v-for="hobby in hobbies"
                :key="hobby.id"
                class="map-marker"
                :class="{ 'is-active': activeHobby === hobby.id, 'is-self': hobby.isSelf }"
                :style="{ left: hobby.position.x + '%', top: hobby.position.y + '%' }"
                @click="openHobbyModal(hobby.id)"
              >
                <div class="marker-tip" :class="{ 'is-flipped': hobby.position.y < 40 }">
                  <div class="marker-tip-title">{{ hobby.tip.title }}</div>
                  <div class="marker-tip-row">
                    <span>坐标</span>
                    <strong>{{ hobby.tip.coords }}</strong>
                  </div>
                  <div class="marker-tip-row">
                    <span>场景</span>
                    <strong>{{ hobby.tip.scene }}</strong>
                  </div>
                </div>
                <div class="marker-pulse" />
                <div class="marker-pin">
                  <span class="marker-dot">
                    <img v-if="hobby.isSelf" src="/assets/avatar.png" alt="" />
                  </span>
                  <span class="marker-stem" />
                </div>
              </div>
            </div>
          </div>
        </RevealOnScroll>
      </div>
    </div>

    <ProjectModal v-model="isModalOpen" direction="left">
      <div class="modal-body hobby-modal-body">
        <div class="modal-meta stagger-item-left" :style="{ animationDelay: staggerDelay(0) }">
          <span class="project-tag accent">{{ selectedHobbyDetail?.tag }}</span>
          <span class="project-year">{{ selectedHobbyDetail?.year }}</span>
        </div>
        <h2 class="modal-title stagger-item-left" :style="{ animationDelay: staggerDelay(1) }">{{ selectedHobbyDetail?.title }}</h2>
        <p class="modal-desc stagger-item-left" :style="{ animationDelay: staggerDelay(2) }">{{ selectedHobbyDetail?.desc }}</p>
        <p
          v-for="(paragraph, index) in selectedHobbyDetail?.paragraphs"
          :key="paragraph"
          class="stagger-item-left"
          :style="{ animationDelay: staggerDelay(index + 3) }"
        >
          {{ paragraph }}
        </p>
        <h4 class="stagger-item-left" :style="{ animationDelay: staggerDelay(6) }">常用器材 / 技术栈</h4>
        <div class="modal-tech stagger-item-left" :style="{ animationDelay: staggerDelay(7) }">
          <span v-for="tech in selectedHobbyDetail?.tech" :key="tech">{{ tech }}</span>
        </div>
        <div class="photo-wall-wrapper stagger-item-left" :style="{ animationDelay: staggerDelay(8) }">
          <h4>照片墙</h4>
          <div class="modal-photos">
            <div
              v-for="height in skeletonHeights"
              :key="height"
              class="modal-photo photo-skeleton"
              :style="{ height: height + 'px' }"
              aria-hidden="true"
            >
            </div>
          </div>
          <p class="modal-photos-hint">照片墙正在整理中 · 稍后补上这一组日常记录</p>
        </div>
        <button class="modal-cta stagger-item-left" :style="{ animationDelay: staggerDelay(9) }">{{ selectedHobbyDetail?.cta }} →</button>
      </div>
    </ProjectModal>
  </section>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { hobbies, type Hobby } from '@/data/projects'
import RevealOnScroll from './ui/RevealOnScroll.vue'
import ProjectModal from './ui/ProjectModal.vue'
import DinoRunner from './ui/DinoRunner.vue'

const activeHobby = ref('photo')
const selectedHobby = ref<Hobby | null>(hobbies[0] ?? null)
const isModalOpen = ref(false)
const skeletonHeights = [180, 240, 200, 280, 160]

const staggerDelay = (index: number) => `${0.08 + index * 0.07}s`

const setActiveHobby = (id: string) => {
  activeHobby.value = id
}

const hobbyDetails: Record<string, {
  tag: string
  year: string
  title: string
  desc: string
  paragraphs: string[]
  tech: string[]
  cta: string
}> = {
  photo: {
    tag: 'Film · 01',
    year: '胶片摄影',
    title: '胶片摄影 · 西安城墙',
    desc: '一台 Nikon FM2，几卷 Portra 400，和一段厚重的古城墙。',
    paragraphs: [
      '西安是我拍胶片最密集的城市。古城墙是天然的引导线，傍晚时分，金色的光沿着砖缝流下来。',
      '我喜欢在钟楼附近反复行走，让人流、车流和老建筑在取景框里形成自己的节奏。',
      '胶片摄影对我来说不是怀旧，而是一种慢下来的观察方式。'
    ],
    tech: ['Nikon FM2', 'Portra 400', 'Epson V600 扫描', 'Lightroom 调色'],
    cta: '查看作品集'
  },
  hike: {
    tag: 'Trail · 02',
    year: '徒步 / 登山',
    title: '徒步 · 昆明 · 高海拔',
    desc: '用脚步丈量高原，不是征服，是学会在稀薄空气里找到自己的节奏。',
    paragraphs: [
      '昆明周边的山路让我重新理解了“距离”这件事：地图上的短线，走起来常常是完整的一天。',
      '我喜欢徒步里那种简单的判断：补水、节奏、天气、脚下的路，每一件都真实具体。',
      '最美的风景往往不在终点，而在“再坚持一下”之后的转角。'
    ],
    tech: ['Salomon X Ultra 4', 'Osprey 背包', 'Garmin Fenix 7', '登山杖'],
    cta: '查看路线笔记'
  },
  coffee: {
    tag: 'Coffee · 03',
    year: '精品咖啡',
    title: '精品咖啡 · 上海武康路',
    desc: '从豆子到杯子，一杯咖啡是一段小型的时间旅行。',
    paragraphs: [
      '武康路是我在上海很喜欢的一段路。梧桐树影把阳光切成碎片，几家小店藏在老房子里。',
      '咖啡对我来说是一种准时开始工作的仪式，不是醒神，而是给一天一个锚点。',
      '我更在意一杯咖啡背后的风味描述、产地故事，以及它被认真对待的方式。'
    ],
    tech: ['Kalita Wave 185', 'Fellow Stagg EKG', '手冲壶', '风味记录本'],
    cta: '查看豆单笔记'
  },
  travel: {
    tag: 'Travel · 04',
    year: '城市漫游',
    title: '城市漫游 · 广州西关',
    desc: '不急着去景点，只在陌生城市的街区里游荡几个小时。',
    paragraphs: [
      '西关是广州老城里很迷人的一片：骑楼街、麻石巷、满洲窗，还有街坊聊天的声音。',
      '我喜欢在这样的地方慢慢走，听街边的生活声，闻别人家的饭菜香。',
      '城市漫游训练我对偶然的开放度：走错路，才更容易遇到没有被攻略写过的惊喜。'
    ],
    tech: ['纸质地图', '一双合脚的鞋', '相机', '空白笔记本'],
    cta: '查看旅行清单'
  },
  music: {
    tag: 'Music · 05',
    year: '黑胶与合成器',
    title: '黑胶与合成器 · 深圳 OCT',
    desc: '一种回放时间，一种创造时间，它们都让我暂时离开屏幕。',
    paragraphs: [
      '深圳的创意园区里有几家独立唱片店，是我固定会去的地方。',
      '合成器是近几年新开的坑。把一个 pad 音色调出层次，本身就是一次小创作。',
      '音乐对我而言是不被语言打扰的时间。项目做累了，切到 DAW 里乱按二十分钟，也是一种恢复。'
    ],
    tech: ['Audio-Technica', 'Korg Minilogue XD', 'Ableton Live', 'KRK Rokit'],
    cta: '查看常听清单'
  },
  read: {
    tag: 'Reading · 06',
    year: '独立书店',
    title: '独立书店 · 北京',
    desc: '认识一座城市，最慢也最可靠的方式，是在它的书店里坐一个下午。',
    paragraphs: [
      '北京有几条书店密度很高的街区，我喜欢把它们当作城市里的临时工作台。',
      '我常常在独立书店里不急着买东西，只是翻完一本诗集，再翻完一本地理散文。',
      '比起连锁书店，独立书店更像私人策展，选品本身就是一种表达。'
    ],
    tech: ['纸质笔记本', 'Moleskine 日程本', 'Kindle Oasis', '铅笔'],
    cta: '查看书单'
  }
}

const selectedHobbyDetail = computed(() => {
  const hobby = selectedHobby.value
  if (!hobby) return null
  const cleanDetail = hobbyDetails[hobby.id]
  if (cleanDetail) return cleanDetail

  return {
    tag: hobby.isSelf ? 'Travel · 04' : `${hobby.name} · ${hobby.id}`,
    year: hobby.tip.scene,
    title: hobby.tip.title,
    desc: hobby.tip.coords,
    paragraphs: [
      hobby.tip.scene,
      '这里记录的是一次认真抵达：用脚步、镜头和时间，把城市里的细节慢慢收进自己的地图。',
      '这些地点不是简单的坐标，而是我和不同生活方式短暂相遇的切片。'
    ],
    tech: ['纸质地图', '相机', '步行路线', hobby.name],
    cta: hobby.tag
  }
})

const openHobbyModal = (id: string) => {
  setActiveHobby(id)
  selectedHobby.value = hobbies.find((hobby) => hobby.id === id) ?? null
  isModalOpen.value = true
}
</script>

<style scoped>
#hobbies {
  padding: 100px 0;
  position: relative;
}

/* 区块背景压到背景球（z-index: -1）之下：球体浮在背景与小恐龙条带之上、内容之下 */
#hobbies::before {
  content: '';
  position: absolute;
  inset: 0;
  z-index: -2;
  background: linear-gradient(180deg, var(--bg) 0%, var(--bg-alt) 100%);
  pointer-events: none;
}

.container {
  max-width: var(--max-w);
  margin: 0 auto;
  padding: 0 3rem;
  /* 内容层叠在 DinoRunner 背景动画之上 */
  position: relative;
  z-index: 1;
}

.section-header {
  display: grid;
  grid-template-columns: auto 1fr;
  gap: 2rem;
  align-items: start;
  margin-bottom: 4rem;
}

.section-num {
  font-family: var(--font-mono);
  font-size: 0.65rem;
  letter-spacing: 0.15em;
  color: var(--ink-muted);
  padding-top: 0.5rem;
}

.section-title {
  font-family: var(--font-display);
  font-size: clamp(2.5rem, 5vw, 4rem);
  font-weight: 900;
  line-height: 0.95;
  letter-spacing: -0.03em;
  color: var(--ink);
  margin-bottom: 1rem;
}

.section-title em {
  font-style: italic;
  color: #FF6B6B;
}

.section-desc {
  font-size: 0.95rem;
  color: var(--ink-light);
  max-width: 760px;
  font-weight: 300;
  line-height: 1.8;
  white-space: nowrap;
}

.hobbies-layout {
  display: grid;
  grid-template-columns: 0.6fr 1.4fr;
  gap: 3rem;
  align-items: start;
}

.hobbies-left {
  display: flex;
  flex-direction: column;
}

.my-location-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  padding: 0.85rem 1rem;
  background: var(--bg-card);
  border: 1px solid rgba(255, 107, 107, 0.15);
  border-left: 3px solid #FF6B6B;
  margin-bottom: 1.6rem;
}

.my-location-label {
  display: inline-flex;
  align-items: center;
  gap: 0.55rem;
  font-family: var(--font-display);
  font-size: 0.95rem;
  font-weight: 700;
  color: var(--ink);
  letter-spacing: 0;
}

.my-location-label::before {
  content: '';
  width: 1.35rem;
  height: 1px;
  background: #FF6B6B;
  flex-shrink: 0;
}

.my-location-value {
  font-family: var(--font-display);
  font-size: 1.05rem;
  font-weight: 700;
  color: #FF6B6B;
  letter-spacing: 0.05em;
}

.hobbies-intro {
  font-family: var(--font-display);
  font-size: 1.08rem;
  font-weight: 400;
  color: var(--ink);
  line-height: 1.9;
  font-style: italic;
  margin-bottom: 1.6rem;
}

.hobby-list {
  display: grid;
  gap: 0.6rem;
}

.hobby-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  padding: 0.85rem 1rem;
  background: var(--bg-card);
  border: 1px solid rgba(91, 164, 230, 0.12);
  border-left: 3px solid transparent;
  cursor: pointer;
  transition: border-color 0.25s, transform 0.25s, background 0.25s;
  font: inherit;
  color: inherit;
  text-align: left;
}

.hobby-item:hover {
  border-left-color: #FF6B6B;
  transform: translateX(4px);
}

.hobby-item.is-active {
  border-left-color: #FF6B6B;
  background: #fff;
  box-shadow: 0 6px 20px rgba(255, 107, 107, 0.06);
}

.hobby-item-left {
  display: flex;
  align-items: center;
  gap: 0.9rem;
  min-width: 0;
}

.hobby-bullet {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #FF6B6B;
  flex-shrink: 0;
  box-shadow: 0 0 0 4px rgba(255, 107, 107, 0.2);
}

.hobby-item.is-active .hobby-bullet {
  box-shadow: 0 0 0 6px rgba(255, 107, 107, 0.2), 0 0 14px #FF6B6B;
}

.hobby-name {
  font-family: var(--font-display);
  font-size: 1.05rem;
  font-weight: 700;
  color: var(--ink);
  letter-spacing: -0.01em;
}

.hobby-tag {
  font-family: var(--font-mono);
  font-size: 0.62rem;
  letter-spacing: 0.1em;
  text-transform: uppercase;
  color: #FF6B6B;
  flex-shrink: 0;
}

/* Map Panel */
.map-panel {
  position: relative;
  background: linear-gradient(180deg, rgba(91, 164, 230, 0.08) 0%, rgba(232, 244, 253, 0.5) 100%);
  border: 1px solid rgba(91, 164, 230, 0.2);
  border-radius: 16px;
  overflow: hidden;
  aspect-ratio: 1029 / 823;
  max-width: 100%;
  box-shadow: 0 8px 32px rgba(27, 58, 75, 0.08);
  isolation: isolate;
}

.map-stage {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  background-color: #E8F4FD;
  background-image: url('/assets/china-map.jpeg');
  background-size: contain;
  background-position: center center;
  background-repeat: no-repeat;
  border-radius: 16px;
}

.map-markers {
  position: absolute;
  inset: 0;
}

.map-marker {
  position: absolute;
  transform: translate(-50%, -100%);
  cursor: pointer;
  z-index: 3;
  transition: transform 0.25s cubic-bezier(0.16,1,0.3,1), z-index 0s;
}

.map-marker:hover,
.map-marker.is-active {
  z-index: 6;
}

.marker-pin {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  transform: translateY(0);
  transition: transform 0.3s cubic-bezier(0.16,1,0.3,1);
}

.map-marker:hover .marker-pin,
.map-marker.is-active .marker-pin {
  transform: translateY(-4px);
}

.marker-dot {
  width: 14px;
  height: 14px;
  border-radius: 50%;
  background: #FF6B6B;
  border: 2.5px solid var(--bg-card);
  box-shadow: 0 0 0 1.5px #FF6B6B, 0 3px 12px rgba(255, 107, 107, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
}

.map-marker.is-active .marker-dot {
  width: 18px;
  height: 18px;
  background: #FF6B6B;
  border-color: var(--bg-card);
  box-shadow: 0 0 0 2px #FF6B6B, 0 0 0 7px rgba(255, 107, 107, 0.2), 0 6px 18px rgba(255, 107, 107, 0.6);
}

.map-marker.is-self .marker-dot {
  background: transparent;
  width: 84px;
  height: 84px;
  box-shadow: 0 0 0 3px #FF6B6B, 0 4px 16px rgba(255, 107, 107, 0.5);
  overflow: hidden;
}

.map-marker.is-self .marker-dot img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 50%;
  display: block;
}

.map-marker.is-self.is-active .marker-dot {
  width: 104px;
  height: 104px;
  box-shadow: 0 0 0 4px #FF6B6B, 0 0 0 12px rgba(255, 107, 107, 0.2), 0 8px 24px rgba(255, 107, 107, 0.55);
}

.map-marker.is-self.is-active .marker-dot {
  width: 104px;
  height: 104px;
  box-shadow: 0 0 0 4px #FF6B6B, 0 0 0 12px rgba(255, 107, 107, 0.2), 0 8px 24px rgba(255, 107, 107, 0.55);
}

.marker-stem {
  position: absolute;
  bottom: -6px;
  left: 50%;
  width: 1.5px;
  height: 6px;
  background: #FF6B6B;
  transform: translateX(-50%);
}

.marker-pulse {
  position: absolute;
  bottom: -1px;
  left: 50%;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: rgba(255, 107, 107, 0.35);
  transform: translate(-50%, 0);
  animation: markerPulse 2.2s ease-out infinite;
  pointer-events: none;
}

.map-marker.is-self .marker-pulse {
  width: 120px;
  height: 120px;
  bottom: -12px;
  background: transparent;
  border: 3px solid rgba(255, 107, 107, 0.5);
  animation: markerPulseAvatar 2.4s ease-out infinite;
}

@keyframes markerPulse {
  0% { transform: translate(-50%, 0) scale(0.5); opacity: 0.9; }
  100% { transform: translate(-50%, 0) scale(2.5); opacity: 0; }
}

@keyframes markerPulseAvatar {
  0% { transform: translate(-50%, 0) scale(0.35); opacity: 0.85; }
  100% { transform: translate(-50%, 0) scale(1.6); opacity: 0; }
}

.marker-tip {
  position: absolute;
  bottom: calc(100% + 12px);
  left: 50%;
  transform: translateX(-50%) translateY(4px);
  min-width: 190px;
  max-width: 240px;
  padding: 0.7rem 0.85rem;
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 10px;
  box-shadow: 0 10px 28px rgba(20,18,16,0.12);
  opacity: 0;
  pointer-events: none;
  transition: opacity 0.2s, transform 0.2s;
  white-space: nowrap;
}

.marker-tip::after {
  content: '';
  position: absolute;
  top: 100%;
  left: 50%;
  transform: translateX(-50%);
  border: 6px solid transparent;
  border-top-color: var(--bg-card);
}

.marker-tip.is-flipped {
  bottom: auto;
  top: calc(100% + 12px);
  transform: translateX(-50%) translateY(-4px);
}

.marker-tip.is-flipped::after {
  top: auto;
  bottom: 100%;
  border-top-color: transparent;
  border-bottom-color: var(--bg-card);
}

.map-marker:hover .marker-tip.is-flipped,
.map-marker.is-active .marker-tip.is-flipped {
  transform: translateX(-50%) translateY(0);
}

.map-marker:hover .marker-tip,
.map-marker.is-active .marker-tip {
  opacity: 1;
  transform: translateX(-50%) translateY(0);
}

.marker-tip-title {
  font-family: var(--font-display);
  font-size: 0.92rem;
  font-weight: 700;
  color: var(--ink);
  margin-bottom: 0.3rem;
  letter-spacing: -0.01em;
  text-align: center;
}

.marker-tip-row {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  font-family: var(--font-mono);
  font-size: 0.62rem;
  color: var(--ink-muted);
  letter-spacing: 0.05em;
}

.marker-tip-row strong {
  color: #FF6B6B;
  font-weight: 500;
}

/* Hobbies modal body */
.hobby-modal-body .photo-wall-wrapper h4 {
  font-family: var(--font-mono);
  font-size: 0.72rem;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: var(--ink);
  margin: 1.6rem 0 0.8rem;
}

@media (max-width: 900px) {
  .section-desc {
    max-width: 560px;
    white-space: normal;
  }

  .hobbies-layout {
    grid-template-columns: 1fr;
  }
}
</style>
