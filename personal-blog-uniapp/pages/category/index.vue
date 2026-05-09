<template>
  <view class="page">
    <view class="section-title">分类</view>
    <LoadingState v-if="loadingCategories" loading />
    <EmptyState v-else-if="categories.length === 0" title="暂无分类" />
    <view v-else class="category-grid">
      <view
        v-for="category in categories"
        :key="category.id"
        class="category-item"
        :class="{ active: selectedCategoryId === category.id }"
        @tap="selectCategory(category)"
      >
        <text class="category-name">{{ category.categoryName }}</text>
        <text class="category-desc">{{ category.description || "暂无描述" }}</text>
      </view>
    </view>

    <view class="section-title">分类文章</view>
    <LoadingState v-if="loadingArticles && articles.length === 0" loading />
    <EmptyState
      v-else-if="!loadingArticles && articles.length === 0"
      title="暂无文章"
      description="请选择分类或稍后再看"
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
import { listPublicCategories } from "../../api/category.js"

const categories = ref([])
const selectedCategoryId = ref(null)
const articles = ref([])
const current = ref(1)
const size = 10
const total = ref(0)
const loadingCategories = ref(false)
const loadingArticles = ref(false)
const loadingMore = ref(false)
const finished = ref(false)

onLoad(async () => {
  await loadCategories()
  if (categories.value.length) {
    selectCategory(categories.value[0])
  }
})

onReachBottom(() => {
  if (!finished.value && !loadingArticles.value && !loadingMore.value) {
    loadArticles(false)
  }
})

async function loadCategories() {
  loadingCategories.value = true
  try {
    categories.value = await listPublicCategories() || []
  } finally {
    loadingCategories.value = false
  }
}

function selectCategory(category) {
  selectedCategoryId.value = category.id
  current.value = 1
  finished.value = false
  loadArticles(true)
}

async function loadArticles(reset) {
  if (!selectedCategoryId.value) {
    return
  }
  if (reset) {
    loadingArticles.value = true
  } else {
    loadingMore.value = true
  }
  try {
    const page = await pagePublicArticles({
      current: current.value,
      size,
      categoryId: selectedCategoryId.value
    })
    const records = page && page.records ? page.records : []
    total.value = page && typeof page.total === "number" ? page.total : records.length
    articles.value = reset ? records : articles.value.concat(records)
    finished.value = articles.value.length >= total.value || records.length < size
    if (!finished.value) {
      current.value += 1
    }
  } finally {
    loadingArticles.value = false
    loadingMore.value = false
  }
}

function openArticle(article) {
  uni.navigateTo({
    url: `/pages/article/detail?id=${article.id}`
  })
}
</script>

<style scoped>
.category-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18rpx;
}

.category-item {
  min-height: 146rpx;
  padding: 22rpx;
  border: 1rpx solid #dfe8dc;
  border-radius: 12rpx;
  background: #fff;
  box-sizing: border-box;
}

.category-item.active {
  border-color: #2f6f4e;
  background: #edf4eb;
}

.category-name {
  display: block;
  color: #1f2b24;
  font-size: 30rpx;
  font-weight: 700;
}

.category-desc {
  display: -webkit-box;
  overflow: hidden;
  margin-top: 10rpx;
  color: #68766a;
  font-size: 24rpx;
  line-height: 1.4;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}
</style>
