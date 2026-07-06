<template>
  <section id="game">
    <div class="container">
      <RevealOnScroll>
        <div class="section-header">
          <span class="section-num">05</span>
          <div class="section-title-group">
            <h2 class="section-title">我的<em>游戏</em></h2>
            <p class="section-desc">从小学到现在，游戏陪伴了我很多时光，让我在紧张的学习生活之余得到了放松。</p>
          </div>
        </div>
      </RevealOnScroll>

      <div class="game-area-wrapper">
        <div class="game-marquee-bg" aria-label="常玩游戏海报">
          <div class="game-marquee-bg-track">
            <template v-for="copy in 2" :key="copy">
              <div v-for="game in bgGames" :key="`${game}-${copy}`" class="game-marquee-bg-item">
                <img :src="game" :alt="game" />
              </div>
            </template>
          </div>
        </div>

        <div class="game-cards-grid">
          <RevealOnScroll
            v-for="(game, index) in featuredGames"
            :key="game.name"
            :class="['game-card-slot', `game-card-slot--${index + 1}`]"
            :delay="(index % 3) + 1"
          >
            <div class="game-card">
              <img :src="game.image" :alt="game.name" loading="lazy" />
              <div class="game-card-overlay">
                <p class="game-card-description">{{ game.description }}</p>
                <span class="game-card-tag">{{ game.tag }}</span>
                <h3 class="game-card-title">{{ game.name }}</h3>
                <p class="game-card-subtitle">{{ game.subtitle }}</p>
              </div>
            </div>
          </RevealOnScroll>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { games } from '@/data/projects'
import RevealOnScroll from './ui/RevealOnScroll.vue'

const gameDescriptions: Record<string, string> = {
  'Counter-Strike 2': '最喜欢它纯粹又残酷的博弈感，每一颗道具、每一次peek都要为团队节奏负责。',
  'Apex 英雄': '机动性和临场决策很迷人，打赢一波混战时会有非常强的爽感和节奏感。',
  '三角洲行动': '偏战术、偏压迫的枪线体验，适合认真研究路线、信息和团队配合。',
  '无畏契约': '技能和枪法互相牵制，回合制的紧张感很足，残局尤其容易让人上头。',
  '守望先锋 2': '英雄机制和团战节奏变化很快，最吸引我的是团队位置和技能交换。',
  '英雄联盟': '长期陪伴型游戏，版本、位置、运营和团战判断总能不断产生新的理解。'
}

const featuredGames = computed(() => (
  games.slice(0, 5).map((game) => ({
    ...game,
    description: gameDescriptions[game.name] ?? game.subtitle
  }))
))

const bgGames = computed(() => [
  './game_posters/valorant.jpeg',
  './game_posters/overwatch2.jpeg',
  './game_posters/cs2.jpg',
  './game_posters/apex.jpg',
  './game_posters/delta-force.jpg',
  './game_posters/league-of-legends.jpeg',
  './game_posters/the-finals.jpg',
])
</script>

<style scoped>
#game {
  padding: 100px 0;
  overflow: hidden;
  position: relative;
  z-index: 2;
}

.container {
  max-width: var(--max-w);
  margin: 0 auto;
  padding: 0 3rem;
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
  color: var(--accent);
}

.section-desc {
  font-size: 0.95rem;
  color: var(--ink-light);
  max-width: 480px;
  font-weight: 300;
  line-height: 1.8;
}

/* Game Area Wrapper */
.game-area-wrapper {
  position: relative;
  overflow: visible;
}

.game-cards-grid {
  display: grid;
  grid-template-columns: 0.9fr 1fr 1fr;
  grid-template-rows: repeat(3, minmax(190px, 1fr));
  gap: 1.5rem;
  position: relative;
  z-index: 1;
}

.game-card-slot {
  min-height: 0;
}

.game-card-slot--1 {
  grid-column: 2 / 4;
  grid-row: 1 / 3;
}

.game-card-slot--2 {
  grid-column: 1 / 2;
  grid-row: 1 / 3;
}

.game-card-slot--3 {
  grid-column: 1 / 2;
  grid-row: 3 / 4;
}

.game-card-slot--4 {
  grid-column: 2 / 3;
  grid-row: 3 / 4;
}

.game-card-slot--5 {
  grid-column: 3 / 4;
  grid-row: 3 / 4;
}

.game-card {
  background: var(--bg-card);
  border-radius: 16px;
  overflow: hidden;
  position: relative;
  height: 100%;
  min-height: 190px;
  cursor: pointer;
  transition: transform 0.4s cubic-bezier(0.25, 0.46, 0.45, 0.94), box-shadow 0.4s ease;
  box-shadow: 0 4px 20px rgba(20, 18, 16, 0.06);
}

.game-card:hover {
  transform: translateY(-8px);
  box-shadow: 0 16px 40px rgba(20, 18, 16, 0.12);
}

.game-card img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
  transition: transform 0.6s cubic-bezier(0.25, 0.46, 0.45, 0.94);
}

.game-card:hover img {
  transform: scale(1.08);
}

.game-card-overlay {
  position: absolute;
  inset: 0;
  background: linear-gradient(
    to top,
    rgba(20, 18, 16, 0.95) 0%,
    rgba(20, 18, 16, 0.6) 40%,
    rgba(20, 18, 16, 0.1) 70%,
    transparent 100%
  );
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
  padding: 1.5rem;
  transition: background 0.4s ease;
}

.game-card:hover .game-card-overlay {
  background: linear-gradient(
    to top,
    rgba(20, 18, 16, 0.98) 0%,
    rgba(20, 18, 16, 0.75) 50%,
    rgba(20, 18, 16, 0.2) 80%,
    transparent 100%
  );
}

.game-card-description {
  position: absolute;
  left: 1.25rem;
  right: 1.25rem;
  top: 1.25rem;
  margin: 0;
  padding: 0.9rem 1rem;
  background: rgba(244, 240, 235, 0.92);
  color: var(--ink);
  border-left: 3px solid var(--accent);
  border-radius: 8px;
  box-shadow: 0 14px 34px rgba(20, 18, 16, 0.22);
  font-size: 0.88rem;
  line-height: 1.65;
  font-weight: 400;
  opacity: 0;
  transform: translateY(-12px);
  transition: opacity 0.35s ease, transform 0.35s cubic-bezier(0.2, 0.85, 0.25, 1);
  pointer-events: none;
}

.game-card:hover .game-card-description {
  opacity: 1;
  transform: translateY(0);
}

.game-card-tag {
  font-family: var(--font-mono);
  font-size: 0.6rem;
  letter-spacing: 0.15em;
  text-transform: uppercase;
  color: var(--accent);
  background: rgba(191, 58, 30, 0.15);
  padding: 0.25rem 0.6rem;
  border-radius: 4px;
  display: inline-block;
  margin-bottom: 0.5rem;
  width: fit-content;
}

.game-card-title {
  font-family: var(--font-display);
  font-size: 1.5rem;
  font-weight: 700;
  color: var(--bg-card);
  line-height: 1.2;
  margin-bottom: 0.3rem;
}

.game-card-subtitle {
  font-family: var(--font-body);
  font-size: 0.8rem;
  color: rgba(244, 240, 235, 0.7);
  opacity: 0;
  transform: translateY(10px);
  transition: opacity 0.4s ease, transform 0.4s ease;
}

.game-card:hover .game-card-subtitle {
  opacity: 1;
  transform: translateY(0);
}

/* Background Marquee */
.game-marquee-bg {
  position: absolute;
  top: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 100vw;
  z-index: 0;
  pointer-events: none;
  overflow: hidden;
}

.game-marquee-bg-track {
  display: flex;
  align-items: center;
  width: max-content;
  animation: gameBgMarqueeScroll 80s linear infinite;
  padding: 3rem 0;
}

.game-marquee-bg-item {
  flex: 0 0 auto;
  display: block;
  margin: 0 1.5rem;
  border-radius: 12px;
  overflow: hidden;
  filter: blur(4px) brightness(0.5);
  opacity: 0.6;
}

.game-marquee-bg-item img {
  width: 800px;
  height: 450px;
  object-fit: cover;
  display: block;
}

@keyframes gameBgMarqueeScroll {
  from {
    transform: translateX(0);
  }

  to {
    transform: translateX(-50%);
  }
}

@media (max-width: 1024px) {
  .game-cards-grid {
    grid-template-columns: repeat(2, 1fr);
    grid-template-rows: none;
  }

  .game-card-slot {
    grid-column: auto;
    grid-row: auto;
  }

  .game-card-slot--1 {
    grid-column: 1 / -1;
  }

  .game-card {
    aspect-ratio: 4 / 3;
    height: auto;
  }
}

@media (max-width: 600px) {
  .game-cards-grid {
    grid-template-columns: 1fr;
    gap: 1rem;
  }

  .game-card-title {
    font-size: 1.25rem;
  }

  .game-card-slot--1 {
    grid-column: auto;
  }

  .game-card-description {
    left: 1rem;
    right: 1rem;
    top: 1rem;
    font-size: 0.8rem;
    line-height: 1.55;
  }
}

@media (prefers-reduced-motion: reduce) {
  .game-marquee-track,
  .game-marquee-bg-track {
    animation: none;
  }
}
</style>
