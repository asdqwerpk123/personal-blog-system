<template>
  <section class="author-page">
    <div class="page-heading">
      <h1>文章详情</h1>
      <el-button text :icon="Back" @click="router.push('/author/articles')">返回列表</el-button>
    </div>

    <section v-loading="loading" class="panel author-detail-panel">
      <h2>{{ article.articleTitle || '-' }}</h2>
      <div class="detail-meta">
        <span>作者：{{ article.authorName || '-' }}</span>
        <span>分类：{{ article.categoryName || '-' }}</span>
        <span>状态：{{ statusMeta(article.articleStatus).label }}</span>
        <span>发布时间：{{ formatDate(article.publishedTime || article.createTime) }}</span>
        <span>浏览量：{{ article.viewCount || 0 }}</span>
      </div>
      <div class="detail-tags">
        <el-tag v-for="tag in article.tags || []" :key="tag" effect="plain">{{ tag }}</el-tag>
      </div>
      <article class="detail-content">{{ article.articleContent || '暂无正文' }}</article>
    </section>

    <section class="panel comments-panel">
      <header class="panel__header">
        <h2>评论列表</h2>
      </header>
      <el-table v-loading="commentsLoading" :data="comments" empty-text="暂无评论">
        <el-table-column label="评论内容" min-width="260">
          <template #default="{ row }">{{ row.commentContent }}</template>
        </el-table-column>
        <el-table-column label="评论人" width="140">
          <template #default="{ row }">{{ row.nickName || '-' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <span class="status-pill" :class="commentStatusMeta(row.commentStatus).className">
              {{ commentStatusMeta(row.commentStatus).label }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="评论时间" width="170">
          <template #default="{ row }">{{ formatDate(row.createTime) }}</template>
        </el-table-column>
      </el-table>

      <div class="comment-compose">
        <h3>发表评论</h3>
        <el-input
          v-model="commentContent"
          type="textarea"
          :rows="4"
          maxlength="500"
          show-word-limit
          placeholder="请输入不少于 2 个字符的评论内容"
        />
        <el-button type="primary" :loading="submitting" @click="submitComment">提交评论</el-button>
      </div>
    </section>
  </section>
</template>

<script setup>
import { Back } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import { onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import { getAuthorArticle, getAuthorArticleComments } from '@/api/articles.js';
import { createAuthorComment } from '@/api/comments.js';

const route = useRoute();
const router = useRouter();
const article = ref({});
const comments = ref([]);
const commentContent = ref('');
const loading = ref(false);
const commentsLoading = ref(false);
const submitting = ref(false);

function extractData(response) {
  return response?.data ?? response ?? {};
}

function normalizeList(response) {
  const data = extractData(response);
  return Array.isArray(data) ? data : data.records || data.list || [];
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

function commentStatusMeta(status) {
  if (status === 'APPROVED') {
    return { label: '已通过', className: '' };
  }
  if (status === 'REJECTED') {
    return { label: '已驳回', className: 'rejected' };
  }
  return { label: '待审核', className: 'pending' };
}

function formatDate(value) {
  if (!value) {
    return '-';
  }
  return String(value).replace('T', ' ').replace(/\.\d+$/, '').slice(0, 16);
}

async function loadArticle() {
  loading.value = true;
  try {
    article.value = extractData(await getAuthorArticle(route.params.id));
  } catch (error) {
    ElMessage.error(error.message || '文章详情加载失败');
    router.push('/author/articles');
  } finally {
    loading.value = false;
  }
}

async function loadComments() {
  commentsLoading.value = true;
  try {
    comments.value = normalizeList(await getAuthorArticleComments(route.params.id));
  } catch (error) {
    ElMessage.error(error.message || '评论列表加载失败');
  } finally {
    commentsLoading.value = false;
  }
}

async function submitComment() {
  if (commentContent.value.trim().length < 2) {
    ElMessage.error('评论内容不能少于 2 个字符');
    return;
  }

  submitting.value = true;
  try {
    await createAuthorComment({
      articleId: Number(route.params.id),
      commentContent: commentContent.value.trim()
    });
    ElMessage.success('评论已提交，等待审核');
    commentContent.value = '';
    await loadComments();
  } catch (error) {
    ElMessage.error(error.message || '评论提交失败');
  } finally {
    submitting.value = false;
  }
}

onMounted(() => {
  loadArticle();
  loadComments();
});
</script>

<style scoped>
.author-detail-panel {
  padding: 28px;
  margin-bottom: 20px;
}

.author-detail-panel h2 {
  margin: 0 0 14px;
  font-size: 26px;
}

.detail-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 10px 18px;
  color: var(--color-muted);
}

.detail-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 16px;
}

.detail-content {
  min-height: 220px;
  margin-top: 24px;
  padding: 22px;
  border-radius: 10px;
  color: #30394b;
  background: #fbfcff;
  line-height: 1.8;
  white-space: pre-wrap;
}

.comments-panel {
  overflow: hidden;
}

.comment-compose {
  display: grid;
  gap: 14px;
  padding: 22px;
  border-top: 1px solid var(--color-border);
}

.comment-compose h3 {
  margin: 0;
}

.comment-compose .el-button {
  justify-self: end;
}

.status-pill.pending {
  color: #e2771f;
  background: #fff0df;
}

.status-pill.rejected {
  color: #e11d48;
  background: #fff0f2;
}

.status-pill.private {
  color: #9333ea;
  background: #f6efff;
}
</style>
