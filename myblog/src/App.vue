<template>
  <RouterView v-if="route.meta.standalone" />
  <div
    v-else
    id="app"
  >
    <Navigation />
    <RouterView />
    <Footer />
    <ScrollSphere />
  </div>
</template>

<script setup lang="ts">
import { watch } from 'vue'
import { RouterView } from 'vue-router'
import { useRoute } from 'vue-router'
import Navigation from './components/Navigation.vue'
import Footer from './components/Footer.vue'
import ScrollSphere from './components/ScrollSphere.vue'
import { registerSiteVisit } from './composables/useSiteStatistics'

const route = useRoute()

watch(
  () => route.path,
  () => void registerSiteVisit(),
  { immediate: true },
)
</script>
