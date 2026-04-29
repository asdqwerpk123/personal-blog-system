<template>
  <section class="category-management">
    <div class="category-page-header">
      <h1>分类管理</h1>
      <el-button class="create-category-button primary-action-button" type="primary" :icon="Plus" @click="openCreateDialog">
        新增分类
      </el-button>
    </div>

    <section class="category-filter-panel" aria-label="分类筛选">
      <div class="category-filter-grid">
        <label class="category-filter-field">
          <span>分类名称</span>
          <el-input
            v-model="filters.categoryName"
            clearable
            placeholder="请输入分类名称"
            @keyup.enter="handleSearch"
          />
        </label>

        <el-button class="category-search-button" type="primary" :icon="Search" @click="handleSearch">
          查询
        </el-button>
        <el-button class="category-reset-button" :icon="RefreshLeft" @click="handleReset">
          重置
        </el-button>
      </div>
    </section>

    <section class="category-table-panel" aria-label="分类列表">
      <el-table
        v-loading="loading"
        class="category-management-table"
        :data="categoryRows"
        row-key="id"
        table-layout="fixed"
      >
        <el-table-column label="序号" width="130">
          <template #default="{ $index }">
            {{ rowSequence($index) }}
          </template>
        </el-table-column>
        <el-table-column label="分类名称" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="category-name-text">{{ row.categoryName || row.name || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="描述" min-width="310" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="category-description-text">{{ row.description || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="排序号" width="130">
          <template #default="{ row }">
            <span class="category-sort-chip">{{ row.sortNo ?? row.sort ?? 0 }}</span>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="190">
          <template #default="{ row }">
            {{ formatDate(row.createTime ?? row.createdTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" align="right" fixed="right">
          <template #default="{ row }">
            <div class="category-row-actions">
              <el-button link type="primary" :icon="EditPen" @click="openEditDialog(row)">
                编辑
              </el-button>
              <el-button
                link
                type="danger"
                :icon="Delete"
                :loading="deleteLoadingId === row.id"
                @click="handleDelete(row)"
              >
                删除
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <div class="category-pagination">
        <span>共 {{ pagination.total }} 条记录，当前第 {{ pagination.page }} / {{ totalPages }} 页</span>
        <el-pagination
          v-model:current-page="pagination.page"
          background
          layout="prev, pager, next"
          :page-size="pagination.pageSize"
          :pager-count="5"
          :total="pagination.total"
          @current-change="handlePageChange"
        />
      </div>
    </section>

    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="520px"
      class="category-dialog"
      destroy-on-close
      :close-on-click-modal="false"
      @closed="resetDialogForm"
    >
      <el-form
        ref="formRef"
        class="category-dialog-form"
        :model="form"
        :rules="formRules"
        label-position="top"
      >
        <el-form-item label="分类名称" prop="categoryName">
          <el-input v-model="form.categoryName" maxlength="50" placeholder="请输入分类名称" />
        </el-form-item>

        <el-form-item label="排序号" prop="sortNo">
          <el-input-number
            v-model="form.sortNo"
            :min="1"
            :max="9999"
            :controls="false"
            placeholder="请输入排序号"
          />
          <p class="sort-helper">数字越小越靠前</p>
        </el-form-item>

        <el-form-item label="描述" prop="description">
          <el-input
            v-model="form.description"
            maxlength="255"
            placeholder="请输入分类描述"
            resize="none"
            :rows="4"
            type="textarea"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="category-dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="submitLoading" @click="submitCategory">确定</el-button>
        </div>
      </template>
    </el-dialog>
  </section>
</template>

<script setup>
import {
  Delete,
  EditPen,
  Plus,
  RefreshLeft,
  Search
} from '@element-plus/icons-vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { computed, nextTick, onMounted, reactive, ref } from 'vue';

import {
  createCategory,
  deleteCategory,
  getCategoryPage,
  updateCategory
} from '@/api/categories.js';

const filters = reactive({
  categoryName: ''
});

const pagination = reactive({
  page: 1,
  pageSize: 10,
  total: 0
});

const form = reactive({
  id: null,
  categoryName: '',
  sortNo: 1,
  description: ''
});

const formRules = {
  categoryName: [
    { required: true, message: '请输入分类名称', trigger: 'blur' },
    { min: 1, max: 50, message: '分类名称不能超过 50 个字符', trigger: 'blur' }
  ],
  sortNo: [
    { required: true, message: '请输入排序号', trigger: 'blur' },
    { type: 'number', min: 1, message: '排序号必须大于 0', trigger: 'change' }
  ]
};

const loading = ref(false);
const submitLoading = ref(false);
const deleteLoadingId = ref(null);
const categoryRows = ref([]);
const dialogVisible = ref(false);
const dialogMode = ref('create');
const formRef = ref(null);

const dialogTitle = computed(() => (dialogMode.value === 'edit' ? '编辑分类' : '新增分类'));
const totalPages = computed(() => Math.max(1, Math.ceil(pagination.total / pagination.pageSize)));

function extractData(response) {
  return response?.data ?? response ?? {};
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
  return {
    page: pagination.page,
    pageSize: pagination.pageSize,
    categoryName: filters.categoryName
  };
}

function getNextSortNo() {
  const maxSortNo = categoryRows.value.reduce((max, category) => {
    const sortNo = Number(category.sortNo ?? category.sort ?? 0);

    return Number.isFinite(sortNo) ? Math.max(max, sortNo) : max;
  }, 0);

  return maxSortNo + 1;
}

async function loadCategories() {
  loading.value = true;

  try {
    const pageData = normalizePageData(await getCategoryPage(buildQueryParams()));
    categoryRows.value = pageData.records;
    pagination.total = pageData.total;
    pagination.page = pageData.page;
    pagination.pageSize = pageData.pageSize;
  } catch (error) {
    categoryRows.value = [];
    pagination.total = 0;
    ElMessage.error(error.message || '分类列表加载失败');
  } finally {
    loading.value = false;
  }
}

async function handleSearch() {
  pagination.page = 1;
  await loadCategories();
}

async function handleReset() {
  filters.categoryName = '';
  pagination.page = 1;
  await loadCategories();
}

function handlePageChange(page) {
  pagination.page = page;
  loadCategories();
}

function rowSequence(rowIndex) {
  return (pagination.page - 1) * pagination.pageSize + rowIndex + 1;
}

function resetDialogForm() {
  form.id = null;
  form.categoryName = '';
  form.sortNo = 1;
  form.description = '';
  formRef.value?.clearValidate();
}

async function openCreateDialog() {
  dialogMode.value = 'create';
  resetDialogForm();
  form.sortNo = getNextSortNo();
  dialogVisible.value = true;

  await nextTick();
  formRef.value?.clearValidate();
}

async function openEditDialog(row) {
  dialogMode.value = 'edit';
  form.id = row.id;
  form.categoryName = row.categoryName || row.name || '';
  form.sortNo = Number(row.sortNo ?? row.sort ?? 1);
  form.description = row.description || '';
  dialogVisible.value = true;

  await nextTick();
  formRef.value?.clearValidate();
}

async function submitCategory() {
  try {
    await formRef.value?.validate();
  } catch {
    return;
  }

  submitLoading.value = true;

  const payload = {
    categoryName: form.categoryName,
    description: form.description,
    sortNo: form.sortNo
  };

  try {
    if (dialogMode.value === 'edit') {
      await updateCategory(form.id, payload);
      ElMessage.success('分类已更新');
    } else {
      await createCategory(payload);
      ElMessage.success('分类已新增');
    }

    dialogVisible.value = false;
    await loadCategories();
  } catch (error) {
    ElMessage.error(error.message || '分类保存失败');
  } finally {
    submitLoading.value = false;
  }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`确定删除分类“${row.categoryName || row.name}”吗？`, '删除确认', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    });

    deleteLoadingId.value = row.id;
    await deleteCategory(row.id);
    ElMessage.success('分类已删除');
    await loadCategories();
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error(error.message || '分类删除失败');
    }
  } finally {
    deleteLoadingId.value = null;
  }
}

function formatDate(value) {
  if (!value) {
    return '-';
  }

  return String(value).replace('T', ' ').replace(/\.\d+$/, '').slice(0, 19);
}

onMounted(() => {
  loadCategories();
});

defineExpose({
  categoryRows,
  dialogTitle,
  filters,
  form,
  handleDelete,
  handleReset,
  handleSearch,
  openCreateDialog,
  openEditDialog,
  pagination,
  rowSequence,
  submitCategory
});
</script>

<style scoped>
.category-management {
  min-height: calc(100vh - 108px);
}

.category-page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 22px;
}

.category-page-header h1 {
  margin: 0;
  color: var(--color-text);
  font-size: 24px;
  line-height: 1.25;
}

.create-category-button {
  min-width: 118px;
  min-height: 40px;
  border: 0;
  border-radius: 7px;
  color: #ffffff;
  background: var(--color-primary);
  box-shadow: 0 8px 18px rgba(54, 87, 245, 0.22);
}

.create-category-button:hover,
.create-category-button:focus {
  color: #ffffff;
  background: var(--color-primary-hover);
}

.category-filter-panel,
.category-table-panel {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-panel);
  background: var(--color-panel);
  box-shadow: var(--shadow-panel);
}

.category-filter-panel {
  padding: 20px 22px;
  margin-bottom: 20px;
}

.category-filter-grid {
  display: grid;
  grid-template-columns: minmax(260px, 420px) auto auto;
  gap: 14px;
  align-items: end;
  justify-content: start;
}

.category-filter-field {
  display: grid;
  gap: 7px;
  min-width: 0;
  color: #556174;
  font-size: 14px;
}

.category-filter-field :deep(.el-input__wrapper) {
  min-height: 38px;
  border-radius: 7px;
  box-shadow: 0 0 0 1px #dde3ee inset;
}

.category-filter-field :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px var(--color-primary) inset;
}

.category-search-button,
.category-reset-button {
  min-width: 80px;
  min-height: 38px;
  border-radius: 7px;
}

.category-search-button {
  border: 0;
  background: var(--color-primary);
  box-shadow: 0 8px 18px rgba(54, 87, 245, 0.22);
}

.category-search-button:hover,
.category-search-button:focus {
  background: var(--color-primary-hover);
}

.category-reset-button {
  color: #556174;
}

.category-table-panel {
  overflow: hidden;
}

.category-management-table {
  width: 100%;
}

.category-management-table :deep(.el-table__header th) {
  height: 48px;
  color: #737d91;
  font-weight: 500;
  background: #f8fafc;
}

.category-management-table :deep(.el-table__row) {
  height: 74px;
}

.category-management-table :deep(.el-table__cell) {
  border-bottom-color: #eef1f6;
}

.category-name-text,
.category-description-text {
  display: inline-block;
  max-width: 100%;
  overflow: hidden;
  color: #1f2937;
  text-overflow: ellipsis;
  vertical-align: middle;
  white-space: nowrap;
}

.category-description-text {
  color: #596477;
}

.category-sort-chip {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 28px;
  min-height: 26px;
  padding: 0 9px;
  border-radius: 6px;
  color: #596477;
  background: #f2f4f8;
  font-size: 13px;
}

.category-row-actions {
  display: grid;
  justify-items: end;
  gap: 6px;
}

.category-row-actions :deep(.el-button) {
  margin-left: 0;
  font-size: 14px;
}

.category-pagination {
  min-height: 58px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 0 18px;
  border-top: 1px solid #eef1f6;
  color: #697386;
}

.category-pagination :deep(.el-pagination.is-background .el-pager li.is-active) {
  color: var(--color-primary);
  background: #eef4ff;
}

.category-dialog-form {
  padding: 8px 2px 0;
}

.category-dialog-form :deep(.el-form-item) {
  margin-bottom: 22px;
}

.category-dialog-form :deep(.el-form-item__label) {
  color: #39445a;
  font-size: 14px;
  line-height: 1.3;
}

.category-dialog-form :deep(.el-form-item.is-required .el-form-item__label::before) {
  color: #ef4444;
}

.category-dialog-form :deep(.el-input__wrapper),
.category-dialog-form :deep(.el-textarea__inner) {
  min-height: 40px;
  border-radius: 7px;
  box-shadow: 0 0 0 1px #dde3ee inset;
}

.category-dialog-form :deep(.el-input__wrapper.is-focus),
.category-dialog-form :deep(.el-textarea__inner:focus) {
  box-shadow: 0 0 0 1px var(--color-primary) inset;
}

.category-dialog-form :deep(.el-input-number) {
  width: 100%;
}

.category-dialog-form :deep(.el-input-number .el-input__inner) {
  text-align: left;
}

.sort-helper {
  width: 100%;
  margin: 7px 0 0;
  color: #8a93a6;
  font-size: 13px;
  line-height: 1.4;
}

.category-dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.category-dialog-footer :deep(.el-button) {
  min-width: 72px;
  min-height: 36px;
  border-radius: 7px;
}

.category-dialog-footer :deep(.el-button--primary) {
  border: 0;
  background: var(--color-primary);
}

.category-dialog-footer :deep(.el-button--primary:hover),
.category-dialog-footer :deep(.el-button--primary:focus) {
  background: var(--color-primary-hover);
}

@media (max-width: 760px) {
  .category-page-header {
    align-items: stretch;
    flex-direction: column;
  }

  .create-category-button {
    width: 100%;
  }

  .category-filter-grid {
    grid-template-columns: 1fr;
  }

  .category-search-button,
  .category-reset-button {
    width: 100%;
  }

  .category-table-panel {
    overflow-x: auto;
  }

  .category-management-table {
    min-width: 880px;
  }

  .category-pagination {
    align-items: flex-start;
    flex-direction: column;
    padding: 14px 16px;
  }
}
</style>
