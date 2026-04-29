<template>
  <main class="login-page">
    <section class="login-card register-card" aria-labelledby="register-title">
      <div class="login-logo">
        <Notebook />
      </div>

      <h1 id="register-title">个人博客管理系统</h1>
      <p class="login-subtitle">注册普通作者账号</p>

      <el-form ref="formRef" class="login-form register-form" :model="form" :rules="rules" label-position="top">
        <el-form-item label="用户名" prop="userName">
          <el-input v-model.trim="form.userName" size="large" placeholder="3-20 位用户名" :prefix-icon="User" />
        </el-form-item>

        <el-form-item label="昵称" prop="nickName">
          <el-input v-model.trim="form.nickName" size="large" placeholder="请输入昵称" />
        </el-form-item>

        <el-form-item label="密码" prop="password">
          <el-input
            v-model="form.password"
            size="large"
            placeholder="6-20 位密码"
            type="password"
            show-password
            :prefix-icon="Lock"
          />
        </el-form-item>

        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input
            v-model="form.confirmPassword"
            size="large"
            placeholder="请再次输入密码"
            type="password"
            show-password
            :prefix-icon="Lock"
            @keyup.enter="handleRegister"
          />
        </el-form-item>

        <el-form-item label="邮箱" prop="email">
          <el-input v-model.trim="form.email" size="large" placeholder="可选" />
        </el-form-item>

        <el-form-item label="手机号" prop="phone">
          <el-input v-model.trim="form.phone" size="large" placeholder="可选" />
        </el-form-item>

        <el-button class="login-button" type="primary" size="large" :loading="loading" @click="handleRegister">
          注册
        </el-button>

        <p class="login-register-entry">
          已有账号？
          <RouterLink to="/login">返回登录</RouterLink>
        </p>
      </el-form>
    </section>
  </main>
</template>

<script setup>
import { Lock, Notebook, User } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import { reactive, ref } from 'vue';
import { RouterLink, useRouter } from 'vue-router';

import { register } from '@/api/auth.js';

const router = useRouter();
const formRef = ref();
const loading = ref(false);

const form = reactive({
  userName: '',
  password: '',
  confirmPassword: '',
  nickName: '',
  email: '',
  phone: ''
});

function validateConfirmPassword(_rule, value, callback) {
  if (!value) {
    callback(new Error('请确认密码'));
    return;
  }
  if (value !== form.password) {
    callback(new Error('两次输入的密码不一致'));
    return;
  }
  callback();
}

const rules = {
  userName: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度必须为 3-20 位', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度必须为 6-20 位', trigger: 'blur' }
  ],
  confirmPassword: [{ validator: validateConfirmPassword, trigger: 'blur' }],
  nickName: [{ required: true, message: '请输入昵称', trigger: 'blur' }]
};

async function validateForm() {
  if (!formRef.value) {
    return Boolean(form.userName && form.password && form.confirmPassword && form.nickName);
  }
  return formRef.value.validate().catch(() => false);
}

async function handleRegister() {
  const valid = await validateForm();

  if (!valid) {
    return;
  }

  loading.value = true;

  try {
    await register({
      userName: form.userName,
      password: form.password,
      nickName: form.nickName,
      email: form.email,
      phone: form.phone
    });
    ElMessage.success('注册成功，请登录');
    await router.replace('/login');
  } catch (error) {
    ElMessage.error(error.message || '注册失败，请稍后重试');
  } finally {
    loading.value = false;
  }
}
</script>
