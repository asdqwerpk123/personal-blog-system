<template>
  <view class="page auth-page">
    <view class="auth-card">
      <text class="auth-title">普通用户登录</text>
      <input v-model="form.userName" class="input field" placeholder="用户名" />
      <input v-model="form.password" class="input field" password placeholder="密码" />
      <button class="primary-button submit" :disabled="submitting" @tap="handleLogin">
        {{ submitting ? "登录中..." : "登录" }}
      </button>
      <button class="ghost-button" @tap="goRegister">还没有账号，去注册</button>
    </view>
  </view>
</template>

<script setup>
import { reactive, ref } from "vue"
import { login } from "../../api/auth.js"
import { clearLoginState, saveLoginState } from "../../utils/request.js"

const form = reactive({
  userName: "",
  password: ""
})
const submitting = ref(false)

async function handleLogin() {
  if (!form.userName.trim() || !form.password.trim()) {
    uni.showToast({ title: "请输入用户名和密码", icon: "none" })
    return
  }

  submitting.value = true
  try {
    const data = await login({
      userName: form.userName.trim(),
      password: form.password
    })
    if (!data || data.roleCode !== "USER") {
      clearLoginState()
      uni.showToast({ title: "仅普通用户可登录移动端", icon: "none" })
      return
    }
    saveLoginState(data)
    uni.showToast({ title: "登录成功", icon: "success" })
    setTimeout(() => {
      uni.switchTab({ url: "/pages/user/profile" })
    }, 300)
  } catch (error) {
    clearLoginState()
  } finally {
    submitting.value = false
  }
}

function goRegister() {
  uni.navigateTo({ url: "/pages/auth/register" })
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
