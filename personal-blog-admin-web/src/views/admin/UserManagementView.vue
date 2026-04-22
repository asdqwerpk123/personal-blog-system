<template>
  <section class="admin-page">
    <div class="page-heading">
      <div>
        <h1>用户管理</h1>
        <span>维护后台账号、角色分配、启禁状态和重置密码</span>
      </div>
      <el-button type="primary" @click="openCreate">新增用户</el-button>
    </div>

    <div class="panel admin-list-panel">
      <div class="admin-toolbar">
        <el-input
          v-model.trim="query.keyword"
          class="toolbar-keyword"
          clearable
          placeholder="搜索用户名、昵称"
          @keyup.enter="loadUsers"
        />
        <el-button type="primary" @click="loadUsers">搜索</el-button>
      </div>

      <el-table v-loading="loading" :data="users" table-layout="fixed">
        <el-table-column label="用户" min-width="180">
          <template #default="{ row }">
            <strong>{{ row.nickName || row.userName }}</strong>
            <span class="muted-line">{{ row.userName }}</span>
          </template>
        </el-table-column>
        <el-table-column label="角色" min-width="130">
          <template #default="{ row }">{{ row.roleName || roleName(row.roleId) || '-' }}</template>
        </el-table-column>
        <el-table-column prop="email" label="邮箱" min-width="190" />
        <el-table-column prop="phone" label="手机号" min-width="140" />
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="isDisabled(row) ? 'danger' : 'success'">{{ isDisabled(row) ? '禁用' : '启用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" min-width="170" />
        <el-table-column label="操作" width="280" align="right" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link :type="isDisabled(row) ? 'success' : 'warning'" @click="toggleStatus(row)">
              {{ isDisabled(row) ? '启用' : '启禁' }}
            </el-button>
            <el-button link type="primary" @click="openPassword(row)">重置密码</el-button>
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
          @current-change="loadUsers"
          @size-change="loadUsers"
        />
      </div>
    </div>

    <el-dialog v-model="formVisible" :title="editingId ? '编辑用户' : '新增用户'" width="560px">
      <el-form label-position="top" :model="form">
        <el-form-item label="用户名">
          <el-input v-model.trim="form.userName" :disabled="Boolean(editingId)" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="昵称">
          <el-input v-model.trim="form.nickName" placeholder="请输入昵称" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model.trim="form.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model.trim="form.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item v-if="!editingId" label="初始密码">
          <el-input v-model="form.password" type="password" show-password placeholder="请输入初始密码" />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="form.roleId" class="full-select" placeholder="选择可分配角色">
            <el-option v-for="role in roles" :key="role.id" :label="role.roleName" :value="role.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.userStatus" class="full-select" placeholder="选择状态">
            <el-option label="启用" value="ENABLED" />
            <el-option label="禁用" value="DISABLED" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" @click="saveUser">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="passwordVisible" title="重置密码" width="420px">
      <el-form label-position="top">
        <el-form-item label="新密码">
          <el-input v-model="newPassword" type="password" show-password placeholder="请输入新密码" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="passwordVisible = false">取消</el-button>
        <el-button type="primary" @click="savePassword">确认重置</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup>
import { ElMessage, ElMessageBox } from 'element-plus';
import { onMounted, reactive, ref } from 'vue';

import { getRoleList } from '@/api/roles.js';
import { createUser, getUserPage, resetUserPassword, updateUser, updateUserStatus } from '@/api/users.js';
import { normalizePage, unwrapData } from './pageData.js';

const loading = ref(false);
const users = ref([]);
const roles = ref([]);
const total = ref(0);
const formVisible = ref(false);
const passwordVisible = ref(false);
const editingId = ref('');
const passwordUserId = ref('');
const newPassword = ref('');
const query = reactive({ current: 1, size: 10, keyword: '' });
const form = reactive({
  userName: '',
  nickName: '',
  email: '',
  phone: '',
  password: '',
  roleId: '',
  userStatus: 'ENABLED'
});

function roleName(roleId) {
  return roles.value.find((role) => String(role.id) === String(roleId))?.roleName;
}

function isDisabled(row) {
  return ['DISABLED', 0, false].includes(row.userStatus);
}

function resetForm(row = {}) {
  form.userName = row.userName || '';
  form.nickName = row.nickName || '';
  form.email = row.email || '';
  form.phone = row.phone || '';
  form.password = '';
  form.roleId = row.roleId || '';
  form.userStatus = row.userStatus || 'ENABLED';
}

async function loadUsers() {
  loading.value = true;

  try {
    const page = normalizePage(await getUserPage({ ...query }));
    users.value = page.records;
    total.value = page.total;
  } finally {
    loading.value = false;
  }
}

async function loadRoles() {
  const data = unwrapData(await getRoleList());
  roles.value = Array.isArray(data) ? data : data.records || data.list || [];
}

function openCreate() {
  editingId.value = '';
  resetForm();
  formVisible.value = true;
}

function openEdit(row) {
  editingId.value = row.id;
  resetForm(row);
  formVisible.value = true;
}

async function saveUser() {
  const payload = { ...form };

  if (editingId.value) {
    delete payload.password;
    await updateUser(editingId.value, payload);
  } else {
    await createUser(payload);
  }

  ElMessage.success('用户已保存');
  formVisible.value = false;
  await loadUsers();
}

async function toggleStatus(row) {
  const nextStatus = isDisabled(row) ? 'ENABLED' : 'DISABLED';
  await ElMessageBox.confirm(`确认${nextStatus === 'DISABLED' ? '禁用' : '启用'}该用户？`, '启禁确认', { type: 'warning' });
  await updateUserStatus(row.id, nextStatus);
  ElMessage.success('用户状态已更新');
  await loadUsers();
}

function openPassword(row) {
  passwordUserId.value = row.id;
  newPassword.value = '';
  passwordVisible.value = true;
}

async function savePassword() {
  await resetUserPassword(passwordUserId.value, newPassword.value);
  ElMessage.success('密码已重置');
  passwordVisible.value = false;
}

onMounted(async () => {
  await Promise.all([loadUsers(), loadRoles()]);
});
</script>
