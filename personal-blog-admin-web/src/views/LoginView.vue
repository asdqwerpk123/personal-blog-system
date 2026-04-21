<template>
  <main class="login-page">
    <section class="login-card" aria-labelledby="login-title">
      <div class="login-logo">
        <Notebook />
      </div>

      <h1 id="login-title">个人博客管理系统</h1>
      <p class="login-subtitle">欢迎登录后台管理系统</p>

      <el-form ref="formRef" class="login-form" :model="form" :rules="rules" label-position="top" @submit.prevent>
        <el-form-item label="用户名" prop="userName">
          <el-input v-model.trim="form.userName" size="large" placeholder="请输入用户名" :prefix-icon="User" />
        </el-form-item>

        <el-form-item label="密码" prop="password">
          <el-input
            v-model="form.password"
            size="large"
            placeholder="请输入密码"
            type="password"
            show-password
            :prefix-icon="Lock"
            @keyup.enter="handleLogin"
          />
        </el-form-item>

        <div class="login-options">
          <el-checkbox v-model="form.remember">记住我</el-checkbox>
          <a href="#" @click.prevent>忘记密码?</a>
        </div>

        <el-button class="login-button" type="primary" size="large" :loading="loading" @click="handleLogin">
          登录
        </el-button>
      </el-form>
    </section>
  </main>
</template>

<script setup>
import { Lock, Notebook, User } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import { reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import { useAuthStore } from '@/stores/auth.js';

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();
const formRef = ref();
const loading = ref(false);

const form = reactive({
  userName: '',
  password: '',
  remember: false
});

const rules = {
  userName: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
};

async function handleLogin() {
  const valid = await formRef.value?.validate().catch(() => false);

  if (!valid) {
    return;
  }

  loading.value = true;

  try {
    await authStore.login(form);
    ElMessage.success('登录成功');
    router.replace(route.query.redirect || '/admin/dashboard');
  } catch (error) {
    ElMessage.error(error.message || '登录失败，请检查用户名和密码');
  } finally {
    loading.value = false;
  }
}
</script>
