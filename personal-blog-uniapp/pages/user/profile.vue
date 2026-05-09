<template>
  <view class="page profile-page">
    <view class="profile-card">
      <image class="avatar" :src="avatarUrl" mode="aspectFill" />
      <view class="profile-info">
        <text class="name">{{ isLoggedIn ? displayName : "未登录" }}</text>
        <text class="hint">{{ isLoggedIn ? "已保存本地登录态" : "登录后可发表评论和管理个人资料" }}</text>
      </view>
    </view>

    <view v-if="!isLoggedIn" class="action-stack">
      <button class="primary-button" @tap="goLogin">登录</button>
      <button class="ghost-button" @tap="goRegister">注册</button>
    </view>

    <view v-else class="info-list">
      <view class="info-row">
        <text>账号</text>
        <text>{{ userInfo.userName || "-" }}</text>
      </view>
      <view class="info-row">
        <text>角色</text>
        <text>{{ roleCode }}</text>
      </view>
      <view class="info-row">
        <text>个人资料</text>
        <text class="muted">P2 完善</text>
      </view>
      <button class="ghost-button logout" @tap="logout">退出登录</button>
    </view>
  </view>
</template>

<script setup>
import { computed, ref } from "vue"
import { onShow } from "@dcloudio/uni-app"
import { defaultImages, resolveAssetUrl, storageKeys } from "../../utils/config.js"
import { clearLoginState } from "../../utils/request.js"

const token = ref("")
const roleCode = ref("")
const userInfo = ref({})

const isLoggedIn = computed(() => Boolean(token.value && roleCode.value === "USER"))
const displayName = computed(() => userInfo.value.nickName || userInfo.value.userName || "普通用户")
const avatarUrl = computed(() => resolveAssetUrl(userInfo.value.avatarUrl, defaultImages.avatar))

onShow(() => {
  token.value = uni.getStorageSync(storageKeys.token) || ""
  roleCode.value = uni.getStorageSync(storageKeys.roleCode) || ""
  userInfo.value = uni.getStorageSync(storageKeys.userInfo) || {}
})

function goLogin() {
  uni.navigateTo({ url: "/pages/auth/login" })
}

function goRegister() {
  uni.navigateTo({ url: "/pages/auth/register" })
}

function logout() {
  clearLoginState()
  token.value = ""
  roleCode.value = ""
  userInfo.value = {}
}
</script>

<style scoped>
.profile-card {
  display: flex;
  align-items: center;
  gap: 24rpx;
  padding: 30rpx;
  border: 1rpx solid #dfe8dc;
  border-radius: 12rpx;
  background: #fff;
}

.avatar {
  width: 116rpx;
  height: 116rpx;
  border-radius: 50%;
  background: #edf4eb;
  flex-shrink: 0;
}

.profile-info {
  min-width: 0;
}

.name {
  display: block;
  color: #1f2b24;
  font-size: 36rpx;
  font-weight: 800;
}

.hint {
  display: block;
  margin-top: 10rpx;
  color: #6c7668;
  font-size: 26rpx;
  line-height: 1.45;
}

.action-stack {
  display: flex;
  gap: 18rpx;
  flex-direction: column;
  margin-top: 30rpx;
}

.info-list {
  margin-top: 30rpx;
  padding: 0 24rpx 24rpx;
  border: 1rpx solid #dfe8dc;
  border-radius: 12rpx;
  background: #fff;
}

.info-row {
  display: flex;
  justify-content: space-between;
  padding: 24rpx 0;
  border-bottom: 1rpx solid #e5ece3;
  color: #1f2b24;
  font-size: 28rpx;
}

.logout {
  margin-top: 24rpx;
}
</style>
