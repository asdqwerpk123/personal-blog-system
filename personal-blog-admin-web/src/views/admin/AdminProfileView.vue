<template>
  <section class="admin-page profile-page">
    <div class="page-heading">
      <div>
        <h1>个人资料</h1>
        <span>维护当前登录账号的资料、头像和密码</span>
      </div>
    </div>

    <el-tabs v-model="activeTab" class="profile-tabs">
      <el-tab-pane label="个人资料" name="profile">
        <div class="panel profile-panel">
          <el-form
            ref="profileFormRef"
            class="profile-form"
            :model="profileForm"
            :rules="profileRules"
            label-position="top"
          >
            <el-form-item label="头像">
              <div class="avatar-editor">
                <div class="avatar-preview">
                  <img
                    v-if="profileForm.avatarUrl && !avatarBroken"
                    :src="profileForm.avatarUrl"
                    alt="头像预览"
                    @error="avatarBroken = true"
                  />
                  <span v-else>{{ userInitial }}</span>
                </div>
                <el-upload
                  accept="image/jpeg,image/png,image/webp"
                  :before-upload="beforeAvatarUpload"
                  :http-request="uploadAvatarRequest"
                  :show-file-list="false"
                >
                  <el-button :loading="avatarUploading">上传头像</el-button>
                </el-upload>
              </div>
            </el-form-item>

            <el-form-item label="用户名">
              <el-input v-model="profileForm.userName" disabled />
            </el-form-item>
            <el-form-item label="昵称" prop="nickName">
              <el-input v-model.trim="profileForm.nickName" maxlength="50" placeholder="请输入昵称" />
            </el-form-item>
            <el-form-item label="邮箱" prop="email">
              <el-input v-model.trim="profileForm.email" maxlength="100" placeholder="请输入邮箱" />
            </el-form-item>
            <el-form-item label="手机号" prop="phone">
              <el-input v-model.trim="profileForm.phone" maxlength="20" placeholder="请输入手机号" />
            </el-form-item>
            <el-form-item label="个人简介">
              <el-input
                v-model="profileForm.introduction"
                maxlength="255"
                placeholder="请输入个人简介"
                resize="none"
                :rows="4"
                type="textarea"
              />
            </el-form-item>
            <div class="profile-actions">
              <el-button type="primary" :loading="profileSaving" @click="saveProfile">保存资料</el-button>
            </div>
          </el-form>
        </div>
      </el-tab-pane>

      <el-tab-pane label="修改密码" name="password">
        <div class="panel profile-panel">
          <el-form
            ref="passwordFormRef"
            class="profile-form"
            :model="passwordForm"
            :rules="passwordRules"
            label-position="top"
          >
            <el-form-item label="原密码" prop="oldPassword">
              <el-input v-model="passwordForm.oldPassword" type="password" show-password placeholder="请输入原密码" />
            </el-form-item>
            <el-form-item label="新密码" prop="newPassword">
              <el-input v-model="passwordForm.newPassword" type="password" show-password placeholder="请输入新密码" />
            </el-form-item>
            <el-form-item label="确认密码" prop="confirmPassword">
              <el-input v-model="passwordForm.confirmPassword" type="password" show-password placeholder="请再次输入新密码" />
            </el-form-item>
            <div class="profile-actions">
              <el-button type="primary" :loading="passwordSaving" @click="changePassword">修改密码</el-button>
            </div>
          </el-form>
        </div>
      </el-tab-pane>
    </el-tabs>
  </section>
</template>

<script setup>
import { ElMessage } from 'element-plus';
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { useRoute } from 'vue-router';

import { getMyProfile, updateMyPassword, updateMyProfile, uploadAvatar } from '@/api/profile.js';
import { useAuthStore } from '@/stores/auth.js';
import { unwrapData } from './pageData.js';

const route = useRoute();
const authStore = useAuthStore();

const activeTab = ref(route.query.tab === 'password' ? 'password' : 'profile');
const profileFormRef = ref(null);
const passwordFormRef = ref(null);
const profileSaving = ref(false);
const passwordSaving = ref(false);
const avatarUploading = ref(false);
const avatarBroken = ref(false);

const profileForm = reactive({
  userName: '',
  nickName: '',
  email: '',
  phone: '',
  avatarUrl: '',
  introduction: ''
});

const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
});

const userInitial = computed(() => (profileForm.nickName || profileForm.userName || '管').slice(0, 1).toUpperCase());

const profileRules = {
  nickName: [{ required: true, message: '请输入昵称', trigger: 'blur' }],
  email: [{ type: 'email', message: '请输入正确的邮箱', trigger: 'blur' }]
};

const passwordRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '新密码不少于 6 位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    {
      validator: (_, value, callback) => {
        if (value !== passwordForm.newPassword) {
          callback(new Error('两次输入的密码不一致'));
          return;
        }
        callback();
      },
      trigger: 'blur'
    }
  ]
};

watch(
  () => route.query.tab,
  (tab) => {
    activeTab.value = tab === 'password' ? 'password' : 'profile';
  }
);

function fillProfile(data = {}) {
  profileForm.userName = data.userName || '';
  profileForm.nickName = data.nickName || '';
  profileForm.email = data.email || '';
  profileForm.phone = data.phone || '';
  profileForm.avatarUrl = data.avatarUrl || '';
  profileForm.introduction = data.introduction || '';
  avatarBroken.value = false;
  authStore.updateProfile({
    nickName: profileForm.nickName,
    avatarUrl: profileForm.avatarUrl
  });
}

async function loadProfile() {
  try {
    fillProfile(unwrapData(await getMyProfile()));
  } catch (error) {
    ElMessage.error(error.message || '个人资料加载失败');
  }
}

function profilePayload() {
  return {
    nickName: profileForm.nickName,
    email: profileForm.email,
    phone: profileForm.phone,
    avatarUrl: profileForm.avatarUrl,
    introduction: profileForm.introduction
  };
}

async function saveProfile() {
  try {
    await profileFormRef.value?.validate();
  } catch {
    return;
  }

  profileSaving.value = true;
  try {
    const response = await updateMyProfile(profilePayload());
    fillProfile({
      ...profileForm,
      ...unwrapData(response)
    });
    ElMessage.success('个人资料已保存');
  } catch (error) {
    ElMessage.error(error.message || '个人资料保存失败');
  } finally {
    profileSaving.value = false;
  }
}

function beforeAvatarUpload(file) {
  const allowedTypes = ['image/jpeg', 'image/png', 'image/webp'];
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

async function uploadAvatarRequest(options) {
  avatarUploading.value = true;
  try {
    const response = await uploadAvatar(options.file);
    const data = response?.data ?? response ?? {};
    const url = response?.url || data?.url || '';
    profileForm.avatarUrl = url;
    avatarBroken.value = false;
    authStore.updateProfile({ avatarUrl: profileForm.avatarUrl });
    ElMessage.success('头像上传成功');
  } catch (error) {
    ElMessage.error(error.message || '头像上传失败');
  } finally {
    avatarUploading.value = false;
  }
}

async function changePassword() {
  try {
    await passwordFormRef.value?.validate();
  } catch {
    return;
  }

  passwordSaving.value = true;
  try {
    await updateMyPassword({
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword
    });
    passwordForm.oldPassword = '';
    passwordForm.newPassword = '';
    passwordForm.confirmPassword = '';
    passwordFormRef.value?.clearValidate();
    ElMessage.success('密码已修改，请牢记新密码');
  } catch (error) {
    ElMessage.error(error.message || '密码修改失败');
  } finally {
    passwordSaving.value = false;
  }
}

onMounted(loadProfile);

defineExpose({
  activeTab,
  changePassword,
  passwordForm,
  profileForm,
  saveProfile,
  uploadAvatarRequest
});
</script>

<style scoped>
.profile-page {
  min-height: calc(100vh - 108px);
}

.profile-tabs {
  max-width: 760px;
}

.profile-panel {
  padding: 24px;
}

.profile-form {
  max-width: 560px;
}

.avatar-editor {
  display: flex;
  align-items: center;
  gap: 18px;
}

.avatar-preview {
  width: 72px;
  height: 72px;
  display: grid;
  place-items: center;
  overflow: hidden;
  border-radius: 999px;
  color: #ffffff;
  background: #8b5cf6;
  font-size: 22px;
  font-weight: 700;
}

.avatar-preview img {
  width: 100%;
  height: 100%;
  display: block;
  object-fit: cover;
}

.profile-actions {
  display: flex;
  justify-content: flex-end;
  padding-top: 8px;
}
</style>
