<template>
  <view class="page auth-page">
    <view class="auth-card">
      <text class="auth-title">注册普通用户</text>
      <input v-model="form.userName" class="input field" placeholder="用户名 3-20 位" />
      <input v-model="form.nickName" class="input field" placeholder="昵称" />
      <input v-model="form.password" class="input field" password placeholder="密码 6-20 位" />
      <input v-model="confirmPassword" class="input field" password placeholder="确认密码" />
      <button class="primary-button submit" :disabled="submitting" @tap="handleRegister">
        {{ submitting ? "注册中..." : "注册" }}
      </button>
      <button class="ghost-button" @tap="goLogin">已有账号，去登录</button>
    </view>
  </view>
</template>

<script setup>
import { reactive, ref } from "vue"
import { register } from "../../api/auth.js"

const form = reactive({
  userName: "",
  nickName: "",
  password: ""
})
const confirmPassword = ref("")
const submitting = ref(false)

async function handleRegister() {
  if (!form.userName.trim() || !form.password.trim()) {
    uni.showToast({ title: "请输入用户名和密码", icon: "none" })
    return
  }
  if (form.password !== confirmPassword.value) {
    uni.showToast({ title: "两次密码不一致", icon: "none" })
    return
  }

  submitting.value = true
  try {
    await register({
      userName: form.userName.trim(),
      password: form.password,
      nickName: form.nickName.trim() || form.userName.trim()
    })
    uni.showToast({ title: "注册成功", icon: "success" })
    setTimeout(() => {
      uni.redirectTo({ url: "/pages/auth/login" })
    }, 300)
  } finally {
    submitting.value = false
  }
}

function goLogin() {
  uni.redirectTo({ url: "/pages/auth/login" })
}
</script>

<style scoped>
.auth-page {
  display: flex;
  justify-content: center;
  padding-top: 70rpx;
}

.auth-card {
  width: 100%;
  padding: 34rpx;
  border: 1rpx solid #dfe8dc;
  border-radius: 12rpx;
  background: #fff;
  box-sizing: border-box;
}

.auth-title {
  display: block;
  margin-bottom: 30rpx;
  color: #1f2b24;
  font-size: 40rpx;
  font-weight: 800;
}

.field {
  margin-bottom: 20rpx;
}

.submit {
  margin: 10rpx 0 18rpx;
}
</style>
