<script setup lang="ts">
import { onMounted, ref } from "vue";
import { resolveMallEntry } from "@gmall/sdk";

const entry = ref<Record<string, unknown>>({});

onMounted(async () => {
  const response = await resolveMallEntry("PLATFORM_HOME");
  entry.value = response.data ?? {};
});
</script>

<template>
  <main class="pc-shell">
    <h1>gMall PC Showcase Shell</h1>
    <p>Platform home/topic/storefront home shell is ready.</p>
    <pre>{{ entry }}</pre>
  </main>
</template>

<style scoped>
.pc-shell {
  min-height: 100vh;
  padding: 40px;
  font-family: "Space Grotesk", "Segoe UI", sans-serif;
  color: #0f172a;
  background: radial-gradient(circle at top right, #dbeafe, #f8fafc 60%);
}
</style>
