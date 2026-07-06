<template>
  <nav class="navigation">
    <div class="nav-logo">旅行者<em>.</em></div>
    <ul class="nav-links">
      <li><a href="#influencer">关于我</a></li>
      <li><a href="#skills">技能</a></li>
      <li><a href="#work">项目</a></li>
      <li><a href="#hobbies">爱好</a></li>
      <li><a href="#game">游戏</a></li>
      <li><a href="#aicoding">Vibe Coding</a></li>
      <li><a href="#contact">联系</a></li>
    </ul>
    <div class="nav-right">
      <a href="#influencer" class="nav-cta">探索更多</a>
    </div>
  </nav>
</template>

<script setup lang="ts">
</script>

<style scoped>
.navigation {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  height: var(--nav-h);
  z-index: 1000;
  display: flex;
  align-items: center;
  padding: 0 3rem;
  background: rgba(244, 240, 235, 0.88);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border-bottom: 1px solid var(--border);
}

.nav-logo {
  font-family: var(--font-display);
  font-size: 1.1rem;
  font-weight: 700;
  color: var(--ink);
  letter-spacing: -0.01em;
  flex-shrink: 0;
}

.nav-logo em {
  font-style: italic;
  color: var(--accent);
}

.nav-links {
  display: flex;
  gap: 2.5rem;
  margin-left: 4rem;
  list-style: none;
}

.nav-links a {
  font-family: var(--font-display);
  font-size: 0.78rem;
  letter-spacing: 0.04em;
  color: var(--ink);
  position: relative;
  transition: color 0.2s;
}

.nav-links a::after {
  content: '';
  position: absolute;
  bottom: -2px;
  left: 0;
  width: 0;
  height: 1px;
  background: var(--accent);
  transition: width 0.3s ease;
}

.nav-links a:hover {
  color: var(--ink);
}

.nav-links a:hover::after {
  width: 100%;
}

.nav-right {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 1.5rem;
}

.nav-cta {
  font-family: var(--font-display);
  font-size: 0.78rem;
  letter-spacing: 0.04em;
  padding: 0.45rem 1.4rem;
  background: var(--accent);
  color: #fff;
  transition: opacity 0.2s, transform 0.2s;
  position: relative;
}

.nav-cta:hover {
  opacity: 0.85;
  transform: translateY(-1px);
}

.nav-cta::after {
  display: none;
}

@media (max-width: 768px) {
  .navigation {
    padding: 0 1.2rem;
  }
  
  .nav-links {
    display: none;
  }
}
</style>
