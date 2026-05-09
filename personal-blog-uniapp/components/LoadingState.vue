<template>
  <view class="loading-state">
    <text v-if="loading">加载中...</text>
    <text v-else-if="finished">没有更多了</text>
  </view>
</template>

<script setup>
defineProps({
  loading: {
    type: Boolean,
    default: false
  },
  finished: {
    type: Boolean,
    default: false
  }
})
</script>

<style scoped>
.loading-state {
  min-height: 76rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #7a8678;
  font-size: 24rpx;
}
</style>
