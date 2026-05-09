<template>
  <view class="empty-state">
    <image class="empty-image" :src="image" mode="aspectFit" />
    <text class="empty-title">{{ title }}</text>
    <text v-if="description" class="empty-description">{{ description }}</text>
    <button v-if="actionText" class="empty-action" @tap="$emit('action')">{{ actionText }}</button>
  </view>
</template>

<script setup>
import { defaultImages } from "../utils/config.js"

defineProps({
  title: {
    type: String,
    default: "暂无数据"
  },
  description: {
    type: String,
    default: ""
  },
  actionText: {
    type: String,
    default: ""
  },
  image: {
    type: String,
    default: defaultImages.empty
  }
})

defineEmits(["action"])
</script>

<style scoped>
.empty-state {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 360rpx;
  padding: 48rpx 32rpx;
  flex-direction: column;
  text-align: center;
}

.empty-image {
  width: 220rpx;
  height: 160rpx;
}

.empty-title {
  margin-top: 20rpx;
  color: #1f2b24;
  font-size: 30rpx;
  font-weight: 600;
}

.empty-description {
  margin-top: 10rpx;
  color: #6c7668;
  font-size: 25rpx;
  line-height: 1.5;
}

.empty-action {
  margin-top: 24rpx;
  padding: 0 34rpx;
  border-radius: 10rpx;
  background: #2f6f4e;
  color: #fff;
  font-size: 26rpx;
}
</style>
