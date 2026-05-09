<template>
  <view class="comment-list">
    <EmptyState
      v-if="!loading && (!comments || comments.length === 0)"
      title="暂无评论"
      description="读完文章后，可以稍后回来参与讨论"
    />
    <view v-for="comment in comments" :key="comment.id" class="comment-item">
      <image class="avatar" :src="avatarUrl(comment.avatarUrl)" mode="aspectFill" />
      <view class="comment-body">
        <view class="comment-head">
          <text class="nickname">{{ comment.nickName || "匿名用户" }}</text>
          <text class="time">{{ formatDate(comment.createTime) }}</text>
        </view>
        <text class="content">{{ comment.commentContent }}</text>
      </view>
    </view>
    <LoadingState :loading="loading" :finished="finished && comments && comments.length > 0" />
  </view>
</template>

<script setup>
import EmptyState from "./EmptyState.vue"
import LoadingState from "./LoadingState.vue"
import { defaultImages, resolveAssetUrl } from "../utils/config.js"

defineProps({
  comments: {
    type: Array,
    default: () => []
  },
  loading: {
    type: Boolean,
    default: false
  },
  finished: {
    type: Boolean,
    default: false
  }
})

function avatarUrl(url) {
  return resolveAssetUrl(url, defaultImages.avatar)
}

function formatDate(value) {
  if (!value) {
    return ""
  }
  return String(value).replace("T", " ").slice(0, 16)
}
</script>

<style scoped>
.comment-list {
  padding-bottom: 24rpx;
}

.comment-item {
  display: flex;
  gap: 18rpx;
  padding: 24rpx 0;
  border-bottom: 1rpx solid #e5ece3;
}

.avatar {
  width: 72rpx;
  height: 72rpx;
  border-radius: 50%;
  background: #edf4eb;
  flex-shrink: 0;
}

.comment-body {
  flex: 1;
  min-width: 0;
}

.comment-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16rpx;
}

.nickname {
  color: #1f2b24;
  font-size: 27rpx;
  font-weight: 600;
}

.time {
  color: #8a9589;
  font-size: 22rpx;
  flex-shrink: 0;
}

.content {
  display: block;
  margin-top: 10rpx;
  color: #46564a;
  font-size: 27rpx;
  line-height: 1.55;
}
</style>
