<template>
  <view class="article-card" @tap="$emit('open', article)">
    <image class="article-cover" :src="coverUrl" mode="aspectFill" />
    <view class="article-body">
      <view class="article-title">{{ article.articleTitle }}</view>
      <view class="article-summary">{{ article.articleSummary || "暂无摘要" }}</view>
      <view class="article-meta">
        <text>{{ article.authorName || "匿名作者" }}</text>
        <text v-if="article.categoryName"> · {{ article.categoryName }}</text>
        <text v-if="publishedText"> · {{ publishedText }}</text>
      </view>
      <view v-if="article.tags && article.tags.length" class="tag-row">
        <text v-for="tag in article.tags" :key="tag.id" class="tag">#{{ tag.tagName }}</text>
      </view>
    </view>
  </view>
</template>

<script setup>
import { computed } from "vue"
import { defaultImages, resolveAssetUrl } from "../utils/config.js"

const props = defineProps({
  article: {
    type: Object,
    required: true
  }
})

defineEmits(["open"])

const coverUrl = computed(() => resolveAssetUrl(props.article.coverUrl, defaultImages.cover))
const publishedText = computed(() => formatDate(props.article.publishedTime))

function formatDate(value) {
  if (!value) {
    return ""
  }
  return String(value).replace("T", " ").slice(0, 16)
}
</script>

<style scoped>
.article-card {
  overflow: hidden;
  margin-bottom: 24rpx;
  border: 1rpx solid #dfe8dc;
  border-radius: 12rpx;
  background: #fff;
}

.article-cover {
  width: 100%;
  height: 300rpx;
  background: #e6efe5;
}

.article-body {
  padding: 24rpx;
}

.article-title {
  color: #1f2b24;
  font-size: 34rpx;
  font-weight: 700;
  line-height: 1.35;
}

.article-summary {
  display: -webkit-box;
  overflow: hidden;
  margin-top: 14rpx;
  color: #4f5f53;
  font-size: 27rpx;
  line-height: 1.55;
  text-overflow: ellipsis;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.article-meta {
  margin-top: 18rpx;
  color: #7a8678;
  font-size: 24rpx;
}

.tag-row {
  display: flex;
  flex-wrap: wrap;
  gap: 10rpx;
  margin-top: 18rpx;
}

.tag {
  padding: 6rpx 14rpx;
  border-radius: 999rpx;
  background: #edf4eb;
  color: #2f6f4e;
  font-size: 22rpx;
}
</style>
