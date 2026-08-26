<template>
  <main
    class="not-found"
    aria-labelledby="not-found-title"
  >
    <p class="not-found__code">
      404
    </p>
    <h1 id="not-found-title">
      页面不存在
    </h1>
    <RouterLink to="/">
      返回首页
    </RouterLink>
  </main>
</template>

<script setup lang="ts">
import { RouterLink } from 'vue-router'
</script>

<style scoped>
.not-found {
  display: grid;
  min-height: 100vh;
  place-content: center;
  gap: 0.75rem;
  padding: 2rem;
  background: #f8fafc;
  color: #183142;
  text-align: center;
}

.not-found__code {
  margin: 0;
  font-size: clamp(4rem, 18vw, 9rem);
  font-weight: 800;
  line-height: 0.9;
}

h1 {
  margin: 0;
  font-size: 1.25rem;
  font-weight: 600;
}

a {
  color: inherit;
  text-underline-offset: 0.25em;
}
</style>
