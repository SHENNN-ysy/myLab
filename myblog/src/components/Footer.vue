<template>
  <footer>
    <span class="copy">&copy; 2026 旅行者 · shennn</span>
    <button type="button" class="back-top" @click="scrollToTop">↑ 回到顶部</button>
  </footer>
</template>

<script setup lang="ts">
const scrollToTop = () => {
  window.scrollTo({ top: 0, behavior: 'smooth' })
}
</script>

<style scoped>
footer {
  padding: 1.5rem 3rem;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 1.5rem;
  border-top: 1px solid var(--border);
}

.copy,
.back-top {
  font-family: var(--font-mono);
  font-size: 0.65rem;
  letter-spacing: 0.08em;
  color: var(--ink-muted);
}

.back-top {
  flex: none;
  padding: 0;
  border: 0;
  background: transparent;
  text-transform: uppercase;
  cursor: pointer;
  transition: color 0.2s;
}

.back-top:hover { color: var(--accent); }

@media (max-width: 720px) {
  footer { padding: 1.3rem 1.25rem; align-items: flex-start; }
}
</style>
