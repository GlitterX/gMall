<script setup lang="ts">
import { onMounted, ref } from "vue";
import { getAdminWorkbenchSummary } from "@gmall/sdk";

const summary = ref<Record<string, unknown>>({});

onMounted(async () => {
  const response = await getAdminWorkbenchSummary();
  summary.value = response.data ?? {};
});
</script>

<template>
  <article>
    <h2>Platform Dashboard</h2>
    <pre>{{ summary }}</pre>
  </article>
</template>
