<template>
  <section class="friend-link-page">
    <div class="page-heading">
      <div>
        <h1>友链管理</h1>
        <span>维护友情链接申请、Logo 和审核状态</span>
      </div>
      <el-button class="primary-action-button" type="primary" :icon="Plus" @click="openCreateDialog">新增友链</el-button>
    </div>

    <div class="panel admin-list-panel">
      <div class="admin-toolbar">
        <el-input
          v-model.trim="query.keyword"
          class="toolbar-keyword"
          clearable
          placeholder="搜索站点名称"
          @keyup.enter="loadFriendLinks"
        />
        <el-select v-model="query.status" class="toolbar-select" clearable placeholder="状态">
          <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-button type="primary" @click="loadFriendLinks">搜索</el-button>
      </div>

      <el-table v-loading="loading" :data="friendLinks" table-layout="fixed">
        <el-table-column label="Logo" width="86">
          <template #default="{ row }">
            <img
              v-if="row.siteLogo && !isLogoBroken(row)"
              class="friend-link-logo-thumb"
              :src="row.siteLogo"
              :alt="row.siteName"
              @error="markLogoBroken(row)"
            />
            <span v-else-if="row.siteLogo" class="logo-invalid-text">图片失效</span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="站点名称" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">{{ row.siteName || '-' }}</template>
        </el-table-column>
        <el-table-column label="链接" min-width="240" show-overflow-tooltip>
          <template #default="{ row }">{{ row.siteUrl || '-' }}</template>
        </el-table-column>
        <el-table-column label="站长" min-width="130" show-overflow-tooltip>
          <template #default="{ row }">{{ row.ownerName || '-' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="statusType(row.linkStatus || row.status)">
              {{ statusLabel(row.linkStatus || row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="更新时间" min-width="170">
          <template #default="{ row }">{{ formatDate(row.updateTime || row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="310" align="right" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEditDialog(row)">编辑</el-button>
            <el-button link type="success" @click="changeFriendLinkStatus(row, 'APPROVED')">通过</el-button>
            <el-button link type="warning" @click="changeFriendLinkStatus(row, 'REJECTED')">驳回</el-button>
            <el-button link type="info" @click="changeFriendLinkStatus(row, 'PENDING')">待审核</el-button>
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
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
          @current-change="loadFriendLinks"
          @size-change="loadFriendLinks"
        />
      </div>
    </div>

    <el-dialog
      v-model="dialogVisible"
      :title="editingId ? '编辑友链' : '新增友链'"
      width="560px"
      destroy-on-close
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" class="friend-link-form" :model="form" :rules="formRules" label-position="top">
        <el-form-item label="站点名称" prop="siteName">
          <el-input v-model.trim="form.siteName" maxlength="100" placeholder="请输入站点名称" />
        </el-form-item>
        <el-form-item label="站点地址" prop="siteUrl">
          <el-input v-model.trim="form.siteUrl" maxlength="255" placeholder="请输入站点地址" />
        </el-form-item>
        <el-form-item label="站点 Logo">
          <div class="logo-uploader">
            <div class="logo-preview">
              <img v-if="form.siteLogo && !logoPreviewBroken" :src="form.siteLogo" alt="友链 Logo 预览" @error="logoPreviewBroken = true" />
              <span v-else-if="form.siteLogo">图片失效</span>
              <span v-else>暂无 Logo</span>
            </div>
            <div class="logo-actions">
              <el-upload
                accept="image/jpeg,image/png,image/webp,image/gif"
                :before-upload="beforeLogoUpload"
                :http-request="uploadLogo"
                :show-file-list="false"
              >
                <el-button :loading="uploadLoading">
                  {{ form.siteLogo ? '更换 Logo' : '上传 Logo' }}
                </el-button>
              </el-upload>
              <p>支持 JPG、PNG、WEBP、GIF，建议大小不超过 2MB</p>
            </div>
          </div>
        </el-form-item>
        <el-form-item label="站长名称">
          <el-input v-model.trim="form.ownerName" maxlength="50" placeholder="请输入站长名称" />
        </el-form-item>
        <el-form-item label="联系邮箱">
          <el-input v-model.trim="form.contactEmail" maxlength="100" placeholder="请输入联系邮箱" />
        </el-form-item>
        <el-form-item label="审核状态">
          <el-select v-model="form.linkStatus" class="full-select" placeholder="请选择审核状态">
            <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="站点描述">
          <el-input
            v-model="form.siteDesc"
            maxlength="255"
            placeholder="请输入站点描述"
            resize="none"
            :rows="4"
            type="textarea"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="submitFriendLink">保存</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup>
import { ElMessage, ElMessageBox } from 'element-plus';
import { Plus } from '@element-plus/icons-vue';
import { onMounted, reactive, ref } from 'vue';

import {
  createFriendLink,
  deleteFriendLink,
  getFriendLinkPage,
  updateFriendLink,
  updateFriendLinkStatus,
  uploadFriendLinkLogo
} from '@/api/friendLinks.js';
import { normalizePage, statusType } from './pageData.js';

const statusOptions = [
  { label: '待审核', value: 'PENDING' },
  { label: '已通过', value: 'APPROVED' },
  { label: '已驳回', value: 'REJECTED' }
];

const formRules = {
  siteName: [{ required: true, message: '请输入站点名称', trigger: 'blur' }],
  siteUrl: [{ required: true, message: '请输入站点地址', trigger: 'blur' }]
};

const loading = ref(false);
const submitLoading = ref(false);
const uploadLoading = ref(false);
const logoPreviewBroken = ref(false);
const brokenLogoIds = ref(new Set());
const friendLinks = ref([]);
const total = ref(0);
const dialogVisible = ref(false);
const editingId = ref('');
const formRef = ref(null);
const query = reactive({
  current: 1,
  size: 10,
  keyword: '',
  status: ''
});
const form = reactive({
  siteName: '',
  siteUrl: '',
  siteLogo: '',
  ownerName: '',
  contactEmail: '',
  siteDesc: '',
  linkStatus: 'PENDING'
});

function cleanParams() {
  return Object.fromEntries(
    Object.entries(query).filter(([, value]) => value !== '' && value !== null && value !== undefined)
  );
}

async function loadFriendLinks() {
  loading.value = true;

  try {
    const page = normalizePage(await getFriendLinkPage(cleanParams()));
    friendLinks.value = page.records;
    total.value = page.total;
  } catch (error) {
    friendLinks.value = [];
    total.value = 0;
    ElMessage.error(error.message || '友链列表加载失败');
  } finally {
    loading.value = false;
  }
}

function resetForm(row = {}) {
  form.siteName = row.siteName || '';
  form.siteUrl = row.siteUrl || '';
  form.siteLogo = row.siteLogo || '';
  form.ownerName = row.ownerName || '';
  form.contactEmail = row.contactEmail || '';
  form.siteDesc = row.siteDesc || '';
  form.linkStatus = row.linkStatus || row.status || 'PENDING';
  logoPreviewBroken.value = false;
}

function openCreateDialog() {
  editingId.value = '';
  resetForm();
  dialogVisible.value = true;
  formRef.value?.clearValidate();
}

function openEditDialog(row) {
  editingId.value = row.id;
  resetForm(row);
  dialogVisible.value = true;
  formRef.value?.clearValidate();
}

function isLogoBroken(row) {
  return brokenLogoIds.value.has(String(row.id || row.siteLogo));
}

function markLogoBroken(row) {
  const next = new Set(brokenLogoIds.value);
  next.add(String(row.id || row.siteLogo));
  brokenLogoIds.value = next;
}

function buildPayload() {
  return { ...form };
}

async function submitFriendLink() {
  try {
    await formRef.value?.validate();
  } catch {
    return;
  }

  submitLoading.value = true;

  try {
    if (editingId.value) {
      await updateFriendLink(editingId.value, buildPayload());
      ElMessage.success('友链已更新');
    } else {
      await createFriendLink(buildPayload());
      ElMessage.success('友链已新增');
    }
    dialogVisible.value = false;
    await loadFriendLinks();
  } catch (error) {
    ElMessage.error(error.message || '友链保存失败');
  } finally {
    submitLoading.value = false;
  }
}

async function changeFriendLinkStatus(row, status) {
  const label = statusLabel(status);
  try {
    await ElMessageBox.confirm(`确定将友链“${row.siteName || '-'}”设为${label}吗？`, '状态确认', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    });
    await updateFriendLinkStatus(row.id, status);
    ElMessage.success(`友链状态已更新为${label}`);
    await loadFriendLinks();
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error(error.message || '友链状态更新失败');
    }
  }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`确定删除友链“${row.siteName || '-'}”吗？`, '删除确认', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    });
    await deleteFriendLink(row.id);
    ElMessage.success('友链已删除');
    await loadFriendLinks();
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error(error.message || '友链删除失败');
    }
  }
}

function beforeLogoUpload(file) {
  const allowedTypes = ['image/jpeg', 'image/png', 'image/webp', 'image/gif'];
  if (!allowedTypes.includes(file.type)) {
    ElMessage.error('不支持的图片格式');
    return false;
  }
  if (file.size > 2 * 1024 * 1024) {
    ElMessage.error('图片大小不能超过 2MB');
    return false;
  }
  return true;
}

async function uploadLogo(options) {
  const file = options.file;
  uploadLoading.value = true;

  try {
    const response = await uploadFriendLinkLogo(file);
    const data = response?.data ?? response ?? {};
    const url = response?.url || data?.url || '';
    form.siteLogo = url;
    logoPreviewBroken.value = false;
    ElMessage.success('Logo 上传成功');
  } catch (error) {
    ElMessage.error(error.message || 'Logo 上传失败');
  } finally {
    uploadLoading.value = false;
  }
}

function statusLabel(status) {
  const labels = {
    PENDING: '待审核',
    APPROVED: '已通过',
    REJECTED: '已驳回'
  };
  return labels[String(status || '').toUpperCase()] || status || '-';
}

function formatDate(value) {
  if (!value) {
    return '-';
  }
  return String(value).replace('T', ' ').replace(/\.\d+$/, '').slice(0, 19);
}

onMounted(loadFriendLinks);

defineExpose({
  changeFriendLinkStatus,
  dialogVisible,
  form,
  handleDelete,
  loadFriendLinks,
  openCreateDialog,
  openEditDialog,
  submitFriendLink,
  uploadLogo
});
</script>

<style scoped>
.friend-link-logo-thumb {
  width: 38px;
  height: 38px;
  display: block;
  border: 1px solid #e8edf5;
  border-radius: 8px;
  object-fit: cover;
  background: #f7f9fc;
}

.logo-invalid-text {
  color: #ef4444;
  font-size: 12px;
}

.friend-link-form {
  padding-top: 4px;
}

.logo-uploader {
  display: flex;
  align-items: center;
  gap: 16px;
}

.logo-preview {
  width: 72px;
  height: 72px;
  display: grid;
  place-items: center;
  overflow: hidden;
  border: 1px dashed #cbd5e1;
  border-radius: 10px;
  color: #8a93a6;
  background: #f8fafc;
  font-size: 13px;
}

.logo-preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.logo-actions {
  display: grid;
  gap: 8px;
}

.logo-actions p {
  margin: 0;
  color: #8a93a6;
  font-size: 13px;
}
</style>
