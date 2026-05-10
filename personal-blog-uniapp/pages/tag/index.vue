<template>
  <view class="page">
    <view class="section-title">标签</view>
    <LoadingState v-if="loading" loading />
    <EmptyState v-else-if="tags.length === 0" title="暂无标签" />
    <view v-else class="tag-cloud">
      <text
        v-for="tag in tags"
        :key="tag.id"
        class="tag-chip"
        :class="{ active: selectedTagId === tag.id }"
        @tap="selectTag(tag)"
      >#{{ tag.tagName }}</text>
    </view>

    <view class="section-title">标签文章</view>
    <LoadingState v-if="loadingArticles && articles.length === 0" loading />
    <EmptyState
      v-else-if="!loadingArticles && articles.length === 0"
      title="暂无文章"
      description="选择其他标签看看"
    />
    <template v-else>
      <ArticleCard
        v-for="article in articles"
        :key="article.id"
        :article="article"
        @open="openArticle"
      />
    </template>
    <LoadingState :loading="loadingMore" :finished="finished && articles.length > 0" />
  </view>
</template>

<script setup>
import { ref } from "vue"
import { onLoad, onReachBottom } from "@dcloudio/uni-app"
import ArticleCard from "../../components/ArticleCard.vue"
import EmptyState from "../../components/EmptyState.vue"
import LoadingState from "../../components/LoadingState.vue"
import { pagePublicArticles } from "../../api/article.js"
import { listPublicTags } from "../../api/tag.js"

const tags = ref([])
const selectedTagId = ref(null)
const articles = ref([])
const current = ref(1)
const size = 20
const total = ref(0)
const loading = ref(false)
const loadingArticles = ref(false)
const loadingMore = ref(false)
const finished = ref(false)
const articleRequestSeq = ref(0)

onLoad(async () => {
  await loadTags()
  if (tags.value.length) {
    selectTag(tags.value[0])
  }
})

onReachBottom(() => {
  if (!finished.value && !loadingArticles.value && !loadingMore.value) {
    loadArticles(false)
  }
})

async function loadTags() {
  loading.value = true
  try {
    tags.value = await listPublicTags() || []
  } finally {
    loading.value = false
  }
}

async function selectTag(tag) {
  selectedTagId.value = tag.id
  const requestId = ++articleRequestSeq.value
  current.value = 1
  finished.value = false
  await loadArticles(true, requestId)
}

async function loadArticles(reset, requestId = articleRequestSeq.value) {
  if (!selectedTagId.value) {
    return
  }
  if (!reset && (loadingArticles.value || loadingMore.value)) {
    return
  }

  const filters = {
    current: reset ? 1 : current.value,
    tagId: selectedTagId.value
  }

  if (reset) {
    loadingArticles.value = true
  } else {
    loadingMore.value = true
  }
  try {
    const page = await pagePublicArticles({
      current: filters.current,
      size,
      tagId: filters.tagId
    })
    if (requestId !== articleRequestSeq.value) {
      return
    }
    const records = page && page.records ? page.records : []
    total.value = page && typeof page.total === "number" ? page.total : records.length
    articles.value = reset ? records : articles.value.concat(records)
    finished.value = articles.value.length >= total.value || records.length < size
    if (!finished.value) {
      current.value += 1
    }
  } finally {
    if (requestId === articleRequestSeq.value) {
      loadingArticles.value = false
      loadingMore.value = false
    }
  }
}

function openArticle(article) {
  uni.navigateTo({
    url: `/pages/article/detail?id=${article.id}`
  })
}
</script>

<style scoped>
.tag-cloud {
  display: flex;
  flex-wrap: wrap;
  gap: 14rpx;
}

.tag-chip {
  padding: 14rpx 22rpx;
  border: 1rpx solid #d7dfd4;
  border-radius: 999rpx;
  background: #fff;
  color: #4f5f53;
  font-size: 26rpx;
}

.tag-chip.active {
  border-color: #d6903d;
  background: #fff6e8;
  color: #8a5516;
}
</style>
