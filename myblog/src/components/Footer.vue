<template>
  <footer>
    <span class="copy">&copy; 2026 游牧人 · 数字叙事者</span>
    <span class="back-top" @click="scrollToTop">↑ 回到顶部</span>
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
  border-top: 1px solid var(--border);
}

.copy {
  font-family: var(--font-mono);
  font-size: 0.65rem;
  letter-spacing: 0.08em;
  color: var(--ink-muted);
}

.back-top {
  font-family: var(--font-mono);
  font-size: 0.65rem;
  letter-spacing: 0.1em;
  text-transform: uppercase;
  color: var(--ink-muted);
  cursor: pointer;
  transition: color 0.2s;
}

.back-top:hover {
  color: var(--accent);
}
</style>
