<template>
  <section
    id="mylab-station"
    class="mylab-station"
    :class="{ 'no-anim': noAnim }"
  >
    <div class="container">
      <RevealOnScroll>
        <div class="section-header">
          <span class="section-num">07</span>
          <div class="section-title-group">
            <h2 class="section-title">
              Welcome to <em>MyLab</em>
            </h2>
            <p class="section-desc">
              这个小站也是我搭建的个人学习实践场景，希望以此能激励我不断前进
            </p>
          </div>
        </div>
      </RevealOnScroll>

      <RevealOnScroll :delay="1">
        <!-- 车站场景面板：放在与标题相同的网格列里，左缘与大标题对齐 -->
        <div class="panel-grid">
          <span aria-hidden="true" />
          <div class="scene-wrap">
            <div
              ref="sceneRef"
              class="scene"
            >
              <!-- 后墙 -->
              <div class="wall" />

              <!-- 站点卡片：站点列表排水平左侧，吊杆悬挂 -->
              <aside
                class="info-panel"
                aria-live="polite"
              >
                <div class="info-panel-head">
                  <span>STATION INFO</span>
                  <span>L1 · LINE</span>
                </div>
                <div class="info-panel-body">
                  <h3 class="info-title">
                    {{ current ? current.name : '—' }}
                  </h3>
                  <div class="info-date">
                    {{ current ? current.date : '—' }}
                  </div>
                  <div class="info-tags">
                    <span
                      v-for="tag in current ? current.tags : []"
                      :key="tag"
                      class="info-tag"
                    >#{{ tag }}</span>
                  </div>
                  <p class="info-summary">
                    {{ current ? current.summary : '—' }}
                  </p>
                  <div class="info-stops">
                    {{ selectedIndex >= 0 ? (selectedIndex + 1) + ' / ' + stations.length + ' STOPS · L1' : '— STOPS' }}
                  </div>
                </div>
              </aside>

              <!-- 悬挂线路牌（靠右） -->
              <div class="board-hanger">
                <div class="board">
                  <div class="board-head">
                    <span class="line-badge">L1</span>
                    <strong>MyLab 中央站</strong>
                    <span class="board-head-en">MYLAB CENTRAL</span>
                  </div>
                  <div class="track-zone">
                    <div class="track">
                      <div
                        v-for="(st, i) in stations"
                        :key="st.key"
                        class="station"
                        :class="{ 'is-active': i === selectedIndex }"
                      >
                        <button
                          type="button"
                          class="station-dot"
                          :aria-label="'选择站点 ' + st.name"
                          @click="selectStation(i)"
                        />
                        <button
                          type="button"
                          class="station-name"
                          :class="{ 'is-above': i % 2 === 1 }"
                          @click="selectStation(i)"
                        >
                          {{ st.name }}
                        </button>
                      </div>
                    </div>
                  </div>
                  <div class="board-actions">
                    <span class="board-actions-label">进入MyLab</span>
                    <button
                      type="button"
                      class="board-btn"
                      @click="goMyLab"
                    >
                      <span class="board-btn-text">查看更多</span>
                    </button>
                  </div>
                </div>
              </div>

              <!-- 列车：长度占满站台，两档车门与屏蔽门对齐，车尾在画面左缘之外 -->
              <div
                class="train"
                :class="{ arrived: trainArrived, departed: trainDeparted, 'doors-open': doorsOpen }"
              >
                <div class="train-band" />
                <div class="train-nose" />
                <div class="train-windshield" />
                <span class="train-headlight" />
                <svg
                  class="train-wave"
                  viewBox="0 0 1000 40"
                  preserveAspectRatio="none"
                  aria-hidden="true"
                >
                  <path
                    d="M0,20 L1000,20"
                    fill="none"
                    stroke="#FF6B6B"
                    stroke-width="7"
                    stroke-linecap="round"
                  />
                </svg>
                <span class="train-stripe-top" />
                <span class="train-skirt" />
                <span class="train-tail-window" />
                <span class="train-tail-light" />
                <div
                  class="train-door"
                  style="left: 34%;"
                >
                  <div class="train-marquee">
                    <span>{{ marqueeText }}</span>
                  </div>
                  <div class="train-door-leaf is-left" />
                  <div class="train-door-leaf is-right" />
                </div>
                <div
                  class="train-door"
                  style="left: 66.3%;"
                >
                  <div class="train-marquee">
                    <span>{{ marqueeText }}</span>
                  </div>
                  <div class="train-door-leaf is-left" />
                  <div class="train-door-leaf is-right" />
                </div>
              </div>

              <!-- 站台屏蔽门：固定玻璃 + 两档滑动自动门，底边直接压在站台地面上 -->
              <div
                class="psd"
                :class="{ open: psdOpen }"
              >
                <div class="psd-panel">
                  <div class="psd-glass" />
                  <div class="psd-louver" />
                </div>
                <div class="psd-door">
                  <span class="psd-light" />
                  <div class="psd-leaf is-left">
                    <div class="psd-glass" />
                    <div class="psd-louver" />
                  </div>
                  <div class="psd-leaf is-right">
                    <div class="psd-glass" />
                    <div class="psd-louver" />
                  </div>
                </div>
                <div class="psd-panel">
                  <div class="psd-glass" />
                  <div class="psd-louver" />
                </div>
                <div class="psd-door">
                  <span class="psd-light" />
                  <div class="psd-leaf is-left">
                    <div class="psd-glass" />
                    <div class="psd-louver" />
                  </div>
                  <div class="psd-leaf is-right">
                    <div class="psd-glass" />
                    <div class="psd-louver" />
                  </div>
                </div>
                <div class="psd-panel">
                  <div class="psd-glass" />
                  <div class="psd-louver" />
                </div>
              </div>

              <!-- 透视地面 -->
              <div class="floor">
                <span class="floor-text">MIND THE GAP · 请先下后上</span>
              </div>
            </div>
          </div>
        </div>
      </RevealOnScroll>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useLabPosts } from '@/composables/useLabPosts'
import RevealOnScroll from './ui/RevealOnScroll.vue'

/* 站点 = 一篇 MyLab 研究记录 */
interface Station {
  key: string
  name: string
  date: string
  tags: string[]
  summary: string
}

const router = useRouter()
const { labPosts } = useLabPosts()

const stations = ref<Station[]>([])
const selectedIndex = ref(-1)

/* 动画状态 */
const trainArrived = ref(false)
const trainDeparted = ref(false)
const doorsOpen = ref(false)
const psdOpen = ref(false)
const noAnim = ref(false)

const ARRIVE_MS = 2600   // 列车减速进站时长，与 CSS .train.arrived transition 对齐
const DEPART_MS = 1000   // 驶离时长，与 .train 基础 transition 对齐
let cycleTimers: number[] = []

const current = computed(() =>
  selectedIndex.value >= 0 ? stations.value[selectedIndex.value] : undefined,
)

/* 车门内跑马灯文案：中文 + 英文同在一条灯带上 */
const marqueeText = computed(() =>
  `下一站是：${current.value?.name ?? '—'} · Next Station: ${current.value?.name ?? '—'}`,
)

/* 后台卡片数据到达后映射为站点（至多 5 个，标题截断为站名） */
watch(labPosts, (posts) => {
  if (!posts.length) return
  stations.value = posts.slice(0, 5).map(p => ({
    key: p.id || p.title,
    name: (p.title || '未命名').slice(0, 12),
    date: p.date || '',
    tags: (p.tags || []).slice(0, 2),
    summary: p.summary || '',
  }))
  if (selectedIndex.value < 0 && stations.value.length > 0) {
    selectedIndex.value = 0
  } else if (selectedIndex.value >= stations.value.length) {
    selectedIndex.value = stations.value.length - 1
  }
}, { immediate: true })

function clearCycleTimers() {
  cycleTimers.forEach(clearTimeout)
  cycleTimers = []
}

/* 进站时序：列车缓慢停稳 → 屏蔽门开 → 车门开 */
function runArrivalSequence() {
  cycleTimers.push(window.setTimeout(() => {
    trainArrived.value = true
  }, 300))
  cycleTimers.push(window.setTimeout(() => {
    psdOpen.value = true
  }, 300 + ARRIVE_MS + 250))
  cycleTimers.push(window.setTimeout(() => {
    doorsOpen.value = true
  }, 300 + ARRIVE_MS + 1200))
}

/* 换站时序：关门 → 向前驶离 → 完全驶离 2s 后重新进站 */
function departAndReturn() {
  clearCycleTimers()
  doorsOpen.value = false
  psdOpen.value = false
  cycleTimers.push(window.setTimeout(() => {
    trainArrived.value = false
    trainDeparted.value = true
  }, 700))
  cycleTimers.push(window.setTimeout(() => {
    /* 瞬时回到左侧起点：先关过渡，待样式生效后再恢复并重新进站 */
    noAnim.value = true
    trainDeparted.value = false
    cycleTimers.push(window.setTimeout(() => {
      noAnim.value = false
      runArrivalSequence()
    }, 50))
  }, 700 + DEPART_MS + 2000))
}

function selectStation(i: number) {
  if (i === selectedIndex.value) return
  selectedIndex.value = i
  /* 列车已在站：换站触发「关门 → 驶离 → 重新进站」 */
  if (trainArrived.value || trainDeparted.value) departAndReturn()
}

function goMyLab() {
  router.push('/mylab')
}

/* 进站动画只在场景滚动到可视区时播放一次 */
const sceneRef = ref<HTMLElement | null>(null)
let sceneObserver: IntersectionObserver | null = null
let arrivalPlayed = false

function startArrival() {
  if (arrivalPlayed) return
  arrivalPlayed = true
  runArrivalSequence()
}

onMounted(() => {
  const el = sceneRef.value
  if (!el || typeof IntersectionObserver === 'undefined') {
    startArrival()
    return
  }
  sceneObserver = new IntersectionObserver((entries) => {
    if (entries.some(entry => entry.isIntersecting)) {
      startArrival()
      sceneObserver?.disconnect()
      sceneObserver = null
    }
  }, { threshold: 0.35 })
  sceneObserver.observe(el)
})

onBeforeUnmount(() => {
  sceneObserver?.disconnect()
  clearCycleTimers()
})
</script>

<style scoped>
.mylab-station {
  padding: 100px 0;
  position: relative;
  /* 站台警戒线黄色（项目全局变量中没有，本区块自定义） */
  --safety: #F5C518;
}

/* 背景与 06 区块（Vibe Coding）一致：--bg-alt 满铺，交界处无缝 */
.mylab-station::before {
  content: '';
  position: absolute;
  inset: 0;
  z-index: -2;
  background: var(--bg-alt);
  pointer-events: none;
}

.container {
  max-width: var(--max-w);
  margin: 0 auto;
  padding: 0 3rem;
  position: relative;
  z-index: 1;
}

.section-header {
  display: grid;
  grid-template-columns: auto 1fr;
  gap: 2rem;
  align-items: start;
  margin-bottom: 3rem;
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
}

/* ============ 车站场景：面板 85% 大小，左缘与大标题对齐 ============ */
/* 与区块头相同的网格列：空占位列对齐序号宽度，面板落在标题列 */
.panel-grid {
  display: grid;
  grid-template-columns: auto 1fr;
  gap: 2rem;
}

.scene-wrap {
  width: 85%;
  aspect-ratio: 16 / 10.5;
}

.scene {
  position: relative;
  /* 内部按原始尺寸布局，再整体缩到 85% */
  width: 117.647%;
  aspect-ratio: 16 / 10.5;
  transform: scale(0.85);
  transform-origin: top left;
  border: 3px solid var(--ink);
  border-radius: 18px;
  background: linear-gradient(180deg, #FDFEFF 0%, #F0F7FC 58%, #E4EFF7 100%);
  box-shadow: 14px 14px 0 rgba(27, 58, 75, 0.12);
  overflow: hidden;
  perspective: 1100px;
}

/* ---- 后墙：大瓷砖 ---- */
.wall {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 62%;
  background:
    repeating-linear-gradient(90deg, transparent 0 calc(100% / 9 - 2px), rgba(27, 58, 75, 0.22) calc(100% / 9 - 2px) calc(100% / 9)),
    repeating-linear-gradient(180deg, transparent 0 calc(100% / 4 - 2px), rgba(27, 58, 75, 0.22) calc(100% / 4 - 2px) calc(100% / 4)),
    linear-gradient(180deg, #FFFFFF 0%, #F4FAFD 100%);
}

/* ---- 悬挂线路牌：靠右悬挂，左侧留给站点卡片 ---- */
.board-hanger {
  position: absolute;
  top: 0;
  right: 2.5%;
  width: 71%;
  z-index: 10;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.board-hanger::before,
.board-hanger::after {
  content: '';
  position: absolute;
  top: 0;
  width: 4px;
  height: 42px;
  background: var(--ink);
}

.board-hanger::before { left: 12%; }
.board-hanger::after { right: 12%; }

.board {
  margin-top: 42px;
  width: 100%;
  min-height: clamp(120px, 11vw, 150px);
  background: #fff;
  border: 4px solid var(--ink);
  border-radius: 8px;
  box-shadow: 8px 8px 0 rgba(27, 58, 75, 0.15);
  display: flex;
  align-items: stretch;
  padding: 1.1% 1.4%;
  gap: 1.6%;
}

/* 牌头：L1 徽章在上，站名在下 */
.board-head {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  justify-content: center;
  gap: 3px;
  padding-right: 1.4%;
  border-right: 2.5px solid var(--ink);
  white-space: nowrap;
}

.line-badge {
  width: clamp(26px, 3vw, 38px);
  height: clamp(26px, 3vw, 38px);
  border-radius: 50%;
  background: var(--accent-dark);
  border: 3px solid var(--ink);
  display: grid;
  place-items: center;
  color: #FFFFFF;
  font-family: var(--font-mono);
  font-weight: 700;
  font-size: clamp(0.7rem, 0.95vw, 0.9rem);
  box-shadow: 0 4px 10px rgba(58, 139, 212, 0.4);
}

.board-head strong {
  font-size: clamp(0.85rem, 1.35vw, 1.15rem);
  font-weight: 700;
  color: var(--ink);
  line-height: 1.25;
}

.board-head-en {
  font-family: var(--font-mono);
  font-size: clamp(0.5rem, 0.7vw, 0.62rem);
  font-weight: 500;
  letter-spacing: 0.14em;
  color: var(--ink-muted);
}

/* 线路区：横条贯穿牌面 */
.track-zone {
  flex: 1;
  display: flex;
  align-items: flex-start;
  min-width: 0;
  padding: clamp(30px, 3vw, 42px) 0 0;
}

.track {
  position: relative;
  flex: 1;
  display: flex;
  justify-content: space-between;
  align-items: center;
  min-width: 0;
  margin: 6px 42px 0;
}

.track::before {
  content: '';
  position: absolute;
  /* 越过站点区两侧留白，水平延伸到牌面黑色竖向分隔线处 */
  left: -56px;
  right: -56px;
  top: 50%;
  height: clamp(8px, 0.9vw, 12px);
  transform: translateY(-50%);
  background: var(--accent-dark);
  border-radius: 999px;
}

.station {
  position: relative;
  z-index: 1;
  flex-shrink: 1;
  min-width: 0;
}

.station-dot {
  display: block;
  width: clamp(16px, 1.8vw, 24px);
  height: clamp(16px, 1.8vw, 24px);
  margin: 0 auto;
  border-radius: 50%;
  background: #fff;
  border: clamp(3px, 0.35vw, 5px) solid var(--accent-dark);
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.station:hover .station-dot { transform: scale(1.18); }

.station.is-active .station-dot {
  background: var(--accent-secondary);
  border-color: var(--ink);
  box-shadow: 0 0 0 5px rgba(46, 196, 182, 0.3);
  transform: scale(1.15);
}

/* 站名：水平排列，1/3/5 站在圆点下方，2/4 站在圆点上方错开 */
.station-name {
  position: absolute;
  top: calc(100% + 8px);
  left: 50%;
  transform: translateX(-50%);
  white-space: nowrap;
  font-size: clamp(0.58rem, 0.8vw, 0.75rem);
  font-weight: 700;
  color: var(--ink);
  cursor: pointer;
  background: transparent;
  border: none;
  padding: 1px 4px;
  border-radius: 6px;
  font-family: var(--font-body);
}

.station-name.is-above {
  top: auto;
  bottom: calc(100% + 8px);
}

.station.is-active .station-name { color: var(--accent-dark); }

/* 线路牌右侧操作区：标签 + 单个「查看更多」动态按钮 */
.board-actions {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding-left: 1.4%;
  border-left: 2.5px solid var(--ink);
  white-space: nowrap;
}

/* 与「MyLab 中央站」牌头标题同款大小与样式 */
.board-actions-label {
  font-size: clamp(0.85rem, 1.35vw, 1.15rem);
  font-weight: 700;
  color: var(--ink);
  line-height: 1.25;
}

.board-btn {
  position: relative;
  overflow: hidden;
  padding: clamp(5px, 0.7vw, 10px) clamp(12px, 1.4vw, 22px);
  border: 2.5px solid var(--ink);
  border-radius: 999px;
  background: linear-gradient(120deg, var(--accent-dark), var(--accent) 55%, var(--accent-secondary));
  background-size: 180% 100%;
  font-family: var(--font-body);
  font-size: clamp(0.65rem, 0.85vw, 0.8rem);
  font-weight: 700;
  letter-spacing: 0.05em;
  color: #fff;
  cursor: pointer;
  animation: btnGlow 2.6s ease-in-out infinite;
  transition: background-position 0.5s ease, transform 0.25s ease, box-shadow 0.25s ease;
}

/* 高光扫过 */
.board-btn::before {
  content: '';
  position: absolute;
  top: 0;
  bottom: 0;
  left: -60%;
  width: 40%;
  background: linear-gradient(100deg, transparent, rgba(255, 255, 255, 0.55), transparent);
  transform: skewX(-20deg);
  animation: btnSheen 2.8s ease-in-out infinite;
}

.board-btn-text {
  position: relative;
  z-index: 1;
}

.board-btn:hover {
  background-position: 100% 0;
  transform: translateY(-2px) scale(1.04);
}

.board-btn:active { transform: translateY(0) scale(0.97); }

@keyframes btnSheen {
  0%, 55% { left: -60%; }
  85%, 100% { left: 130%; }
}

@keyframes btnGlow {
  0%, 100% { box-shadow: 0 6px 16px rgba(91, 164, 230, 0.35); }
  50% { box-shadow: 0 6px 22px rgba(46, 196, 182, 0.55); }
}

/* ---- 站点卡片（选中站点 = 研究记录）：位于站点列表排水平左侧，吊杆悬挂 ---- */
.info-panel {
  position: absolute;
  top: 42px;
  left: 2.5%;
  width: 20%;
  background: #fff;
  border: 3px solid var(--ink);
  border-radius: 8px;
  box-shadow: 6px 6px 0 rgba(27, 58, 75, 0.15);
  z-index: 6;
}

/* 天花板连接杆 */
.info-panel::before,
.info-panel::after {
  content: '';
  position: absolute;
  bottom: 100%;
  width: 4px;
  height: 42px;
  background: var(--ink);
}

.info-panel::before { left: 20%; }
.info-panel::after { right: 20%; }

.info-panel-head {
  padding: 6px 12px;
  background: var(--accent-dark);
  color: #fff;
  font-family: var(--font-mono);
  font-size: clamp(0.5rem, 0.68vw, 0.62rem);
  font-weight: 600;
  letter-spacing: 0.18em;
  display: flex;
  justify-content: space-between;
  border-radius: 5px 5px 0 0;
}

.info-panel-body { padding: 10px 12px 12px; }

.info-title {
  font-size: clamp(0.75rem, 1vw, 0.95rem);
  font-weight: 700;
  color: var(--ink);
  line-height: 1.35;
  margin-bottom: 2px;
}

.info-date {
  font-family: var(--font-mono);
  font-size: clamp(0.52rem, 0.65vw, 0.6rem);
  color: var(--ink-muted);
  margin-bottom: 6px;
}

.info-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  margin-bottom: 6px;
}

.info-tag {
  font-size: clamp(0.5rem, 0.62vw, 0.58rem);
  font-weight: 700;
  color: var(--accent-dark);
  background: var(--accent-light);
  padding: 1px 7px;
  border-radius: 999px;
}

.info-summary {
  font-size: clamp(0.55rem, 0.72vw, 0.68rem);
  line-height: 1.55;
  color: var(--ink-light);
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.info-stops {
  margin-top: 8px;
  padding-top: 6px;
  border-top: 1.5px dashed var(--border);
  font-family: var(--font-mono);
  font-size: clamp(0.5rem, 0.65vw, 0.6rem);
  font-weight: 600;
  letter-spacing: 0.12em;
  color: var(--ink-muted);
}

/* ---- 列车：白车身 + 明亮窗带 + 珊瑚色车头盖与双腰线，停站时车尾在画面外 ---- */
.train {
  position: absolute;
  left: -2%;
  top: 34%;
  width: 122%;
  height: 37%;
  background: linear-gradient(180deg, #FFFFFF 0%, #F2FAFF 55%, #D9EBF8 100%);
  border: 3px solid var(--ink);
  border-radius: 6px;
  border-top-right-radius: 22% 65%;
  border-bottom-right-radius: 10% 22%;
  /* 车尾圆角：停站时车尾在画面左缘之外，驶离动画时掠过站台 */
  border-top-left-radius: 12% 60%;
  border-bottom-left-radius: 5% 18%;
  overflow: hidden;
  z-index: 4;
  transform: translateX(-110%);
  transition: transform 1s ease-in;
}

.train.arrived {
  transform: translateX(-14%);
  transition: transform 2.6s cubic-bezier(0.25, 1, 0.35, 1);
}

/* 向前（车头方向，向右）驶离站台 */
.train.departed {
  transform: translateX(110%);
}

/* 明亮窗带（透出车厢灯光，带竖向分隔） */
.train-band {
  position: absolute;
  left: 0;
  right: 0;
  top: 16%;
  height: 36%;
  background:
    repeating-linear-gradient(90deg, transparent 0 calc(5.4% - 2px), rgba(27, 58, 75, 0.5) calc(5.4% - 2px) 5.4%),
    linear-gradient(180deg, #EAF6FF 0%, #C4E2F5 55%, #9CC8E8 100%);
  border-top: 2px solid var(--ink);
  border-bottom: 2px solid var(--ink);
}

/* 珊瑚色车头盖 */
.train-nose {
  position: absolute;
  right: 0;
  top: 0;
  width: 13%;
  height: 62%;
  background: linear-gradient(165deg, #FF8A8A 0%, #FF6B6B 50%, #E85555 100%);
  border-bottom: 3px solid var(--ink);
  border-bottom-left-radius: 75% 95%;
}

/* 前挡风玻璃 */
.train-windshield {
  position: absolute;
  right: 2.5%;
  top: 18%;
  width: 7%;
  height: 38%;
  background: linear-gradient(120deg, #1A2936, #060D14);
  border: 2.5px solid var(--ink);
  border-radius: 28% 42% 30% 18% / 45% 55% 40% 30%;
}

/* 车头灯 */
.train-headlight {
  position: absolute;
  right: 1.8%;
  bottom: 18%;
  width: 1.1%;
  aspect-ratio: 1;
  background: radial-gradient(circle, #FFFFFF 0%, #BFE3FF 75%);
  border: 2px solid var(--ink);
  border-radius: 50%;
}

/* 珊瑚色水平腰线 */
.train-wave {
  position: absolute;
  left: 0;
  top: 55%;
  width: 91%;
  height: 20%;
  pointer-events: none;
}

/* 车门上方的珊瑚色水平色带 */
.train-stripe-top {
  position: absolute;
  left: 0;
  top: 6%;
  width: 91%;
  height: 4.5%;
  background: #FF6B6B;
  pointer-events: none;
}

/* 深色裙板 */
.train-skirt {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  height: 12%;
  background: linear-gradient(180deg, #2A3B47, #16222B);
  border-top: 2px solid var(--ink);
}

/* 车尾窗（停站时位于画面外） */
.train-tail-window {
  position: absolute;
  left: 1.2%;
  top: 18%;
  width: 4.5%;
  height: 32%;
  background: linear-gradient(120deg, #1A2936, #060D14);
  border: 2.5px solid var(--ink);
  border-radius: 40% 18% 18% 30% / 55% 40% 40% 45%;
}

/* 尾灯（停站时位于画面外） */
.train-tail-light {
  position: absolute;
  left: 1.2%;
  bottom: 18%;
  width: 1.2%;
  aspect-ratio: 1;
  background: radial-gradient(circle, #FF8A80 0%, #E85555 75%);
  border: 2px solid var(--ink);
  border-radius: 50%;
}

/* 列车车门 ×2：比屏蔽门单元更宽更高，打开后可见对侧关闭的车门 */
.train-door {
  position: absolute;
  top: 16%;
  bottom: 0;
  width: 13%;
  background: linear-gradient(180deg, #E8F3FA 0%, #D5E8F4 100%);
  border: 2.5px solid var(--ink);
}

/* 门内跑马灯条带：位于原对侧车门玻璃窗的高度，左右延伸至与门框边缘相触 */
.train-marquee {
  position: absolute;
  left: 0;
  right: 0;
  top: 14%;
  height: 22%;
  background: #101C26;
  border-top: 2px solid var(--ink);
  border-bottom: 2px solid var(--ink);
  overflow: hidden;
  display: flex;
  align-items: center;
}

.train-marquee span {
  white-space: nowrap;
  font-family: var(--font-mono);
  font-size: clamp(0.55rem, 0.75vw, 0.7rem);
  font-weight: 600;
  color: #7CFFB2;
  padding-left: 100%;
  animation: marqueeScroll 9s linear infinite;
}

@keyframes marqueeScroll {
  to { transform: translateX(-100%); }
}

.train-door-leaf {
  position: absolute;
  /* 门叶外扩 2.5px、宽度补到 50%+2.5px：边框与门框重合且中缝闭紧，关/开门边框粗细一致 */
  top: -2.5px;
  bottom: -2.5px;
  width: calc(50% + 2.5px);
  z-index: 2;   /* 压在门内跑马灯条带之上 */
  background: linear-gradient(180deg, #F7FCFF, #E3F0F9);
  /* 门叶四周完整黑框，滑出门洞后边缘依然清晰 */
  border: 2.5px solid var(--ink);
  transition: transform 0.85s cubic-bezier(0.4, 0, 0.2, 1) 0.05s;
}

.train-door-leaf::before {
  content: '';
  position: absolute;
  top: 6%;
  left: 12%;
  right: 12%;
  height: 46%;
  background: linear-gradient(180deg, #EAF6FF, #9CC8E8);
  border: 2px solid var(--ink);
  border-radius: 3px;
}

.train-door-leaf.is-left { left: -2.5px; }
.train-door-leaf.is-right { right: -2.5px; }

.train.doors-open .train-door-leaf.is-left { transform: translateX(-94%); }
.train.doors-open .train-door-leaf.is-right { transform: translateX(94%); }

/* ---- 站台屏蔽门（玻璃围栏 + 两档滑动自动门），底边直接压在站台地面上 ---- */
.psd {
  position: absolute;
  left: 0;
  right: 0;
  top: 38%;
  height: 33%;
  display: flex;
  z-index: 5;
  border-top: 3px solid var(--ink);
  border-bottom: 3px solid var(--ink);
  background: rgba(255, 255, 255, 0.12);
}

.psd-panel {
  position: relative;
  height: 100%;
  flex: 1;
  background:
    repeating-linear-gradient(90deg, transparent 0 calc(25% - 2px), rgba(27, 58, 75, 0.35) calc(25% - 2px) 25%),
    rgba(255, 255, 255, 0.18);
  border-left: 2px solid rgba(27, 58, 75, 0.55);
  border-right: 2px solid rgba(27, 58, 75, 0.55);
  overflow: hidden;
}

/* 玻璃高光 */
.psd-glass {
  position: absolute;
  inset: 0;
  background: linear-gradient(115deg, rgba(255, 255, 255, 0.6) 0%, rgba(255, 255, 255, 0.05) 32%, rgba(255, 255, 255, 0.4) 58%, rgba(255, 255, 255, 0.05) 82%);
}

/* 下部百叶挡板 */
.psd-louver {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  height: 34%;
  background: repeating-linear-gradient(180deg, #E8F1F8 0 3px, #CBDEEC 3px 6px);
  border-top: 2px solid rgba(27, 58, 75, 0.45);
}

/* 自动门单元：位置与列车车门对齐 */
.psd-door {
  position: relative;
  width: 18%;
  flex-shrink: 0;
  height: 100%;
}

.psd-light {
  position: absolute;
  top: -11px;
  left: 50%;
  transform: translateX(-50%);
  width: 16px;
  height: 8px;
  background: #FF6B6B;
  border: 2px solid var(--ink);
  border-radius: 4px;
  z-index: 2;
  transition: background 0.3s ease;
}

.psd.open .psd-light { background: var(--accent-secondary); }

.psd-leaf {
  position: absolute;
  top: 0;
  bottom: 0;
  width: 50%;
  border: 2.5px solid var(--ink);
  background: rgba(190, 224, 245, 0.4);
  overflow: hidden;
  transition: transform 0.85s cubic-bezier(0.4, 0, 0.2, 1);
}

.psd-leaf.is-left { left: 0; border-right-width: 1.25px; }
.psd-leaf.is-right { right: 0; border-left-width: 1.25px; }

.psd.open .psd-leaf.is-left { transform: translateX(-97%); }
.psd.open .psd-leaf.is-right { transform: translateX(97%); }

/* ---- 站台地面（透视） ---- */
.floor {
  position: absolute;
  left: -12%;
  right: -12%;
  bottom: -4%;
  height: 52%;
  transform: rotateX(58deg);
  transform-origin: bottom center;
  background:
    repeating-linear-gradient(90deg, transparent 0 6.9%, rgba(27, 58, 75, 0.28) 6.9% 7.1%),
    repeating-linear-gradient(180deg, transparent 0 12.5%, rgba(27, 58, 75, 0.28) 12.5% 13%),
    linear-gradient(180deg, #F8FCFE 0%, #E9F3FA 100%);
  z-index: 2;
}

/* 黄色安全线 */
.floor::before {
  content: '';
  position: absolute;
  left: 0;
  right: 0;
  top: 24%;
  height: 4.5%;
  background: var(--safety);
  box-shadow: 0 2px 0 rgba(27, 58, 75, 0.2);
}

.floor-text {
  position: absolute;
  top: 32%;
  left: 50%;
  transform: translateX(-50%);
  font-family: var(--font-mono);
  font-size: clamp(0.9rem, 1.5vw, 1.35rem);
  font-weight: 700;
  letter-spacing: 0.6em;
  color: var(--safety);
  text-shadow: 0 1px 2px rgba(27, 58, 75, 0.25);
  white-space: nowrap;
}

@media (max-width: 767px) {
  /* 移动端面板占满宽度，避免缩得过小 */
  .scene-wrap { width: 100%; }
}

@media (prefers-reduced-motion: reduce) {
  .train,
  .train-door-leaf,
  .psd-leaf { transition: none; }
  .board-btn,
  .board-btn::before { animation: none; }
  .train-marquee span { animation: none; padding-left: 0; }
}

/* 换站时瞬移回起点用：禁用门与列车的过渡 */
.no-anim .train,
.no-anim .train-door-leaf,
.no-anim .psd-leaf,
.no-anim .psd-light { transition: none !important; }
</style>
