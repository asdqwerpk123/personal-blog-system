<template>
  <view class="page index-page">
    <view class="search-bar">
      <input
        v-model="keyword"
        class="search-input"
        confirm-type="search"
        placeholder="搜索文章标题或摘要"
        @confirm="refreshArticles"
      />
      <button class="search-button" @tap="refreshArticles">搜索</button>
    </view>

    <scroll-view class="filter-scroll" scroll-x>
      <view class="filter-row">
        <text
          class="filter-chip"
          :class="{ active: !selectedCategoryId }"
          @tap="selectCategory(null)"
        >全部分类</text>
        <text
          v-for="category in categories"
          :key="category.id"
          class="filter-chip"
          :class="{ active: selectedCategoryId === category.id }"
          @tap="selectCategory(category.id)"
        >{{ category.categoryName }}</text>
      </view>
    </scroll-view>

    <scroll-view class="filter-scroll" scroll-x>
      <view class="filter-row">
        <text
          class="filter-chip secondary"
          :class="{ active: !selectedTagId }"
          @tap="selectTag(null)"
        >全部标签</text>
        <text
          v-for="tag in tags"
          :key="tag.id"
          class="filter-chip secondary"
          :class="{ active: selectedTagId === tag.id }"
          @tap="selectTag(tag.id)"
        >#{{ tag.tagName }}</text>
      </view>
    </scroll-view>

    <LoadingState v-if="loading && articles.length === 0" loading />
    <EmptyState
      v-else-if="!loading && articles.length === 0"
      title="暂无文章"
      description="换个关键词或筛选条件试试"
      action-text="清除筛选"
      @action="clearFilters"
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
import { onLoad, onPullDownRefresh, onReachBottom } from "@dcloudio/uni-app"
import ArticleCard from "../../components/ArticleCard.vue"
import EmptyState from "../../components/EmptyState.vue"
import LoadingState from "../../components/LoadingState.vue"
import { pagePublicArticles } from "../../api/article.js"
import { listPublicCategories } from "../../api/category.js"
import { listPublicTags } from "../../api/tag.js"

const keyword = ref("")
const selectedCategoryId = ref(null)
const selectedTagId = ref(null)
const categories = ref([])
const tags = ref([])
const articles = ref([])
const current = ref(1)
const size = 10
const total = ref(0)
const loading = ref(false)
const loadingMore = ref(false)
const finished = ref(false)

onLoad((options) => {
  selectedCategoryId.value = normalizeId(options.categoryId)
  selectedTagId.value = normalizeId(options.tagId)
  loadFilters()
  refreshArticles()
})

onPullDownRefresh(async () => {
  try {
    await refreshArticles()
  } finally {
    uni.stopPullDownRefresh()
  }
})

onReachBottom(() => {
  if (!finished.value && !loading.value && !loadingMore.value) {
    loadArticles(false)
  }
})

async function loadFilters() {
  try {
    const [categoryList, tagList] = await Promise.all([
      listPublicCategories(),
      listPublicTags()
    ])
    categories.value = categoryList || []
    tags.value = tagList || []
  } catch (error) {
    // request.js already shows the backend message.
  }
}

async function refreshArticles() {
  current.value = 1
  finished.value = false
  await loadArticles(true)
}

async function loadArticles(reset) {
  if (reset) {
    loading.value = true
  } else {
    loadingMore.value = true
  }

  try {
    const page = await pagePublicArticles({
      current: current.value,
      size,
      keyword: keyword.value,
      categoryId: selectedCategoryId.value || undefined,
      tagId: selectedTagId.value || undefined
    })
    const records = page && page.records ? page.records : []
    total.value = page && typeof page.total === "number" ? page.total : records.length
    articles.value = reset ? records : articles.value.concat(records)
    finished.value = articles.value.length >= total.value || records.length < size
    if (!finished.value) {
      current.value += 1
    }
  } finally {
    loading.value = false
    loadingMore.value = false
  }
}

function selectCategory(id) {
  selectedCategoryId.value = id
  refreshArticles()
}

function selectTag(id) {
  selectedTagId.value = id
  refreshArticles()
}

function clearFilters() {
  keyword.value = ""
  selectedCategoryId.value = null
  selectedTagId.value = null
  refreshArticles()
}

function openArticle(article) {
  uni.navigateTo({
    url: `/pages/article/detail?id=${article.id}`
  })
}

function normalizeId(value) {
  const numberValue = Number(value)
  return Number.isFinite(numberValue) && numberValue > 0 ? numberValue : null
}
</script>

<style scoped>
.index-page {
  padding-top: 20rpx;
}

.search-bar {
  display: flex;
  gap: 16rpx;
  margin-bottom: 20rpx;
}

.search-input {
  flex: 1;
  min-width: 0;
  height: 78rpx;
  padding: 0 22rpx;
  border: 1rpx solid #d7dfd4;
  border-radius: 10rpx;
  background: #fff;
  font-size: 27rpx;
}

.search-button {
  width: 128rpx;
  height: 78rpx;
  margin: 0;
  border-radius: 10rpx;
  background: #2f6f4e;
  color: #fff;
  font-size: 27rpx;
  line-height: 78rpx;
}

.filter-scroll {
  margin-bottom: 16rpx;
  white-space: nowrap;
}

.filter-row {
  display: inline-flex;
  gap: 14rpx;
  min-width: 100%;
}

.filter-chip {
  display: inline-flex;
  align-items: center;
  height: 58rpx;
  padding: 0 22rpx;
  border: 1rpx solid #d7dfd4;
  border-radius: 999rpx;
  background: #fff;
  color: #4f5f53;
  font-size: 25rpx;
}

.filter-chip.secondary {
  background: #fbfaf4;
}

.filter-chip.active {
  border-color: #2f6f4e;
  background: #2f6f4e;
  color: #fff;
}
</style>
