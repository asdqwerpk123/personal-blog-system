<template>
  <div class="dashboard-page">
    <div class="page-heading">
      <h1>仪表盘</h1>
      <span>最后更新: {{ lastUpdatedLabel }}</span>
    </div>

    <section class="stats-grid" aria-label="数据概览">
      <article v-for="stat in dashboardStats" :key="stat.label" class="stat-card">
        <span class="stat-card__icon" :class="`tone-${stat.tone}`">
          <el-icon>
            <component :is="statIconMap[stat.icon]" />
          </el-icon>
        </span>
        <div>
          <p>{{ stat.label }}</p>
          <strong>{{ stat.value }}</strong>
        </div>
      </article>
    </section>

    <section class="dashboard-grid">
      <article class="panel articles-panel">
        <div class="panel__header">
          <h2>最新文章</h2>
          <el-button link type="primary">查看全部</el-button>
        </div>

        <el-table class="article-table" :data="latestArticles" table-layout="fixed">
          <el-table-column label="标题" min-width="245">
            <template #default="{ row }">
              <div class="article-title">{{ row.title }}</div>
              <span class="article-date">{{ row.date }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="category" label="分类" width="130">
            <template #default="{ row }">
              <span class="soft-tag">{{ row.category }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="views" label="浏览量" width="110" />
          <el-table-column label="状态" width="120">
            <template #default="{ row }">
              <span class="status-pill" :class="{ draft: row.status === '草稿' }">
                {{ row.status }}
              </span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="110" align="right">
            <template #default>
              <div class="table-actions">
                <el-button link :icon="View" aria-label="预览" />
                <el-button link :icon="EditPen" aria-label="编辑" />
              </div>
            </template>
          </el-table-column>
        </el-table>
      </article>

      <aside class="side-stack">
        <article class="panel comments-panel">
          <div class="panel__header">
            <h2>最新评论</h2>
          </div>

          <ul class="comment-list">
            <li v-for="comment in latestComments" :key="`${comment.author}-${comment.time}`">
              <div class="comment-head">
                <span class="comment-avatar">{{ comment.avatar }}</span>
                <div>
                  <strong>{{ comment.author }}</strong>
                  <p>{{ comment.time }}</p>
                </div>
                <span v-if="comment.status" class="audit-tag">{{ comment.status }}</span>
              </div>
              <blockquote>"{{ comment.content }}"</blockquote>
              <p class="comment-article">评于: {{ comment.article }}</p>
            </li>
          </ul>

          <button class="panel-link" type="button">查看所有评论</button>
        </article>

        <article class="panel logs-panel">
          <div class="panel__header">
            <h2>操作日志</h2>
          </div>

          <ul class="log-list">
            <li v-for="log in operationLogs" :key="`${log.user}-${log.time}`">
              <div>
                <p>
                  <strong>{{ log.user }}</strong>
                  {{ log.action }}
                </p>
                <span>{{ log.time }} · {{ log.ip }}</span>
              </div>
              <i :class="log.state"></i>
            </li>
          </ul>
        </article>
      </aside>
    </section>
  </div>
</template>

<script setup>
import { ChatLineSquare, Clock, Connection, Document, EditPen, View } from '@element-plus/icons-vue';
import { computed, onMounted, ref } from 'vue';

import { getDashboardSummary } from '@/api/dashboard.js';
import { unwrapData } from '@/views/admin/pageData.js';

const statIconMap = {
  ChatLineSquare,
  Clock,
  Connection,
  Document
};

const summary = ref({
  articleCount: 0,
  categoryCount: 0,
  tagCount: 0,
  commentCount: 0,
  pendingCommentCount: 0,
  friendLinkCount: 0,
  latestArticles: [],
  latestComments: [],
  latestOperationLogs: [],
  latestLogs: []
});
const lastUpdatedAt = ref(null);

const dashboardStats = computed(() => [
  {
    label: '文章总数',
    value: Number(summary.value.articleCount || 0).toLocaleString(),
    icon: 'Document',
    tone: 'blue'
  },
  {
    label: '分类/标签',
    value: `${Number(summary.value.categoryCount || 0)} / ${Number(summary.value.tagCount || 0)}`,
    icon: 'Connection',
    tone: 'green'
  },
  {
    label: '评论总数',
    value: Number(summary.value.commentCount || 0).toLocaleString(),
    icon: 'ChatLineSquare',
    tone: 'orange'
  },
  {
    label: '待审核',
    value: Number(summary.value.pendingCommentCount || 0).toLocaleString(),
    icon: 'Clock',
    tone: 'red'
  }
]);

const latestArticles = computed(() => (summary.value.latestArticles || []).map((article) => ({
  title: article.articleTitle || article.title || '-',
  date: formatTime(article.updateTime || article.createTime || article.publishedTime),
  category: article.categoryName || (article.categoryId ? `分类 ${article.categoryId}` : '-'),
  views: article.viewCount ?? 0,
  status: articleStatusLabel(article.articleStatus || article.status)
})));

const latestComments = computed(() => (summary.value.latestComments || []).map((comment) => {
  const author = comment.nickName || comment.userName || (comment.userId ? `用户 ${comment.userId}` : '访客');

  return {
    author,
    avatar: author.slice(0, 1).toUpperCase(),
    time: formatTime(comment.updateTime || comment.createTime),
    content: comment.commentContent || comment.content || '-',
    article: comment.articleTitle || (comment.articleId ? `文章 ${comment.articleId}` : '-'),
    status: commentStatusLabel(comment.commentStatus || comment.status)
  };
}));

const operationLogs = computed(() => (summary.value.latestOperationLogs || summary.value.latestLogs || []).map((log) => ({
  user: log.operatorUserName || (log.operatorUserId ? `用户 ${log.operatorUserId}` : '系统'),
  action: log.actionDetail || `${log.actionType || '-'} ${log.targetType || ''}`.trim(),
  time: formatTime(log.createTime || log.updateTime),
  ip: log.targetType || '-',
  state: ['FAILED', 'FAILURE'].includes(String(log.actionResult || '').toUpperCase()) ? 'danger' : 'success'
})));

const lastUpdatedLabel = computed(() => {
  if (!lastUpdatedAt.value) {
    return '加载中';
  }

  return formatTime(lastUpdatedAt.value);
});

onMounted(async () => {
  const response = await getDashboardSummary();
  summary.value = {
    ...summary.value,
    ...unwrapData(response)
  };
  lastUpdatedAt.value = new Date().toISOString();
});

function articleStatusLabel(status) {
  const value = String(status || '').toUpperCase();
  const labels = {
    DRAFT: '草稿',
    PRIVATE: '私密',
    PUBLISHED: '已发布'
  };

  return labels[value] || status || '-';
}

function commentStatusLabel(status) {
  const value = String(status || '').toUpperCase();
  const labels = {
    APPROVED: '已通过',
    PENDING: '待审核',
    REJECTED: '已驳回'
  };

  return labels[value] || status || '';
}

function formatTime(value) {
  if (!value) {
    return '-';
  }

  const normalized = String(value).replace('T', ' ');
  return normalized.length >= 16 ? normalized.slice(0, 16) : normalized;
}
</script>
