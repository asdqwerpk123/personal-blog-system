<template>
  <section class="author-page">
    <div class="page-heading">
      <h1>个人资料</h1>
    </div>

    <section v-loading="loading" class="panel profile-panel">
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <div class="avatar-section">
          <div class="profile-avatar-preview">
            <img v-if="form.avatarUrl" :src="form.avatarUrl" alt="头像" />
            <span v-else>{{ userInitial }}</span>
          </div>
          <div class="avatar-upload-box" @click="fileInputRef?.click()">
            <el-icon><Picture /></el-icon>
            <p>将图片拖拽至此，或 <strong>点击选择文件</strong></p>
            <span>支持 JPG、JPEG、PNG、WEBP，图片大小不能超过 2MB</span>
          </div>
          <input ref="fileInputRef" class="sr-only" type="file" accept=".jpg,.jpeg,.png,.webp" @change="handleFileChange" />
          <el-button type="primary" :icon="Upload" :loading="avatarUploading" @click="fileInputRef?.click()">上传头像</el-button>
        </div>

        <el-divider />

        <div class="form-grid">
          <el-form-item label="用户名（不可修改）">
            <el-input v-model="form.userName" disabled />
          </el-form-item>
          <el-form-item label="昵称" prop="nickName">
            <el-input v-model.trim="form.nickName" />
          </el-form-item>
          <el-form-item label="邮箱" prop="email">
            <el-input v-model.trim="form.email" />
          </el-form-item>
          <el-form-item label="手机号" prop="phone">
            <el-input v-model.trim="form.phone" />
          </el-form-item>
        </div>

        <el-form-item label="个人简介" prop="introduction">
          <el-input v-model="form.introduction" type="textarea" :rows="4" maxlength="300" show-word-limit />
        </el-form-item>

        <div class="form-actions">
          <el-button type="primary" :icon="DocumentChecked" :loading="saving" @click="saveProfile">保存资料</el-button>
          <el-button :icon="Key" @click="passwordDialogVisible = true">修改密码</el-button>
        </div>
      </el-form>
    </section>

    <el-dialog v-model="passwordDialogVisible" title="修改密码" width="520px" destroy-on-close>
      <el-form ref="passwordFormRef" :model="passwordForm" :rules="passwordRules" label-position="top">
        <el-form-item label="当前密码" prop="currentPassword">
          <el-input v-model="passwordForm.currentPassword" type="password" show-password placeholder="请输入当前密码" />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="passwordForm.newPassword" type="password" show-password placeholder="请输入新密码（6-20 位）" />
        </el-form-item>
        <el-form-item label="确认新密码" prop="confirmPassword">
          <el-input v-model="passwordForm.confirmPassword" type="password" show-password placeholder="请再次输入新密码" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="passwordDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="passwordSaving" @click="changePassword">确认修改</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup>
import { DocumentChecked, Key, Picture, Upload } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import {
  getAuthorProfile,
  updateAuthorPassword,
  updateAuthorProfile,
  uploadAuthorAvatar
} from '@/api/profile.js';
import { useAuthStore } from '@/stores/auth.js';

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();
const formRef = ref();
const passwordFormRef = ref();
const fileInputRef = ref();
const loading = ref(false);
const saving = ref(false);
const avatarUploading = ref(false);
const passwordDialogVisible = ref(false);
const passwordSaving = ref(false);

const form = reactive({
  userName: '',
  nickName: '',
  email: '',
  phone: '',
  introduction: '',
  avatarUrl: ''
});

const passwordForm = reactive({
  currentPassword: '',
  newPassword: '',
  confirmPassword: ''
});

const rules = {
  nickName: [{ required: true, message: '请输入昵称', trigger: 'blur' }],
  email: [{ type: 'email', message: '邮箱格式不正确', trigger: 'blur' }]
};

function validateConfirmPassword(_rule, value, callback) {
  if (!value) {
    callback(new Error('请确认新密码'));
    return;
  }
  if (value !== passwordForm.newPassword) {
    callback(new Error('两次输入的新密码不一致'));
    return;
  }
  callback();
}

const passwordRules = {
  currentPassword: [{ required: true, message: '请输入当前密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 20, message: '新密码长度必须为 6-20 位', trigger: 'blur' }
  ],
  confirmPassword: [{ validator: validateConfirmPassword, trigger: 'blur' }]
};

const userInitial = computed(() => (form.nickName || form.userName || authStore.userName || '作').slice(0, 1).toUpperCase());

function extractData(response) {
  return response?.data ?? response ?? {};
}

function fillForm(profile) {
  form.userName = profile.userName || '';
  form.nickName = profile.nickName || '';
  form.email = profile.email || '';
  form.phone = profile.phone || '';
  form.introduction = profile.introduction || '';
  form.avatarUrl = profile.avatarUrl || '';
}

async function loadProfile() {
  loading.value = true;
  try {
    fillForm(extractData(await getAuthorProfile()));
  } catch (error) {
    ElMessage.error(error.message || '个人资料加载失败');
  } finally {
    loading.value = false;
  }
}

function validateAvatar(file) {
  const allowed = ['image/jpeg', 'image/png', 'image/webp'];
  const suffixAllowed = /\.(jpe?g|png|webp)$/i.test(file.name || '');
  if (!allowed.includes(file.type) && !suffixAllowed) {
    ElMessage.error('不支持的图片格式');
    return false;
  }
  if (file.size > 2 * 1024 * 1024) {
    ElMessage.error('图片大小不能超过 2MB');
    return false;
  }
  return true;
}

async function handleFileChange(event) {
  const file = event.target.files?.[0];
  event.target.value = '';
  if (!file || !validateAvatar(file)) {
    return;
  }

  avatarUploading.value = true;
  try {
    const data = extractData(await uploadAuthorAvatar(file));
    form.avatarUrl = data.url || data.avatarUrl || '';
    authStore.updateProfile({ nickName: form.nickName, avatarUrl: form.avatarUrl });
    ElMessage.success('头像上传成功');
  } catch (error) {
    ElMessage.error(error.message || '头像上传失败');
  } finally {
    avatarUploading.value = false;
  }
}

async function saveProfile() {
  try {
    await formRef.value?.validate();
  } catch {
    return;
  }

  saving.value = true;
  try {
    const profile = extractData(await updateAuthorProfile({
      nickName: form.nickName,
      email: form.email,
      phone: form.phone,
      introduction: form.introduction,
      avatarUrl: form.avatarUrl
    }));
    fillForm(profile);
    authStore.updateProfile({ nickName: form.nickName, avatarUrl: form.avatarUrl });
    ElMessage.success('个人资料已保存');
  } catch (error) {
    ElMessage.error(error.message || '个人资料保存失败');
  } finally {
    saving.value = false;
  }
}

function resetPasswordForm() {
  passwordForm.currentPassword = '';
  passwordForm.newPassword = '';
  passwordForm.confirmPassword = '';
}

async function changePassword() {
  try {
    await passwordFormRef.value?.validate();
  } catch {
    return;
  }

  passwordSaving.value = true;
  try {
    await updateAuthorPassword(passwordForm);
    ElMessage.success('密码修改成功，请重新登录');
    authStore.logout();
    router.replace('/login');
  } catch (error) {
    ElMessage.error(error.message || '当前密码错误');
  } finally {
    passwordSaving.value = false;
  }
}

watch(passwordDialogVisible, (visible) => {
  if (!visible) {
    resetPasswordForm();
  }
});

watch(
  () => route.query.tab,
  (tab) => {
    if (tab === 'password') {
      passwordDialogVisible.value = true;
    }
  },
  { immediate: true }
);

onMounted(loadProfile);
</script>

<style scoped>
.profile-panel {
  max-width: 760px;
  padding: 28px;
}

.avatar-section {
  display: grid;
  grid-template-columns: 112px minmax(260px, 1fr);
  gap: 18px 28px;
  align-items: center;
}

.profile-avatar-preview {
  width: 96px;
  height: 96px;
  display: grid;
  place-items: center;
  overflow: hidden;
  border-radius: 999px;
  color: #ffffff;
  font-size: 34px;
  font-weight: 800;
  background: #5966ff;
}

.profile-avatar-preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-upload-box {
  min-height: 118px;
  display: grid;
  place-items: center;
  align-content: center;
  gap: 8px;
  padding: 18px;
  border: 1px dashed #d5dced;
  border-radius: 12px;
  color: #8090aa;
  background: #fbfcff;
  cursor: pointer;
}

.avatar-upload-box p {
  margin: 0;
}

.avatar-upload-box strong {
  color: var(--color-primary);
}

.avatar-upload-box span {
  font-size: 13px;
}

.sr-only {
  position: absolute;
  width: 1px;
  height: 1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 22px;
}

.form-actions {
  display: flex;
  gap: 12px;
}

@media (max-width: 760px) {
  .avatar-section,
  .form-grid {
    grid-template-columns: 1fr;
  }
}
</style>
