<template>
  <section class="article-management">
    <div class="article-page-header">
      <h1>文章管理</h1>
      <el-button class="create-article-button primary-action-button" type="primary" :icon="Plus" @click="handleCreate">
        新增文章
      </el-button>
    </div>

    <section class="article-filter-panel" aria-label="文章筛选">
      <div class="article-filter-grid">
        <label class="article-filter-field article-filter-field--title">
          <span>标题</span>
          <el-input
            v-model="filters.title"
            clearable
            placeholder="请输入文章标题"
            @keyup.enter="handleSearch"
          />
        </label>

        <label class="article-filter-field">
          <span>分类</span>
          <el-select v-model="filters.category" placeholder="全部" filterable>
            <el-option
              v-for="category in categoryOptions"
              :key="`${category.source}-${category.value}`"
              :label="category.label"
              :value="category.value"
            />
          </el-select>
        </label>

        <label class="article-filter-field">
          <span>状态</span>
          <el-select v-model="filters.status" placeholder="全部">
            <el-option
              v-for="status in statusOptions"
              :key="status.value || 'all-status'"
              :label="status.label"
              :value="status.value"
            />
          </el-select>
        </label>

        <el-button class="article-search-button" type="primary" :icon="Search" @click="handleSearch">
          查询
        </el-button>
        <el-button class="article-reset-button" :icon="RefreshLeft" @click="handleReset">
          重置
        </el-button>
      </div>
    </section>

    <section class="article-table-panel" aria-label="文章列表">
      <el-table
        v-loading="loading"
        class="article-management-table"
        :data="articleRows"
        row-key="id"
        table-layout="fixed"
      >
        <el-table-column label="序号" width="86">
          <template #default="{ $index }">
            {{ rowSequence($index) }}
          </template>
        </el-table-column>
        <el-table-column label="标题" min-width="220" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="article-title-text">{{ getArticleTitle(row) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="分类" width="145">
          <template #default="{ row }">
            <span class="article-category-chip">{{ getCategoryLabel(row) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="130">
          <template #default="{ row }">
            <span class="article-status-pill" :class="getStatusMeta(row).className">
              {{ getStatusMeta(row).label }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="浏览量" width="130">
          <template #default="{ row }">
            {{ formatNumber(row.viewCount ?? row.views ?? 0) }}
          </template>
        </el-table-column>
        <el-table-column label="发布时间" width="190">
          <template #default="{ row }">
            {{ formatDate(row.publishedTime ?? row.publishTime ?? row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" align="right" fixed="right">
          <template #default="{ row }">
            <div class="article-row-actions">
              <el-tooltip content="查看" placement="top">
                <el-button link :icon="View" aria-label="查看文章" @click="handleView(row)" />
              </el-tooltip>
              <el-tooltip content="编辑" placement="top">
                <el-button link :icon="EditPen" aria-label="编辑文章" @click="handleEdit(row)" />
              </el-tooltip>
              <el-tooltip content="状态切换" placement="top">
                <el-button
                  link
                  :icon="SwitchButton"
                  :loading="statusLoadingId === row.id"
                  aria-label="切换文章状态"
                  @click="handleStatusToggle(row)"
                />
              </el-tooltip>
              <el-tooltip content="删除" placement="top">
                <el-button
                  link
                  class="danger-action"
                  :icon="Delete"
                  :loading="deleteLoadingId === row.id"
                  aria-label="删除文章"
                  @click="handleDelete(row)"
                />
              </el-tooltip>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <div class="article-pagination">
        <span>共 {{ pagination.total }} 条记录，当前第 {{ pagination.page }} / {{ totalPages }} 页</span>
        <el-pagination
          v-model:current-page="pagination.page"
          background
          layout="prev, pager, next, jumper"
          :page-size="pagination.pageSize"
          :pager-count="5"
          :total="pagination.total"
          @current-change="handlePageChange"
        />
      </div>
    </section>

    <el-drawer
      v-model="articleDialogVisible"
      :title="articleDialogTitle"
      size="620px"
      destroy-on-close
      :close-on-click-modal="false"
      class="article-editor-drawer"
    >
      <el-form
        ref="articleFormRef"
        class="article-editor-form"
        :model="articleForm"
        :rules="articleRules"
        label-position="top"
      >
        <el-form-item label="文章标题" prop="articleTitle">
          <el-input v-model.trim="articleForm.articleTitle" maxlength="200" placeholder="请输入文章标题" />
        </el-form-item>

        <el-form-item label="短链接" prop="articleSlug">
          <el-input v-model.trim="articleForm.articleSlug" maxlength="200" placeholder="例如 spring-boot-notes" />
        </el-form-item>

        <el-form-item label="分类" prop="categoryId">
          <el-select v-model="articleForm.categoryId" class="article-full-control" clearable filterable placeholder="请选择分类">
            <el-option
              v-for="category in remoteCategoryOptions"
              :key="category.value"
              :label="category.label"
              :value="category.value"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="状态" prop="articleStatus">
          <el-select v-model="articleForm.articleStatus" class="article-full-control" placeholder="请选择状态">
            <el-option
              v-for="status in articleStatusOptions"
              :key="status.value"
              :label="status.label"
              :value="status.value"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="摘要" prop="articleSummary">
          <el-input
            v-model="articleForm.articleSummary"
            maxlength="500"
            placeholder="请输入文章摘要"
            resize="none"
            :rows="3"
            type="textarea"
          />
        </el-form-item>

        <el-form-item label="封面图地址" prop="coverUrl">
          <div class="cover-url-editor">
            <el-input
              v-model.trim="articleForm.coverUrl"
              placeholder="请输入封面图 URL"
              @input="coverPreviewBroken = false"
            />
            <el-upload
              accept="image/jpeg,image/png,image/webp,image/gif"
              :before-upload="beforeCoverUpload"
              :http-request="uploadCover"
              :show-file-list="false"
            >
              <el-button :icon="Upload" :loading="coverUploading">上传封面</el-button>
            </el-upload>
          </div>
          <div v-if="articleForm.coverUrl" class="cover-preview">
            <img
              v-if="!coverPreviewBroken"
              :src="articleForm.coverUrl"
              alt="封面图预览"
              @error="coverPreviewBroken = true"
            />
            <span v-else>封面图预览失败</span>
          </div>
        </el-form-item>

        <el-form-item label="正文" prop="articleContent">
          <el-input
            v-model="articleForm.articleContent"
            placeholder="请输入文章正文"
            resize="vertical"
            :rows="8"
            type="textarea"
          />
        </el-form-item>

        <div class="article-switch-grid">
          <el-form-item label="是否置顶" prop="topFlag">
            <el-switch v-model="articleForm.topFlag" active-text="置顶" inactive-text="不置顶" />
          </el-form-item>
          <el-form-item label="是否允许评论" prop="allowComment">
            <el-switch v-model="articleForm.allowComment" active-text="允许" inactive-text="关闭" />
          </el-form-item>
        </div>
      </el-form>

      <template #footer>
        <div class="article-drawer-footer">
          <el-button @click="articleDialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="submitLoading" @click="submitArticle">保存</el-button>
        </div>
      </template>
    </el-drawer>

    <el-dialog
      v-model="detailDialogVisible"
      title="文章详情"
      width="720px"
      class="article-detail-dialog"
      destroy-on-close
    >
      <el-descriptions :column="2" border>
        <el-descriptions-item label="标题" :span="2">
          {{ articleDetail.articleTitle || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="分类">
          {{ getCategoryLabel(articleDetail) }}
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          {{ getStatusMeta(articleDetail).label }}
        </el-descriptions-item>
        <el-descriptions-item label="发布时间">
          {{ formatDate(articleDetail.publishedTime || articleDetail.createTime) }}
        </el-descriptions-item>
        <el-descriptions-item label="浏览量">
          {{ formatNumber(articleDetail.viewCount || 0) }}
        </el-descriptions-item>
        <el-descriptions-item label="短链接" :span="2">
          {{ articleDetail.articleSlug || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="摘要" :span="2">
          {{ articleDetail.articleSummary || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="封面图" :span="2">
          {{ articleDetail.coverUrl || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="正文" :span="2">
          <div class="article-detail-content">{{ articleDetail.articleContent || '-' }}</div>
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </section>
</template>

<script setup>
import {
  Delete,
  EditPen,
  Plus,
  RefreshLeft,
  Search,
  SwitchButton,
  Upload,
  View
} from '@element-plus/icons-vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { computed, nextTick, onMounted, reactive, ref, watch } from 'vue';
import { useRoute } from 'vue-router';

import {
  createArticle,
  deleteArticle,
  getArticle,
  getArticlePage,
  getCategoryList,
  updateArticle,
  updateArticleStatus
} from '@/api/articles.js';
import { uploadFile } from '@/api/files.js';

const route = useRoute();

const fallbackCategories = [
  '前端开发',
  '后端开发',
  '架构设计',
  '数据库',
  '运维部署'
].map((label) => ({
  label,
  value: label,
  source: 'fallback'
}));

const statusOptions = [
  { label: '全部', value: '' },
  { label: '已发布', value: 'PUBLISHED' },
  { label: '草稿', value: 'DRAFT' },
  { label: '已下线', value: 'PRIVATE' }
];
const articleStatusOptions = statusOptions.filter((status) => status.value);

const filters = reactive({
  title: '',
  category: '',
  status: ''
});

const pagination = reactive({
  page: 1,
  pageSize: 10,
  total: 0
});

const loading = ref(false);
const submitLoading = ref(false);
const coverUploading = ref(false);
const coverPreviewBroken = ref(false);
const articleRows = ref([]);
const categoryOptions = ref([{ label: '全部', value: '', source: 'all' }]);
const statusLoadingId = ref(null);
const deleteLoadingId = ref(null);
const articleDialogVisible = ref(false);
const detailDialogVisible = ref(false);
const dialogMode = ref('create');
const editingArticleId = ref(null);
const handledRouteAction = ref('');
const articleFormRef = ref(null);
const articleDetail = ref({});
const articleForm = reactive({
  articleTitle: '',
  articleSlug: '',
  categoryId: '',
  articleStatus: 'DRAFT',
  articleSummary: '',
  coverUrl: '',
  articleContent: '',
  topFlag: false,
  allowComment: true
});
const articleRules = {
  articleTitle: [{ required: true, message: '请输入文章标题', trigger: 'blur' }],
  articleSlug: [{ required: true, message: '请输入短链接', trigger: 'blur' }],
  articleContent: [{ required: true, message: '请输入文章正文', trigger: 'blur' }]
};

const totalPages = computed(() => Math.max(1, Math.ceil(pagination.total / pagination.pageSize)));
const articleDialogTitle = computed(() => (dialogMode.value === 'edit' ? '编辑文章' : '新增文章'));
const remoteCategoryOptions = computed(() => categoryOptions.value.filter((category) => category.source === 'remote'));
const categoryNameById = computed(() => {
  const map = new Map();

  categoryOptions.value
    .filter((category) => category.source === 'remote')
    .forEach((category) => {
      map.set(String(category.value), category.label);
    });

  return map;
});

function extractData(response) {
  return response?.data ?? response ?? {};
}

function normalizeCategoryOptions(response) {
  const data = extractData(response);
  const list = Array.isArray(data) ? data : data.records || data.list || data.rows || [];
  const remoteCategories = list
    .map((category) => {
      const label = category.categoryName || category.name || category.title;
      const value = category.id ?? category.categoryId ?? label;

      return label ? { label, value, source: 'remote' } : null;
    })
    .filter(Boolean);

  return [
    { label: '全部', value: '', source: 'all' },
    ...(remoteCategories.length > 0 ? remoteCategories : fallbackCategories)
  ];
}

function normalizePageData(response) {
  const data = extractData(response);
  const records = data.records || data.list || data.rows || [];

  return {
    records,
    total: Number(data.total ?? records.length ?? 0),
    page: Number(data.current ?? data.page ?? data.pageNum ?? pagination.page),
    pageSize: Number(data.size ?? data.pageSize ?? pagination.pageSize)
  };
}

function buildQueryParams() {
  const selectedCategory = categoryOptions.value.find((category) => category.value === filters.category);
  const params = {
    page: pagination.page,
    pageSize: pagination.pageSize,
    title: filters.title,
    categoryId: '',
    categoryName: '',
    status: filters.status
  };

  if (selectedCategory?.source === 'remote') {
    params.categoryId = selectedCategory.value;
  }

  if (selectedCategory?.source === 'fallback') {
    params.categoryName = selectedCategory.value;
  }

  return params;
}

async function loadCategories() {
  try {
    categoryOptions.value = normalizeCategoryOptions(await getCategoryList());
  } catch (error) {
    categoryOptions.value = [
      { label: '全部', value: '', source: 'all' },
      ...fallbackCategories
    ];
    ElMessage.warning(error.message || '分类列表暂不可用，已使用临时分类');
  }
}

async function loadArticles() {
  loading.value = true;

  try {
    const pageData = normalizePageData(await getArticlePage(buildQueryParams()));
    articleRows.value = pageData.records;
    pagination.total = pageData.total;
    pagination.page = pageData.page;
    pagination.pageSize = pageData.pageSize;
  } catch (error) {
    articleRows.value = [];
    pagination.total = 0;
    ElMessage.error(error.message || '文章列表加载失败');
  } finally {
    loading.value = false;
  }
}

function getArticleTitle(row) {
  return row.articleTitle || row.title || row.name || '未命名文章';
}

function getCategoryLabel(row) {
  return row.categoryName
    || row.category?.categoryName
    || row.category?.name
    || categoryNameById.value.get(String(row.categoryId))
    || row.category
    || '-';
}

function getStatusMeta(row) {
  const status = row.articleStatus || row.status;

  if (status === 'PUBLISHED' || status === '已发布') {
    return { label: '已发布', className: 'is-published' };
  }

  if (status === 'PRIVATE' || status === 'OFFLINE' || status === '已下线') {
    return { label: '已下线', className: 'is-offline' };
  }

  return { label: '草稿', className: 'is-draft' };
}

function getNextStatus(row) {
  const status = row.articleStatus || row.status;

  return status === 'PUBLISHED' ? 'PRIVATE' : 'PUBLISHED';
}

function formatNumber(value) {
  return Number(value || 0).toLocaleString('en-US');
}

function formatDate(value) {
  if (!value) {
    return '-';
  }

  return String(value).replace('T', ' ').replace(/\.\d+$/, '').slice(0, 19);
}

function handleSearch() {
  pagination.page = 1;
  loadArticles();
}

function handleReset() {
  filters.title = '';
  filters.category = '';
  filters.status = '';
  pagination.page = 1;
  loadArticles();
}

function handlePageChange(page) {
  pagination.page = page;
  loadArticles();
}

function rowSequence(rowIndex) {
  return (pagination.page - 1) * pagination.pageSize + rowIndex + 1;
}

function beforeCoverUpload(file) {
  if (!file.type || !file.type.startsWith('image/')) {
    ElMessage.error('请选择图片文件');
    return false;
  }

  if (file.size > 5 * 1024 * 1024) {
    ElMessage.error('图片大小不能超过 5MB');
    return false;
  }

  return true;
}

async function uploadCover(options) {
  coverUploading.value = true;

  try {
    const response = await uploadFile(options.file);
    const data = response?.data ?? response ?? {};
    const url = response?.url || data?.url || '';
    articleForm.coverUrl = url;
    coverPreviewBroken.value = false;
    ElMessage.success('封面图上传成功');
  } catch (error) {
    ElMessage.error(error.message || '封面图上传失败');
  } finally {
    coverUploading.value = false;
  }
}

async function handleCreate() {
  dialogMode.value = 'create';
  editingArticleId.value = null;
  resetArticleForm();
  articleDialogVisible.value = true;
  await nextTick();
  articleFormRef.value?.clearValidate();
}

async function handleView(row) {
  try {
    articleDetail.value = extractData(await getArticle(row.id));
    detailDialogVisible.value = true;
  } catch (error) {
    ElMessage.error(error.message || '文章详情加载失败');
  }
}

async function handleEdit(row) {
  dialogMode.value = 'edit';
  editingArticleId.value = row.id;
  articleDialogVisible.value = true;

  try {
    fillArticleForm(extractData(await getArticle(row.id)));
    await nextTick();
    articleFormRef.value?.clearValidate();
  } catch (error) {
    articleDialogVisible.value = false;
    ElMessage.error(error.message || '文章详情加载失败');
  }
}

function resetArticleForm() {
  articleForm.articleTitle = '';
  articleForm.articleSlug = '';
  articleForm.categoryId = '';
  articleForm.articleStatus = 'DRAFT';
  articleForm.articleSummary = '';
  articleForm.coverUrl = '';
  articleForm.articleContent = '';
  articleForm.topFlag = false;
  articleForm.allowComment = true;
  coverPreviewBroken.value = false;
}

async function handleRouteArticleAction() {
  const action = String(route.query.action || '');
  const id = route.query.id;

  if (!id || !['view', 'edit'].includes(action)) {
    return;
  }

  const actionKey = `${action}:${id}`;
  if (handledRouteAction.value === actionKey) {
    return;
  }
  handledRouteAction.value = actionKey;

  if (action === 'view') {
    await handleView({ id });
    return;
  }

  await handleEdit({ id });
}

function fillArticleForm(article) {
  articleForm.articleTitle = article.articleTitle || '';
  articleForm.articleSlug = article.articleSlug || '';
  articleForm.categoryId = article.categoryId ?? '';
  articleForm.articleStatus = article.articleStatus || 'DRAFT';
  articleForm.articleSummary = article.articleSummary || '';
  articleForm.coverUrl = article.coverUrl || '';
  articleForm.articleContent = article.articleContent || '';
  articleForm.topFlag = Boolean(article.topFlag);
  articleForm.allowComment = article.allowComment !== false;
  coverPreviewBroken.value = false;
}

function buildArticlePayload() {
  return {
    articleTitle: articleForm.articleTitle,
    articleSlug: articleForm.articleSlug,
    categoryId: articleForm.categoryId === '' ? null : articleForm.categoryId,
    articleStatus: articleForm.articleStatus,
    articleSummary: articleForm.articleSummary,
    coverUrl: articleForm.coverUrl,
    articleContent: articleForm.articleContent,
    topFlag: articleForm.topFlag,
    allowComment: articleForm.allowComment
  };
}

async function submitArticle() {
  try {
    await articleFormRef.value?.validate();
  } catch {
    return;
  }

  submitLoading.value = true;

  try {
    const payload = buildArticlePayload();
    if (dialogMode.value === 'edit') {
      await updateArticle(editingArticleId.value, payload);
      ElMessage.success('文章已更新');
    } else {
      await createArticle(payload);
      ElMessage.success('文章已新增');
    }
    articleDialogVisible.value = false;
    await loadArticles();
  } catch (error) {
    ElMessage.error(error.message || '文章保存失败');
  } finally {
    submitLoading.value = false;
  }
}

async function handleStatusToggle(row) {
  statusLoadingId.value = row.id;

  try {
    await updateArticleStatus(row.id, getNextStatus(row));
    ElMessage.success('文章状态已更新');
    await loadArticles();
  } catch (error) {
    ElMessage.error(error.message || '文章状态更新失败');
  } finally {
    statusLoadingId.value = null;
  }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`确定删除文章“${getArticleTitle(row)}”吗？`, '删除确认', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    });

    deleteLoadingId.value = row.id;
    await deleteArticle(row.id);
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

watch(
  () => [route.query.action, route.query.id],
  () => {
    handleRouteArticleAction();
  }
);

onMounted(() => {
  loadCategories();
  loadArticles();
  handleRouteArticleAction();
});

defineExpose({
  articleDetail,
  articleDialogTitle,
  articleDialogVisible,
  articleForm,
  articleRows,
  categoryOptions,
  detailDialogVisible,
  filters,
  handleCreate,
  handleEdit,
  handleView,
  pagination,
  rowSequence,
  submitArticle,
  uploadCover
});
</script>

<style scoped>
.article-management {
  min-height: calc(100vh - 108px);
}

.article-page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 22px;
}

.article-page-header h1 {
  margin: 0;
  color: var(--color-text);
  font-size: 24px;
  line-height: 1.25;
}

.create-article-button {
  min-width: 118px;
  min-height: 40px;
  border: 0;
  border-radius: 7px;
  color: #ffffff;
  background: var(--color-primary);
  box-shadow: 0 8px 18px rgba(54, 87, 245, 0.22);
}

.create-article-button:hover,
.create-article-button:focus {
  color: #ffffff;
  background: var(--color-primary-hover);
}

.article-filter-panel,
.article-table-panel {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-panel);
  background: var(--color-panel);
  box-shadow: var(--shadow-panel);
}

.article-filter-panel {
  padding: 20px 22px;
  margin-bottom: 20px;
}

.article-filter-grid {
  display: grid;
  grid-template-columns: minmax(240px, 1.35fr) minmax(168px, 0.7fr) minmax(168px, 0.7fr) auto auto;
  gap: 14px;
  align-items: end;
}

.article-filter-field {
  display: grid;
  gap: 7px;
  min-width: 0;
  color: #556174;
  font-size: 14px;
}

.article-filter-field--title {
  min-width: 280px;
}

.article-filter-field :deep(.el-input__wrapper),
.article-filter-field :deep(.el-select__wrapper) {
  min-height: 38px;
  border-radius: 7px;
  box-shadow: 0 0 0 1px #dde3ee inset;
}

.article-filter-field :deep(.el-input__wrapper.is-focus),
.article-filter-field :deep(.el-select__wrapper.is-focused) {
  box-shadow: 0 0 0 1px var(--color-primary) inset;
}

.article-search-button,
.article-reset-button {
  min-width: 80px;
  min-height: 38px;
  border-radius: 7px;
}

.article-search-button {
  border: 0;
  background: var(--color-primary);
  box-shadow: 0 8px 18px rgba(54, 87, 245, 0.22);
}

.article-search-button:hover,
.article-search-button:focus {
  background: var(--color-primary-hover);
}

.article-reset-button {
  color: #556174;
}

.article-table-panel {
  overflow: hidden;
}

.article-management-table {
  width: 100%;
}

.article-management-table :deep(.el-table__header th) {
  height: 48px;
  color: #737d91;
  font-weight: 500;
  background: #f8fafc;
}

.article-management-table :deep(.el-table__row) {
  height: 64px;
}

.article-management-table :deep(.el-table__cell) {
  border-bottom-color: #eef1f6;
}

.article-title-text {
  display: inline-block;
  max-width: 100%;
  overflow: hidden;
  color: #1f2937;
  text-overflow: ellipsis;
  vertical-align: middle;
  white-space: nowrap;
}

.article-category-chip {
  display: inline-flex;
  align-items: center;
  min-height: 24px;
  max-width: 100%;
  padding: 0 12px;
  overflow: hidden;
  border: 1px solid #e7ebf1;
  border-radius: 7px;
  color: #596477;
  background: #f3f5f9;
  font-size: 13px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.article-status-pill {
  display: inline-flex;
  align-items: center;
  min-height: 26px;
  padding: 0 12px;
  border-radius: 7px;
  font-size: 13px;
}

.article-status-pill::before {
  width: 6px;
  height: 6px;
  content: "";
  margin-right: 7px;
  border-radius: 999px;
  background: currentColor;
}

.article-status-pill.is-published {
  color: #079447;
  background: #eaf9ef;
  box-shadow: 0 0 0 1px #d6f3df inset;
}

.article-status-pill.is-draft {
  color: #626c7c;
  background: #f0f2f5;
}

.article-status-pill.is-offline {
  color: #e11d48;
  background: #fff0f2;
  box-shadow: 0 0 0 1px #ffd9df inset;
}

.article-row-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.article-row-actions :deep(.el-button) {
  color: #99a3b4;
  font-size: 17px;
}

.article-row-actions :deep(.el-button:hover) {
  color: var(--color-primary);
}

.article-row-actions .danger-action:hover {
  color: #e11d48;
}

.article-pagination {
  min-height: 58px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 0 18px;
  border-top: 1px solid #eef1f6;
  color: #697386;
}

.article-pagination :deep(.el-pagination.is-background .el-pager li.is-active) {
  color: var(--color-primary);
  background: #eef4ff;
}

.article-editor-form {
  padding: 4px 4px 20px;
}

.article-editor-form :deep(.el-form-item) {
  margin-bottom: 20px;
}

.article-full-control {
  width: 100%;
}

.cover-url-editor {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 10px;
  width: 100%;
}

.cover-preview {
  width: min(220px, 100%);
  aspect-ratio: 16 / 9;
  display: grid;
  place-items: center;
  margin-top: 10px;
  overflow: hidden;
  border: 1px solid #e2e8f0;
  border-radius: 7px;
  color: #8a93a6;
  background: #f8fafc;
  font-size: 13px;
}

.cover-preview img {
  width: 100%;
  height: 100%;
  display: block;
  object-fit: cover;
}

.article-switch-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
}

.article-drawer-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.article-detail-content {
  max-height: 260px;
  overflow: auto;
  color: #4a566b;
  line-height: 1.7;
  white-space: pre-wrap;
}

@media (max-width: 1180px) {
  .article-filter-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .article-search-button,
  .article-reset-button {
    width: 100%;
  }
}

@media (max-width: 760px) {
  .article-page-header {
    align-items: stretch;
    flex-direction: column;
  }

  .create-article-button {
    width: 100%;
  }

  .cover-url-editor {
    grid-template-columns: 1fr;
  }

  .article-filter-grid {
    grid-template-columns: 1fr;
  }

  .article-filter-field--title {
    min-width: 0;
  }

  .article-table-panel {
    overflow-x: auto;
  }

  .article-management-table {
    min-width: 880px;
  }

  .article-pagination {
    align-items: flex-start;
    flex-direction: column;
    padding: 14px 16px;
  }
}
</style>
