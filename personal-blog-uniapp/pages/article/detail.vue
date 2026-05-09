<template>
  <view class="page detail-page">
    <LoadingState v-if="loading" loading />
    <EmptyState
      v-else-if="!article"
      title="文章不可访问"
      description="文章可能未发布或已删除"
      action-text="返回首页"
      @action="goHome"
    />
    <view v-else>
      <image class="cover" :src="coverUrl" mode="aspectFill" />
      <view class="article-panel">
        <text class="title">{{ article.articleTitle }}</text>
        <view class="meta">
          <text>{{ article.authorName || "匿名作者" }}</text>
          <text v-if="article.publishedTime"> · {{ formatDate(article.publishedTime) }}</text>
          <text v-if="article.viewCount !== undefined"> · {{ article.viewCount }} 阅读</text>
        </view>
        <view v-if="article.tags && article.tags.length" class="tag-row">
          <text v-for="tag in article.tags" :key="tag.id" class="tag">#{{ tag.tagName }}</text>
        </view>
        <rich-text class="content" :nodes="contentNodes" />
      </view>

      <view class="comment-panel">
        <view class="comment-title">评论</view>
        <CommentList :comments="comments" :loading="commentLoading" :finished="commentFinished" />
        <button
          v-if="!commentFinished && comments.length > 0"
          class="ghost-button load-comment"
          :disabled="commentLoading"
          @tap="loadComments(false)"
        >加载更多评论</button>
        <view class="comment-entry">
          <textarea
            v-model="commentContent"
            class="comment-input"
            maxlength="300"
            placeholder="写下你的评论"
          />
          <button class="primary-button" @tap="handleCommentTap">发表评论</button>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { computed, ref } from "vue"
import { onLoad } from "@dcloudio/uni-app"
import CommentList from "../../components/CommentList.vue"
import EmptyState from "../../components/EmptyState.vue"
import LoadingState from "../../components/LoadingState.vue"
import { getPublicArticle } from "../../api/article.js"
import { pageArticleComments } from "../../api/comment.js"
import { defaultImages, resolveAssetUrl } from "../../utils/config.js"
import { getToken } from "../../utils/request.js"

const articleId = ref(null)
const article = ref(null)
const loading = ref(false)
const comments = ref([])
const commentCurrent = ref(1)
const commentSize = 10
const commentTotal = ref(0)
const commentLoading = ref(false)
const commentFinished = ref(false)
const commentContent = ref("")

const coverUrl = computed(() => resolveAssetUrl(article.value && article.value.coverUrl, defaultImages.cover))
const contentNodes = computed(() => {
  const content = article.value && article.value.articleContent ? article.value.articleContent : ""
  return content.replace(/\n/g, "<br/>")
})

onLoad((options) => {
  articleId.value = Number(options.id)
  loadDetail()
})

async function loadDetail() {
  if (!articleId.value) {
    return
  }
  loading.value = true
  try {
    article.value = await getPublicArticle(articleId.value)
    await refreshComments()
  } catch (error) {
    article.value = null
  } finally {
    loading.value = false
  }
}

async function refreshComments() {
  commentCurrent.value = 1
  commentFinished.value = false
  comments.value = []
  await loadComments(true)
}

async function loadComments(reset) {
  if (!articleId.value || commentLoading.value) {
    return
  }
  commentLoading.value = true
  try {
    const page = await pageArticleComments(articleId.value, {
      current: commentCurrent.value,
      size: commentSize
    })
    const records = page && page.records ? page.records : []
    commentTotal.value = page && typeof page.total === "number" ? page.total : records.length
    comments.value = reset ? records : comments.value.concat(records)
    commentFinished.value = comments.value.length >= commentTotal.value || records.length < commentSize
    if (!commentFinished.value) {
      commentCurrent.value += 1
    }
  } finally {
    commentLoading.value = false
  }
}

function handleCommentTap() {
  if (!getToken()) {
    uni.navigateTo({ url: "/pages/auth/login" })
    return
  }
  uni.showToast({
    title: "发表评论将在 P2 完成",
    icon: "none"
  })
}

function goHome() {
  uni.switchTab({ url: "/pages/index/index" })
}

function formatDate(value) {
  return value ? String(value).replace("T", " ").slice(0, 16) : ""
}
</script>

<style scoped>
.cover {
  width: 100%;
  height: 360rpx;
  border-radius: 12rpx;
  background: #e6efe5;
}

.article-panel,
.comment-panel {
  margin-top: 24rpx;
  padding: 28rpx;
  border: 1rpx solid #dfe8dc;
  border-radius: 12rpx;
  background: #fff;
}

.title {
  display: block;
  color: #1f2b24;
  font-size: 42rpx;
  font-weight: 800;
  line-height: 1.3;
}

.meta {
  margin-top: 18rpx;
  color: #7a8678;
  font-size: 25rpx;
}

.tag-row {
  display: flex;
  flex-wrap: wrap;
  gap: 12rpx;
  margin-top: 20rpx;
}

.tag {
  padding: 7rpx 16rpx;
  border-radius: 999rpx;
  background: #edf4eb;
  color: #2f6f4e;
  font-size: 23rpx;
}

.content {
  display: block;
  margin-top: 28rpx;
  color: #334139;
  font-size: 30rpx;
  line-height: 1.75;
}

.comment-title {
  color: #1f2b24;
  font-size: 34rpx;
  font-weight: 700;
}

.load-comment {
  margin: 10rpx 0 24rpx;
}

.comment-entry {
  display: flex;
  gap: 16rpx;
  flex-direction: column;
  margin-top: 18rpx;
}

.comment-input {
  box-sizing: border-box;
  width: 100%;
  min-height: 160rpx;
  padding: 20rpx;
  border: 1rpx solid #d7dfd4;
  border-radius: 10rpx;
  background: #fbfdf9;
  color: #1f2b24;
  font-size: 28rpx;
}
</style>
