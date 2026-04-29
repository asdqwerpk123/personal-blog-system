<template>
  <section class="author-page">
    <div class="page-heading">
      <div>
        <h1>作者首页</h1>
        <span>欢迎回来，{{ displayName }}</span>
      </div>
    </div>

    <div class="stats-grid">
      <article v-for="item in statCards" :key="item.label" class="stat-card">
        <span class="stat-card__icon" :class="item.tone">
          <component :is="item.icon" />
        </span>
        <div>
          <p>{{ item.label }}</p>
          <strong>{{ item.value }}</strong>
        </div>
      </article>
    </div>

    <div class="author-dashboard-grid">
      <section class="panel articles-panel">
        <header class="panel__header">
          <h2>最近文章</h2>
          <el-button link type="primary" @click="router.push('/author/articles')">查看全部</el-button>
        </header>
        <el-table v-loading="loading" class="article-table" :data="recentArticles" empty-text="暂无文章">
          <el-table-column label="标题" min-width="220" show-overflow-tooltip>
            <template #default="{ row }">
              <span class="article-title">{{ row.articleTitle || '-' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="分类" width="130">
            <template #default="{ row }">
              <span class="soft-tag">{{ row.categoryName || '-' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="120">
            <template #default="{ row }">
              <span class="status-pill" :class="statusMeta(row.articleStatus).className">
                {{ statusMeta(row.articleStatus).label }}
              </span>
            </template>
          </el-table-column>
          <el-table-column label="浏览量" width="100">
            <template #default="{ row }">{{ row.viewCount || 0 }}</template>
          </el-table-column>
          <el-table-column label="更新时间" width="170">
            <template #default="{ row }">{{ formatDate(row.updateTime || row.createTime) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="110" align="right">
            <template #default="{ row }">
              <div class="table-actions">
                <el-button link :icon="View" aria-label="查看文章" @click="router.push(`/author/articles/detail/${row.id}`)" />
                <el-button link :icon="EditPen" aria-label="编辑文章" @click="router.push(`/author/articles/edit/${row.id}`)" />
              </div>
            </template>
          </el-table-column>
        </el-table>
      </section>

      <aside class="side-stack">
        <section class="panel author-quick-panel">
          <header class="panel__header">
            <h2>快捷操作</h2>
          </header>
          <div class="quick-actions">
            <el-button class="primary-action-button" type="primary" :icon="Plus" @click="router.push('/author/articles/new')">新建文章</el-button>
            <el-button :icon="List" @click="router.push('/author/articles')">我的文章</el-button>
          </div>
        </section>

        <section class="panel overview-panel">
          <header class="panel__header">
            <h2>内容概览</h2>
          </header>
          <dl>
            <div>
              <dt>总浏览量</dt>
              <dd>{{ summary.totalViewCount || 0 }}</dd>
            </div>
            <div>
              <dt>本月更新</dt>
              <dd>{{ summary.monthUpdateCount || 0 }} 篇</dd>
            </div>
            <div>
              <dt>待审评论</dt>
              <dd>{{ summary.pendingCommentCount || 0 }} 条</dd>
            </div>
          </dl>
        </section>
      </aside>
    </div>
  </section>
</template>

<script setup>
import { Document, EditPen, List, Lock, Plus, View, CircleCheck, Edit } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import { computed, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';

import { getAuthorDashboardSummary } from '@/api/dashboard.js';
import { useAuthStore } from '@/stores/auth.js';

const router = useRouter();
const authStore = useAuthStore();
const loading = ref(false);
const summary = ref({});

const displayName = computed(() => authStore.nickName || authStore.userName || '作者');
const recentArticles = computed(() => summary.value.recentArticles || []);
const statCards = computed(() => [
  { label: '我的文章', value: summary.value.articleCount || 0, icon: Document, tone: 'tone-blue' },
  { label: '已发布', value: summary.value.publishedCount || 0, icon: CircleCheck, tone: 'tone-green' },
  { label: '草稿', value: summary.value.draftCount || 0, icon: Edit, tone: 'tone-orange' },
  { label: '私密文章', value: summary.value.privateCount || 0, icon: Lock, tone: 'tone-violet' }
]);

function extractData(response) {
  return response?.data ?? response ?? {};
}

function statusMeta(status) {
  if (status === 'PUBLISHED') {
    return { label: '已发布', className: '' };
  }
  if (status === 'PRIVATE' || status === 'OFFLINE') {
    return { label: '私密', className: 'private' };
  }
  return { label: '草稿', className: 'draft' };
}

function formatDate(value) {
  if (!value) {
    return '-';
  }
  return String(value).replace('T', ' ').replace(/\.\d+$/, '').slice(0, 16);
}

async function loadSummary() {
  loading.value = true;
  try {
    summary.value = extractData(await getAuthorDashboardSummary());
  } catch (error) {
    ElMessage.error(error.message || '作者首页数据加载失败');
  } finally {
    loading.value = false;
  }
}

onMounted(loadSummary);
</script>

<style scoped>
.author-dashboard-grid {
  display: grid;
  grid-template-columns: minmax(0, 2fr) minmax(300px, 0.9fr);
  gap: 20px;
}

.author-quick-panel,
.overview-panel {
  min-height: 170px;
}

.quick-actions {
  display: grid;
  gap: 14px;
  padding: 24px;
}

.quick-actions :deep(.el-button) {
  min-height: 48px;
  margin-left: 0;
  border-radius: 10px;
}

.overview-panel dl {
  margin: 0;
  padding: 20px 24px;
}

.overview-panel div {
  display: flex;
  justify-content: space-between;
  padding: 13px 0;
  border-bottom: 1px solid #f1f3f8;
}

.overview-panel div:last-child {
  border-bottom: 0;
}

.overview-panel dt {
  color: #697386;
}

.overview-panel dd {
  margin: 0;
  color: var(--color-primary);
  font-weight: 700;
}

.status-pill.private {
  color: #9333ea;
  background: #f6efff;
}

@media (max-width: 1120px) {
  .author-dashboard-grid {
    grid-template-columns: 1fr;
  }
}
</style>
