<template>
  <section class="author-page">
    <div class="page-heading">
      <h1>我的文章</h1>
      <el-button class="primary-action-button" type="primary" :icon="Plus" @click="router.push('/author/articles/new')">新建文章</el-button>
    </div>

    <section class="admin-list-panel panel">
      <div class="admin-toolbar">
        <el-input
          v-model.trim="filters.title"
          class="toolbar-keyword"
          clearable
          placeholder="请输入文章标题"
          @keyup.enter="handleSearch"
        />
        <el-select v-model="filters.categoryId" class="toolbar-select" clearable placeholder="分类">
          <el-option v-for="category in categoryOptions" :key="category.id" :label="category.categoryName" :value="category.id" />
        </el-select>
        <el-select v-model="filters.status" class="toolbar-select" placeholder="状态">
          <el-option v-for="status in filterStatusOptions" :key="status.value" :label="status.label" :value="status.value" />
        </el-select>
        <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
        <el-button :icon="RefreshLeft" @click="handleReset">重置</el-button>
      </div>

      <el-table v-loading="loading" :data="rows" row-key="id" empty-text="暂无文章">
        <el-table-column label="序号" width="86">
          <template #default="{ $index }">{{ rowSequence($index) }}</template>
        </el-table-column>
        <el-table-column label="标题" min-width="220" show-overflow-tooltip>
          <template #default="{ row }">{{ row.articleTitle || '-' }}</template>
        </el-table-column>
        <el-table-column label="分类" width="140">
          <template #default="{ row }">
            <span class="soft-tag">{{ row.categoryName || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="130">
          <template #default="{ row }">
            <span class="status-pill" :class="statusMeta(row.articleStatus).className">
              {{ statusMeta(row.articleStatus).label }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="浏览量" width="110">
          <template #default="{ row }">{{ row.viewCount || 0 }}</template>
        </el-table-column>
        <el-table-column label="发布时间" width="170">
          <template #default="{ row }">{{ formatDate(row.publishedTime || row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="210" fixed="right" align="right">
          <template #default="{ row }">
            <div class="table-actions">
              <el-button link :icon="View" title="查看" @click="router.push(`/author/articles/detail/${row.id}`)" />
              <el-button link :icon="EditPen" title="编辑" @click="router.push(`/author/articles/edit/${row.id}`)" />
              <el-button
                link
                :icon="SwitchButton"
                :loading="statusLoadingId === row.id"
                :title="row.articleStatus === 'PUBLISHED' ? '下线' : '发布'"
                @click="handleStatus(row)"
              />
              <el-button
                link
                class="danger-action"
                :icon="Delete"
                :loading="deleteLoadingId === row.id"
                title="删除"
                @click="handleDelete(row)"
              />
            </div>
          </template>
        </el-table-column>
      </el-table>

      <div class="admin-pagination">
        <el-pagination
          v-model:current-page="pagination.current"
          background
          layout="total, prev, pager, next, jumper"
          :page-size="pagination.size"
          :pager-count="5"
          :total="pagination.total"
          @current-change="loadArticles"
        />
      </div>
    </section>
  </section>
</template>

<script setup>
import { Delete, EditPen, Plus, RefreshLeft, Search, SwitchButton, View } from '@element-plus/icons-vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { onMounted, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';

import {
  deleteAuthorArticle,
  getAuthorArticlePage,
  getAuthorCategories,
  updateAuthorArticleStatus
} from '@/api/articles.js';

const router = useRouter();
const loading = ref(false);
const statusLoadingId = ref(null);
const deleteLoadingId = ref(null);
const rows = ref([]);
const categoryOptions = ref([]);

const filters = reactive({
  title: '',
  categoryId: '',
  status: ''
});

const pagination = reactive({
  current: 1,
  size: 10,
  total: 0
});

const filterStatusOptions = [
  { label: '全部', value: '' },
  { label: '草稿', value: 'DRAFT' },
  { label: '已发布', value: 'PUBLISHED' },
  { label: '私密', value: 'PRIVATE' }
];

function extractData(response) {
  return response?.data ?? response ?? {};
}

function normalizeList(response) {
  const data = extractData(response);
  return Array.isArray(data) ? data : data.records || data.list || [];
}

function normalizePage(response) {
  const data = extractData(response);
  const records = data.records || data.list || [];
  return {
    records,
    total: Number(data.total ?? records.length),
    current: Number(data.current ?? pagination.current),
    size: Number(data.size ?? pagination.size)
  };
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

function rowSequence(rowIndex) {
  return (pagination.current - 1) * pagination.size + rowIndex + 1;
}

async function loadCategories() {
  try {
    categoryOptions.value = normalizeList(await getAuthorCategories());
  } catch (error) {
    ElMessage.warning(error.message || '分类加载失败');
  }
}

async function loadArticles() {
  loading.value = true;
  try {
    const page = normalizePage(await getAuthorArticlePage({
      current: pagination.current,
      size: pagination.size,
      title: filters.title,
      categoryId: filters.categoryId,
      status: filters.status
    }));
    rows.value = page.records;
    pagination.total = page.total;
    pagination.current = page.current;
    pagination.size = page.size;
  } catch (error) {
    rows.value = [];
    pagination.total = 0;
    ElMessage.error(error.message || '文章列表加载失败');
  } finally {
    loading.value = false;
  }
}

function handleSearch() {
  pagination.current = 1;
  loadArticles();
}

function handleReset() {
  filters.title = '';
  filters.categoryId = '';
  filters.status = '';
  pagination.current = 1;
  loadArticles();
}

async function handleStatus(row) {
  const nextStatus = row.articleStatus === 'PUBLISHED' ? 'PRIVATE' : 'PUBLISHED';
  statusLoadingId.value = row.id;
  try {
    await updateAuthorArticleStatus(row.id, nextStatus);
    ElMessage.success(nextStatus === 'PUBLISHED' ? '文章已发布' : '文章已下线');
    await loadArticles();
  } catch (error) {
    ElMessage.error(error.message || '文章状态修改失败');
  } finally {
    statusLoadingId.value = null;
  }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`确定删除文章“${row.articleTitle || row.id}”吗？`, '删除确认', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    });
    deleteLoadingId.value = row.id;
    await deleteAuthorArticle(row.id);
    ElMessage.success('文章已删除');
    await loadArticles();
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error(error.message || '文章删除失败');
    }
  } finally {
    deleteLoadingId.value = null;
  }
}

onMounted(() => {
  loadCategories();
  loadArticles();
});
</script>

<style scoped>
.danger-action {
  color: #ef4444;
}

.status-pill.private {
  color: #9333ea;
  background: #f6efff;
}
</style>
