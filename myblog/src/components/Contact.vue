<template>
  <section id="contact">
    <div class="container">
      <RevealOnScroll>
        <div class="section-header-centered">
          <h2 class="section-title">合作与<em>联系</em></h2>
          <p class="section-desc">
            我与来自不同领域的人合作，这正是我创建这个网站的原因。欢迎联系我，一起探索更多可能性。
          </p>
        </div>
      </RevealOnScroll>

      <RevealOnScroll :delay="1">
        <div class="contact-links">
          <a href="#" class="contact-link">GitHub</a>
          <a href="#" class="contact-link">Twitter</a>
          <a href="#" class="contact-link">Email</a>
          <a href="#" class="contact-link">Bilibili</a>
          <a href="#" class="contact-link">YouTube</a>
        </div>
      </RevealOnScroll>
    </div>
  </section>
</template>

<script setup lang="ts">
import RevealOnScroll from './ui/RevealOnScroll.vue'
</script>

<style scoped>
#contact {
  background: linear-gradient(180deg, #0D1B2A 0%, #1B4965 100%);
  padding: 100px 0;
  text-align: center;
}

#contact .section-title {
  color: var(--bg);
}

#contact .section-desc {
  color: rgba(244, 240, 235, 0.55);
}

.container {
  max-width: var(--max-w);
  margin: 0 auto;
  padding: 0 3rem;
}

.section-header-centered {
  margin-bottom: 0;
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
  max-width: 480px;
  font-weight: 300;
  line-height: 1.8;
  margin: 0 auto;
}

.contact-links {
  display: flex;
  justify-content: center;
  gap: 1rem;
  flex-wrap: wrap;
  margin-top: 2.5rem;
}

.contact-link {
  font-family: var(--font-mono);
  font-size: 0.72rem;
  letter-spacing: 0.1em;
  text-transform: uppercase;
  padding: 0.7rem 1.5rem;
  border: 1px solid rgba(91, 164, 230, 0.4);
  color: rgba(255, 255, 255, 0.9);
  transition: border-color 0.2s, background 0.2s, box-shadow 0.2s;
}

.contact-link:hover {
  border-color: var(--accent);
  background: rgba(91, 164, 230, 0.2);
  box-shadow: 0 0 20px rgba(91, 164, 230, 0.3);
}
</style>
