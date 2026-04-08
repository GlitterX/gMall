<script setup lang="ts">
import { onMounted, ref } from "vue";
import { resolveMallEntry } from "@gmall/sdk";

const mobileEntry = ref<Record<string, unknown>>({});

onMounted(async () => {
  const response = await resolveMallEntry("STOREFRONT_HOME");
  mobileEntry.value = response.data ?? {};
});
</script>

<template>
  <main class="mobile-shell">
    <h1>gMall UniApp Shell</h1>
    <p>H5/mini-program container scaffold is ready.</p>
    <pre>{{ mobileEntry }}</pre>
  </main>
</template>

<style scoped>
.mobile-shell {
  min-height: 100vh;
  padding: 20px;
  color: #111827;
  font-family: "Noto Sans", "PingFang SC", sans-serif;
  background: linear-gradient(180deg, #fef3c7, #fff7ed);
}
</style>
