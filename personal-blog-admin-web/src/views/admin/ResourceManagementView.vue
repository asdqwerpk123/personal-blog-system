<template>
  <section class="admin-page">
    <div class="page-heading">
      <div>
        <h1>{{ title }}</h1>
        <span>{{ description }}</span>
      </div>
      <el-button v-if="canCreate" class="primary-action-button" type="primary" :icon="Plus" @click="openCreate">
        {{ createText }}
      </el-button>
    </div>

    <div class="panel admin-list-panel">
      <div class="admin-toolbar">
        <el-input
          v-model.trim="query.keyword"
          class="toolbar-keyword"
          clearable
          :placeholder="searchPlaceholder"
          @keyup.enter="loadRows"
        />
        <el-select v-if="statusOptions.length" v-model="query.status" class="toolbar-select" clearable placeholder="状态">
          <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-input
          v-if="showArticleFilter"
          v-model.trim="query.articleId"
          class="toolbar-select"
          clearable
          placeholder="文章 ID"
          @keyup.enter="loadRows"
        />
        <el-input
          v-if="showOperatorFilter"
          v-model.trim="query.operatorUserId"
          class="toolbar-select"
          clearable
          placeholder="操作者 ID"
          @keyup.enter="loadRows"
        />
        <el-input
          v-if="showTargetFilter"
          v-model.trim="query.targetType"
          class="toolbar-select"
          clearable
          placeholder="对象类型"
          @keyup.enter="loadRows"
        />
        <el-select
          v-if="resultOptions.length"
          v-model="query.actionResult"
          class="toolbar-select"
          clearable
          placeholder="结果"
        >
          <el-option v-for="item in resultOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-button type="primary" @click="loadRows">搜索</el-button>
      </div>

      <el-table v-loading="loading" :data="rows" table-layout="fixed">
        <el-table-column v-for="column in columns" :key="column.prop" :label="column.label" :min-width="column.minWidth || 120">
          <template #default="{ row }">
            <el-tag v-if="column.type === 'status'" :type="statusType(pick(row, column.keys || [column.prop]))">
              {{ pick(row, column.keys || [column.prop]) }}
            </el-tag>
            <span v-else class="cell-text">{{ pick(row, column.keys || [column.prop]) }}</span>
          </template>
        </el-table-column>
        <el-table-column v-if="!readonly" label="操作" width="250" align="right" fixed="right">
          <template #default="{ row }">
            <el-button v-if="canEdit" link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button v-if="canAudit" link type="success" @click="changeStatus(row, approveStatus)">通过</el-button>
            <el-button v-if="canAudit" link type="warning" @click="changeStatus(row, rejectStatus)">驳回</el-button>
            <el-button v-if="canPublish" link type="success" @click="changeStatus(row, publishStatus)">发布</el-button>
            <el-button v-if="canPublish" link type="info" @click="changeStatus(row, draftStatus)">下架</el-button>
            <el-button v-if="canTags" link type="primary" @click="openTagEditor(row)">标签</el-button>
            <el-button v-if="canDelete" link type="danger" @click="removeRow(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="admin-pagination">
        <el-pagination
          v-model:current-page="query.current"
          v-model:page-size="query.size"
          background
          layout="total, sizes, prev, pager, next"
          :total="total"
          :page-sizes="[10, 20, 50]"
          @current-change="loadRows"
          @size-change="loadRows"
        />
      </div>
    </div>

    <el-dialog v-model="dialogVisible" :title="editingId ? `编辑${shortTitle}` : createText" width="560px">
      <el-form label-position="top" :model="form">
        <el-form-item v-for="field in formFields" :key="field.prop" :label="field.label">
          <el-input
            v-if="field.type !== 'textarea'"
            v-model="form[field.prop]"
            :placeholder="field.placeholder || field.label"
          />
          <el-input
            v-else
            v-model="form[field.prop]"
            type="textarea"
            :rows="4"
            :placeholder="field.placeholder || field.label"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveRow">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="tagDialogVisible" title="编辑文章标签" width="460px">
      <el-select v-model="tagIds" multiple class="full-select" placeholder="选择标签">
        <el-option v-for="tag in tagOptions" :key="tag.id" :label="tag.tagName || tag.name" :value="tag.id" />
      </el-select>
      <template #footer>
        <el-button @click="tagDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveTags">保存标签</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup>
import { ElMessage, ElMessageBox } from 'element-plus';
import { Plus } from '@element-plus/icons-vue';
import { computed, onMounted, reactive, ref } from 'vue';

import { normalizePage, pick, rowId, statusType, unwrapData } from './pageData.js';

const props = defineProps({
  title: { type: String, required: true },
  description: { type: String, default: '' },
  shortTitle: { type: String, default: '资源' },
  searchPlaceholder: { type: String, default: '搜索关键词' },
  columns: { type: Array, required: true },
  formFields: { type: Array, default: () => [] },
  fetchPage: { type: Function, required: true },
  createRow: { type: Function, default: null },
  updateRow: { type: Function, default: null },
  deleteRow: { type: Function, default: null },
  updateStatus: { type: Function, default: null },
  fetchTags: { type: Function, default: null },
  fetchRowTags: { type: Function, default: null },
  updateTags: { type: Function, default: null },
  statusOptions: { type: Array, default: () => [] },
  resultOptions: { type: Array, default: () => [] },
  approveStatus: { type: String, default: 'APPROVED' },
  rejectStatus: { type: String, default: 'REJECTED' },
  publishStatus: { type: String, default: 'PUBLISHED' },
  draftStatus: { type: String, default: 'DRAFT' },
  readonly: { type: Boolean, default: false },
  canAudit: { type: Boolean, default: false },
  canPublish: { type: Boolean, default: false },
  canTags: { type: Boolean, default: false },
  showArticleFilter: { type: Boolean, default: false },
  showOperatorFilter: { type: Boolean, default: false },
  showTargetFilter: { type: Boolean, default: false }
});

const loading = ref(false);
const rows = ref([]);
const total = ref(0);
const dialogVisible = ref(false);
const tagDialogVisible = ref(false);
const editingId = ref('');
const currentTagArticleId = ref('');
const tagIds = ref([]);
const tagOptions = ref([]);
const form = reactive({});
const query = reactive({
  current: 1,
  size: 10,
  keyword: '',
  status: '',
  articleId: '',
  operatorUserId: '',
  targetType: '',
  actionResult: ''
});

const canCreate = computed(() => Boolean(props.createRow && props.formFields.length));
const canEdit = computed(() => Boolean(props.updateRow && props.formFields.length));
const canDelete = computed(() => Boolean(props.deleteRow));
const createText = computed(() => `新增${props.shortTitle}`);

function cleanParams() {
  return Object.fromEntries(
    Object.entries(query).filter(([, value]) => value !== '' && value !== null && value !== undefined)
  );
}

function resetForm(row = {}) {
  for (const field of props.formFields) {
    form[field.prop] = row[field.prop] ?? '';
  }
}

async function loadRows() {
  loading.value = true;

  try {
    const page = normalizePage(await props.fetchPage(cleanParams()));
    rows.value = page.records;
    total.value = page.total;
  } finally {
    loading.value = false;
  }
}

function openCreate() {
  editingId.value = '';
  resetForm();
  dialogVisible.value = true;
}

function openEdit(row) {
  editingId.value = rowId(row);
  resetForm(row);
  dialogVisible.value = true;
}

async function saveRow() {
  if (editingId.value) {
    await props.updateRow(editingId.value, { ...form });
  } else {
    await props.createRow({ ...form });
  }

  ElMessage.success('保存成功');
  dialogVisible.value = false;
  await loadRows();
}

async function changeStatus(row, status) {
  await props.updateStatus(rowId(row), status);
  ElMessage.success('状态已更新');
  await loadRows();
}

async function removeRow(row) {
  await ElMessageBox.confirm(`确认删除这条${props.shortTitle}记录？`, '删除确认', { type: 'warning' });
  await props.deleteRow(rowId(row));
  ElMessage.success('删除成功');
  await loadRows();
}

async function openTagEditor(row) {
  currentTagArticleId.value = rowId(row);
  const [tagsResponse, currentTagsResponse] = await Promise.all([
    props.fetchTags({ current: 1, size: 100 }),
    props.fetchRowTags ? props.fetchRowTags(currentTagArticleId.value) : Promise.resolve([])
  ]);
  tagOptions.value = normalizePage(tagsResponse).records;
  const currentTags = unwrapData(currentTagsResponse);
  const selectedTags = Array.isArray(currentTags) ? currentTags : currentTags.records || currentTags.list || [];
  tagIds.value = selectedTags.map(rowId).filter((id) => id !== undefined && id !== null);
  tagDialogVisible.value = true;
}

async function saveTags() {
  await props.updateTags(currentTagArticleId.value, tagIds.value);
  ElMessage.success('标签已更新');
  tagDialogVisible.value = false;
}

onMounted(loadRows);
</script>
