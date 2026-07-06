<template>
  <div id="app">
    <Navigation />
    <!-- <SkillProjectConnector /> -->
    <main>
      <HeroCinema />
      <div class="page-divider">
        <div class="divider-line" />
      </div>
      <Hero id="hero-intro" />
      <About />
      <Skills />
      <Projects />
      <Hobbies />
      <Games />
      <VibeCoding />
      <Contact />
    </main>
    <Footer />
  </div>
</template>

<script setup lang="ts">
import Navigation from './components/Navigation.vue'
// import SkillProjectConnector from './components/ui/SkillProjectConnector.vue'
import HeroCinema from './components/HeroCinema.vue'
import Hero from './components/Hero.vue'
import About from './components/About.vue'
import Skills from './components/Skills.vue'
import Projects from './components/Projects.vue'
import Hobbies from './components/Hobbies.vue'
import Games from './components/Games.vue'
import VibeCoding from './components/VibeCoding.vue'
import Contact from './components/Contact.vue'
import Footer from './components/Footer.vue'
</script>

<style>
/* Page divider */
.page-divider {
  padding: 0 3rem;
  margin-bottom: 0;
}

.divider-line {
  height: 1px;
  background: linear-gradient(
    to right,
    transparent 0%,
    var(--border) 10%,
    var(--border) 90%,
    transparent 100%
  );
}
</style>
