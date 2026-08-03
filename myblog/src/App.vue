<template>
  <div id="app">
    <Navigation />
    <!-- <SkillProjectConnector /> -->
    <RouterView />
    <Footer />
    <ScrollSphere />
  </div>
</template>

<script setup lang="ts">
import { RouterView } from 'vue-router'
import Navigation from './components/Navigation.vue'
import Footer from './components/Footer.vue'
import ScrollSphere from './components/ScrollSphere.vue'
</script>
