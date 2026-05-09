<template>
  <view class="page comments-page">
    <view class="toolbar">
      <text class="section-title">我的评论</text>
      <button class="ghost-button refresh-button" :disabled="loading" @tap="refresh">刷新</button>
    </view>

    <EmptyState
      v-if="!loading && comments.length === 0"
      title="暂无评论"
      description="你发表过的评论会显示在这里"
    />

    <view v-for="comment in comments" :key="comment.id" class="comment-card">
      <view class="comment-head">
        <text class="article-title">{{ comment.articleTitle || "文章" }}</text>
        <text class="status" :class="statusClass(comment.commentStatus)">
          {{ statusText(comment.commentStatus) }}
        </text>
      </view>
      <text class="content">{{ comment.commentContent }}</text>
      <view class="meta-row">
        <text>{{ formatDate(comment.createTime) }}</text>
        <button class="delete-button" @tap="confirmDelete(comment.id)">删除</button>
      </view>
    </view>

    <LoadingState :loading="loading" :finished="finished && comments.length > 0" />
  </view>
</template>

<script setup>
import { ref } from "vue"
import { onReachBottom, onShow } from "@dcloudio/uni-app"
import EmptyState from "../../components/EmptyState.vue"
import LoadingState from "../../components/LoadingState.vue"
import { deleteUserComment, pageUserComments } from "../../api/comment.js"
import { buildLoginUrl, getCurrentPageUrl, isUserLoggedIn } from "../../utils/request.js"

const comments = ref([])
const current = ref(1)
const size = 10
const total = ref(0)
const loading = ref(false)
const finished = ref(false)

onShow(() => {
  if (!isUserLoggedIn()) {
    redirectToLogin()
    return
  }
  refresh()
})

onReachBottom(() => {
  if (!finished.value) {
    loadComments(false)
  }
})

function redirectToLogin() {
  uni.redirectTo({ url: buildLoginUrl(getCurrentPageUrl()) })
}

async function refresh() {
  current.value = 1
  finished.value = false
  comments.value = []
  await loadComments(true)
}

async function loadComments(reset) {
  if (loading.value || !isUserLoggedIn()) {
    return
  }
  loading.value = true
  try {
    const page = await pageUserComments({
      current: current.value,
      size
    })
    const records = page && page.records ? page.records : []
    total.value = page && typeof page.total === "number" ? page.total : records.length
    comments.value = reset ? records : comments.value.concat(records)
    finished.value = comments.value.length >= total.value || records.length < size
    if (!finished.value) {
      current.value += 1
    }
  } finally {
    loading.value = false
  }
}

function confirmDelete(id) {
  uni.showModal({
    title: "删除评论",
    content: "确认删除这条评论吗？",
    success: async (result) => {
      if (result.confirm) {
        await deleteComment(id)
      }
    }
  })
}

async function deleteComment(id) {
  await deleteUserComment(id)
  uni.showToast({ title: "评论已删除", icon: "success" })
  await refresh()
}

function statusText(status) {
  const map = {
    PENDING: "待审核",
    APPROVED: "已通过",
    REJECTED: "未通过"
  }
  return map[status] || status || "-"
}

function statusClass(status) {
  return {
    approved: status === "APPROVED",
    rejected: status === "REJECTED"
  }
}

function formatDate(value) {
  return value ? String(value).replace("T", " ").slice(0, 16) : ""
}
</script>

<style scoped>
.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20rpx;
}

.refresh-button {
  min-height: 64rpx;
  padding: 0 28rpx;
  font-size: 24rpx;
}

.comment-card {
  margin-top: 20rpx;
  padding: 26rpx;
  border: 1rpx solid #dfe8dc;
  border-radius: 12rpx;
  background: #fff;
}

.comment-head,
.meta-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18rpx;
}

.article-title {
  min-width: 0;
  color: #1f2b24;
  font-size: 29rpx;
  font-weight: 700;
}

.status {
  padding: 6rpx 14rpx;
  border-radius: 999rpx;
  background: #f4f0e8;
  color: #7a6c45;
  font-size: 22rpx;
  flex-shrink: 0;
}

.status.approved {
  background: #edf4eb;
  color: #2f6f4e;
}

.status.rejected {
  background: #f8e9e7;
  color: #a54035;
}

.content {
  display: block;
  margin-top: 16rpx;
  color: #46564a;
  font-size: 28rpx;
  line-height: 1.55;
}

.meta-row {
  margin-top: 20rpx;
  color: #8a9589;
  font-size: 24rpx;
}

.delete-button {
  margin: 0;
  padding: 0 22rpx;
  min-height: 56rpx;
  border: 1rpx solid #f0cbc6;
  border-radius: 8rpx;
  background: #fff;
  color: #a54035;
  font-size: 23rpx;
}
</style>
