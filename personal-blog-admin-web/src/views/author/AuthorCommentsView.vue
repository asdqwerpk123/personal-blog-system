<template>
  <section class="author-page">
    <div class="page-heading">
      <h1>我的评论</h1>
      <el-button type="primary" @click="router.push('/author/articles')">去文章列表评论</el-button>
    </div>

    <section class="admin-list-panel panel">
      <div class="admin-toolbar">
        <el-input
          v-model.trim="filters.keyword"
          class="toolbar-keyword"
          clearable
          placeholder="请输入关键词搜索评论"
          @keyup.enter="handleSearch"
        />
        <el-select v-model="filters.status" class="toolbar-select" placeholder="状态">
          <el-option v-for="status in statusOptions" :key="status.value" :label="status.label" :value="status.value" />
        </el-select>
        <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
        <el-button :icon="RefreshLeft" @click="handleReset">重置</el-button>
      </div>

      <el-table v-loading="loading" :data="rows" row-key="id" empty-text="暂无评论">
        <el-table-column label="序号" width="86">
          <template #default="{ $index }">{{ rowSequence($index) }}</template>
        </el-table-column>
        <el-table-column label="评论内容" min-width="280">
          <template #default="{ row }">
            <span class="comment-content">{{ row.commentContent }}</span>
          </template>
        </el-table-column>
        <el-table-column label="关联文章" min-width="190" show-overflow-tooltip>
          <template #default="{ row }">
            <el-button link type="primary" @click="router.push(`/author/articles/detail/${row.articleId}`)">
              {{ row.articleTitle || '-' }}
            </el-button>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <span class="status-pill" :class="statusMeta(row.commentStatus).className">
              {{ statusMeta(row.commentStatus).label }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="评论时间" width="170">
          <template #default="{ row }">{{ formatDate(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="130" align="right">
          <template #default="{ row }">
            <div class="table-actions">
              <el-button link :icon="View" title="查看文章" @click="router.push(`/author/articles/detail/${row.articleId}`)" />
              <el-button
                link
                class="danger-action"
                :icon="Delete"
                :loading="deleteLoadingId === row.id"
                title="删除评论"
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
          @current-change="loadComments"
        />
      </div>
    </section>
  </section>
</template>

<script setup>
import { Delete, RefreshLeft, Search, View } from '@element-plus/icons-vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { onMounted, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';

import { deleteAuthorComment, getAuthorCommentPage } from '@/api/comments.js';

const router = useRouter();
const loading = ref(false);
const deleteLoadingId = ref(null);
const rows = ref([]);

const filters = reactive({
  keyword: '',
  status: ''
});

const pagination = reactive({
  current: 1,
  size: 10,
  total: 0
});

const statusOptions = [
  { label: '全部', value: '' },
  { label: '待审核', value: 'PENDING' },
  { label: '已通过', value: 'APPROVED' },
  { label: '已驳回', value: 'REJECTED' }
];

function extractData(response) {
  return response?.data ?? response ?? {};
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

function rowSequence(rowIndex) {
  return (pagination.current - 1) * pagination.size + rowIndex + 1;
}

async function loadComments() {
  loading.value = true;
  try {
    const page = normalizePage(await getAuthorCommentPage({
      current: pagination.current,
      size: pagination.size,
      keyword: filters.keyword,
      status: filters.status
    }));
    rows.value = page.records;
    pagination.total = page.total;
    pagination.current = page.current;
    pagination.size = page.size;
  } catch (error) {
    rows.value = [];
    pagination.total = 0;
    ElMessage.error(error.message || '评论列表加载失败');
  } finally {
    loading.value = false;
  }
}

function handleSearch() {
  pagination.current = 1;
  loadComments();
}

function handleReset() {
  filters.keyword = '';
  filters.status = '';
  pagination.current = 1;
  loadComments();
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm('确定删除这条评论吗？', '删除确认', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    });
    deleteLoadingId.value = row.id;
    await deleteAuthorComment(row.id);
    ElMessage.success('评论已删除');
    await loadComments();
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error(error.message || '评论删除失败');
    }
  } finally {
    deleteLoadingId.value = null;
  }
}

onMounted(loadComments);
</script>

<style scoped>
.comment-content {
  line-height: 1.7;
}

.danger-action {
  color: #ef4444;
}

.status-pill.pending {
  color: #e2771f;
  background: #fff0df;
}

.status-pill.rejected {
  color: #e11d48;
  background: #fff0f2;
}
</style>
