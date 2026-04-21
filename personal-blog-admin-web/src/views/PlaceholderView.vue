<template>
  <section class="placeholder-page">
    <h1>{{ route.meta.title }}</h1>
    <div class="placeholder-box">
      <span>{{ route.meta.title?.slice(0, 1) }}</span>
      <p>第一阶段暂未开放</p>
    </div>
  </section>
</template>

<script setup>
import { useRoute } from 'vue-router';

const route = useRoute();
</script>
