<template>
  <section id="contact">
    <div class="container">
      <div class="contact-wrapper">
        <div class="main-row">
          <div class="left-col">
            <div class="runtime-mini">
              <span class="runtime-dot" />
              <span class="runtime-text">已运行 <span class="runtime-num">{{ days }}</span>天 <span class="runtime-num">{{ pad(hours) }}</span>小时 <span class="runtime-num">{{ pad(minutes) }}</span>分钟 <span class="runtime-num">{{ pad(seconds) }}</span>秒</span>
            </div>
            <div class="social-row">
              <a
                v-if="support.github_enabled"
                :href="support.github_url"
                target="_blank"
                rel="noopener"
                class="social-btn"
              >
                <svg
                  viewBox="0 0 19 19"
                  aria-hidden="true"
                >
                  <path
                    fill="currentColor"
                    fill-rule="evenodd"
                    d="M9.356 1.85C5.05 1.85 1.57 5.356 1.57 9.694a7.84 7.84 0 0 0 5.324 7.44c.387.079.528-.168.528-.376 0-.182-.013-.805-.013-1.454-2.165.467-2.616-.935-2.616-.935-.349-.91-.864-1.143-.864-1.143-.71-.48.051-.48.051-.48.787.051 1.2.805 1.2.805.695 1.194 1.817.857 2.268.649.064-.507.27-.857.49-1.052-1.728-.182-3.545-.857-3.545-3.87 0-.857.31-1.558.8-2.104-.078-.195-.349-1 .077-2.078 0 0 .657-.208 2.14.805a7.5 7.5 0 0 1 1.946-.26c.657 0 1.328.092 1.946.26 1.483-1.013 2.14-.805 2.14-.805.426 1.078.155 1.883.078 2.078.502.546.799 1.247.799 2.104 0 3.013-1.818 3.675-3.558 3.87.284.247.528.714.528 1.454 0 1.052-.012 1.896-.012 2.156 0 .208.142.455.528.377a7.84 7.84 0 0 0 5.324-7.441c.013-4.338-3.48-7.844-7.773-7.844"
                    clip-rule="evenodd"
                  />
                </svg>
                <span>GitHub</span>
              </a>
              <div
                v-if="support.email_enabled"
                class="social-btn email-panel"
                :title="emailDisplay"
              >
                <svg
                  viewBox="0 0 24 24"
                  aria-hidden="true"
                >
                  <path
                    d="M4.75 6.75h14.5v10.5H4.75z"
                    fill="none"
                    stroke="currentColor"
                    stroke-linecap="round"
                    stroke-linejoin="round"
                    stroke-width="1.7"
                  />
                  <path
                    d="m5.25 7.25 6.75 5.5 6.75-5.5"
                    fill="none"
                    stroke="currentColor"
                    stroke-linecap="round"
                    stroke-linejoin="round"
                    stroke-width="1.7"
                  />
                </svg>
                <span class="email-copy">
                  <span class="email-label">邮箱</span>
                  <span class="email-address">{{ emailDisplay }}</span>
                </span>
              </div>
            </div>
          </div>

          <div
            v-if="statistics"
            class="stats-grid"
            aria-label="全站统计"
          >
            <div class="stat-card">
              <span class="stat-label">访问数</span>
              <span class="stat-value">{{ formatNumber(statistics.visit_count) }}</span>
            </div>
            <div class="stat-card">
              <span class="stat-label">点赞数</span>
              <span class="stat-value">{{ formatNumber(statistics.total_like_count) }}</span>
            </div>
            <div class="stat-card">
              <span class="stat-label">浏览量</span>
              <span class="stat-value">{{ formatNumber(statistics.total_view_count) }}</span>
            </div>
          </div>
        </div>

        <p
          v-if="support.icp_enabled && support.icp_number"
          class="icp-note"
        >
          2026 &copy; shennn的个人空间 · 备案号 {{ support.icp_number }}
        </p>
        <p
          v-if="support.cloud_enabled && support.cloud_provider"
          class="cloud-note"
        >
          由 <strong>{{ support.cloud_provider }}</strong> 提供云服务
        </p>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { useSiteStatistics } from '../composables/useSiteStatistics'

const support = {
  site_started_at: '2024-04-21T00:00:00+08:00',
  github_url: 'https://github.com', github_enabled: true,
  email: '', email_enabled: true,
  icp_number: 'XXXXXXX', icp_enabled: true,
  cloud_provider: '阿里云', cloud_enabled: true,
}
const { statistics } = useSiteStatistics()
const numberFormatter = new Intl.NumberFormat('zh-CN')
const formatNumber = (value: number) => numberFormatter.format(value)
const emailDisplay = support.email || '邮箱暂未设置'
const days = ref(0)
const hours = ref(0)
const minutes = ref(0)
const seconds = ref(0)
let runtimeTimer: number | undefined
let startTime = 0

const pad = (value: number) => String(value).padStart(2, '0')

const updateRuntime = () => {
  const startedAt = Date.parse(support.site_started_at)
  const total = Math.max(0, Math.floor((Date.now() - (Number.isNaN(startedAt) ? startTime : startedAt)) / 1000))
  days.value = Math.floor(total / 86400)
  hours.value = Math.floor((total % 86400) / 3600)
  minutes.value = Math.floor((total % 3600) / 60)
  seconds.value = total % 60
}

onMounted(() => {
  startTime = Date.now()
  updateRuntime()
  runtimeTimer = window.setInterval(updateRuntime, 1000)
})

onBeforeUnmount(() => {
  if (runtimeTimer) {
    window.clearInterval(runtimeTimer)
  }
})
</script>

<style scoped>
#contact {
  position: relative;
  overflow: hidden;
  padding: clamp(1.5rem, 3vw, 2.5rem) 0;
  text-align: center;
  background: linear-gradient(180deg, #1B4965 0%, #0D1B2A 100%);
}

#contact::after {
  content: '';
  position: absolute;
  left: 50%;
  bottom: 0;
  width: min(760px, 88vw);
  height: 1px;
  transform: translateX(-50%);
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.2), transparent);
}

.container {
  position: relative;
  z-index: 1;
  max-width: 1000px;
  margin: 0 auto;
  padding: 0 2rem;
}

.contact-wrapper {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 1rem;
}

.main-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  gap: 1.5rem;
}

.left-col {
  display: flex;
  flex-direction: row;
  align-items: center;
  gap: 0.5rem;
  flex-shrink: 0;
}

.stats-grid {
  display: flex;
  gap: 0.5rem;
  flex-wrap: nowrap;
  justify-content: flex-end;
  flex-shrink: 0;
}

.stat-card {
  display: flex;
  flex-direction: row;
  align-items: center;
  justify-content: center;
  gap: 0.45rem;
  min-width: 96px;
  padding: 0.7rem 0.9rem;
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.15);
  transition: transform 0.2s ease, background 0.2s ease, border-color 0.2s ease;
}

.stat-card:hover {
  transform: translateY(-3px);
  background: rgba(255, 255, 255, 0.1);
  border-color: rgba(255, 255, 255, 0.2);
}

.stat-value {
  font-family: var(--font-mono);
  font-size: clamp(1rem, 2vw, 1.35rem);
  font-weight: 700;
  color: #5BA4E6;
  line-height: 1;
}

.stat-label {
  font-size: 0.78rem;
  font-weight: 500;
  letter-spacing: 0.02em;
  color: rgba(255, 255, 255, 0.85);
}

.runtime-mini {
  display: inline-flex;
  align-items: center;
  gap: 0.45rem;
  padding: 0.7rem 0.85rem;
  background: rgba(91, 164, 230, 0.08);
  border: 1px solid rgba(91, 164, 230, 0.15);
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.15);
}

.runtime-dot {
  width: 5px;
  height: 5px;
  background: #39FF14;
  box-shadow: 0 0 6px #39FF14, 0 0 12px rgba(57, 255, 20, 0.5);
  border-radius: 50%;
  animation: pulse 2s ease-in-out infinite;
  flex-shrink: 0;
}

@keyframes pulse {
  0%, 100% { opacity: 1; transform: scale(1); }
  50% { opacity: 0.5; transform: scale(0.85); }
}

.runtime-text {
  font-family: var(--font-mono);
  font-size: clamp(0.9rem, 1.5vw, 1.05rem);
  font-weight: 500;
  letter-spacing: 0.02em;
  color: rgba(255, 255, 255, 0.9);
  line-height: 1;
}

.runtime-num {
  color: #39FF14;
  font-weight: 700;
  text-shadow: 0 0 6px rgba(57, 255, 20, 0.5);
  font-variant-numeric: tabular-nums;
}

.social-row {
  display: flex;
  gap: 0.5rem;
  flex-wrap: nowrap;
  align-items: center;
}

.social-btn {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  padding: 0.7rem 0.85rem;
  font-size: 0.78rem;
  font-weight: 500;
  letter-spacing: 0.02em;
  color: rgba(255, 255, 255, 0.85);
  text-decoration: none;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.15);
  transition: all 0.2s ease;
}

.social-btn svg {
  width: 0.95rem;
  height: 0.95rem;
  color: rgba(255, 255, 255, 0.85);
}

.social-btn:hover {
  color: #5BA4E6;
  border-color: rgba(91, 164, 230, 0.5);
  background: rgba(91, 164, 230, 0.1);
  transform: translateY(-2px);
}

.social-btn:hover svg {
  color: #5BA4E6;
}

.email-panel {
  cursor: default;
}

.email-copy,
.email-label,
.email-address {
  display: inline-flex;
  align-items: center;
}

.email-copy {
  min-width: 2rem;
  justify-content: center;
}

.email-address {
  display: none;
  font-family: var(--font-mono);
  font-size: 0.72rem;
  white-space: nowrap;
}

.email-panel:hover .email-label {
  display: none;
}

.email-panel:hover .email-address {
  display: inline-flex;
}

.icp-note {
  margin: 0;
  font-size: 0.65rem;
  color: rgba(255, 255, 255, 0.35);
  letter-spacing: 0.02em;
}

.cloud-note {
  margin: -0.4rem 0 0;
  font-size: 0.62rem;
  color: rgba(255, 255, 255, 0.3);
  letter-spacing: 0.02em;
}

.cloud-note strong {
  color: rgba(255, 255, 255, 0.55);
  font-weight: 500;
}

@media (max-width: 600px) {
  .container {
    padding: 0 1.25rem;
  }

  .main-row {
    flex-direction: column;
    align-items: center;
    gap: 1rem;
  }

  .left-col {
    align-items: center;
  }

  .stats-grid {
    justify-content: center;
  }

  .stat-card {
    min-width: 88px;
    padding: 0.6rem 0.75rem;
  }
}
</style>
