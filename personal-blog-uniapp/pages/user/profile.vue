<template>
  <view class="page profile-page">
    <view class="profile-card">
      <image class="avatar" :src="avatarUrl" mode="aspectFill" @tap="chooseAvatar" />
      <view class="profile-info">
        <text class="name">{{ isLoggedIn ? displayName : "未登录" }}</text>
        <text class="hint">
          {{ isLoggedIn ? "管理个人资料、头像和评论记录" : "登录后可发表评论和管理个人资料" }}
        </text>
      </view>
    </view>

    <view v-if="!isLoggedIn" class="action-stack">
      <button class="primary-button" @tap="goLogin">登录</button>
      <button class="ghost-button" @tap="goRegister">注册</button>
    </view>

    <view v-else>
      <view class="info-list">
        <view class="info-row">
          <text>用户名</text>
          <text>{{ userInfo.userName || "-" }}</text>
        </view>
        <view class="info-row">
          <text>昵称</text>
          <text>{{ userInfo.nickName || "-" }}</text>
        </view>
        <view v-if="userInfo.email" class="info-row">
          <text>邮箱</text>
          <text>{{ userInfo.email }}</text>
        </view>
        <view v-if="userInfo.phone" class="info-row">
          <text>手机号</text>
          <text>{{ userInfo.phone }}</text>
        </view>
        <view class="info-row block-row">
          <text>简介</text>
          <text class="intro">{{ userInfo.introduction || "暂无简介" }}</text>
        </view>
      </view>

      <view class="profile-actions">
        <button class="ghost-button" :disabled="uploading" @tap="chooseAvatar">
          {{ uploading ? "上传中..." : "更换头像" }}
        </button>
        <button class="ghost-button" @tap="goComments">我的评论</button>
      </view>

      <view class="edit-panel">
        <text class="section-title">编辑资料</text>
        <input v-model="form.nickName" class="input field" placeholder="昵称" />
        <input v-model="form.email" class="input field" placeholder="邮箱" />
        <input v-model="form.phone" class="input field" placeholder="手机号" />
        <textarea v-model="form.introduction" class="textarea field" maxlength="200" placeholder="简介" />
        <button class="primary-button" :disabled="saving" @tap="saveProfile">
          {{ saving ? "保存中..." : "保存资料" }}
        </button>
      </view>

      <button class="ghost-button logout" @tap="logout">退出登录</button>
    </view>
  </view>
</template>

<script setup>
import { computed, reactive, ref } from "vue"
import { onShow } from "@dcloudio/uni-app"
import { uploadAvatar } from "../../api/file.js"
import { getProfile, updateProfile } from "../../api/user.js"
import { defaultImages, resolveAssetUrl } from "../../utils/config.js"
import {
  clearLoginState,
  getRoleCode,
  getStoredUserInfo,
  getToken,
  saveUserInfo
} from "../../utils/request.js"

const token = ref("")
const roleCode = ref("")
const userInfo = ref({})
const loading = ref(false)
const saving = ref(false)
const uploading = ref(false)
const form = reactive({
  nickName: "",
  email: "",
  phone: "",
  introduction: ""
})

const isLoggedIn = computed(() => Boolean(token.value && roleCode.value === "USER"))
const displayName = computed(() => userInfo.value.nickName || userInfo.value.userName || "普通用户")
const avatarUrl = computed(() => resolveAssetUrl(userInfo.value.avatarUrl, defaultImages.avatar))

onShow(() => {
  syncLocalState()
  if (isLoggedIn.value) {
    loadProfile()
  }
})

function syncLocalState() {
  token.value = getToken()
  roleCode.value = getRoleCode()
  userInfo.value = getStoredUserInfo()
  fillForm(userInfo.value)
}

function fillForm(profile) {
  form.nickName = profile.nickName || ""
  form.email = profile.email || ""
  form.phone = profile.phone || ""
  form.introduction = profile.introduction || ""
}

async function loadProfile() {
  if (loading.value) {
    return
  }
  loading.value = true
  try {
    const profile = await getProfile()
    userInfo.value = saveUserInfo(profile)
    fillForm(userInfo.value)
  } finally {
    loading.value = false
  }
}

async function saveProfile() {
  if (!isLoggedIn.value) {
    goLogin()
    return
  }
  saving.value = true
  try {
    const profile = await updateProfile({
      nickName: form.nickName.trim(),
      email: form.email.trim(),
      phone: form.phone.trim(),
      introduction: form.introduction.trim(),
      avatarUrl: userInfo.value.avatarUrl || ""
    })
    userInfo.value = saveUserInfo(profile)
    fillForm(userInfo.value)
    uni.showToast({ title: "资料已保存", icon: "success" })
  } finally {
    saving.value = false
  }
}

function chooseAvatar() {
  if (!isLoggedIn.value) {
    goLogin()
    return
  }
  uni.chooseImage({
    count: 1,
    sizeType: ["compressed"],
    sourceType: ["album", "camera"],
    success: async (result) => {
      const filePath = result.tempFilePaths && result.tempFilePaths[0]
      if (filePath) {
        await uploadSelectedAvatar(filePath)
      }
    },
    fail: (error) => {
      if (error && error.errMsg && !error.errMsg.includes("cancel")) {
        uni.showToast({ title: "选择图片失败", icon: "none" })
      }
    }
  })
}

async function uploadSelectedAvatar(filePath) {
  uploading.value = true
  try {
    const result = await uploadAvatar(filePath)
    const nextUserInfo = saveUserInfo({
      ...userInfo.value,
      avatarUrl: result && result.url ? result.url : userInfo.value.avatarUrl
    })
    userInfo.value = nextUserInfo
    await loadProfile()
    uni.showToast({ title: "头像已更新", icon: "success" })
  } finally {
    uploading.value = false
  }
}

function goLogin() {
  uni.navigateTo({ url: "/pages/auth/login" })
}

function goRegister() {
  uni.navigateTo({ url: "/pages/auth/register" })
}

function goComments() {
  uni.navigateTo({ url: "/pages/user/comments" })
}

function logout() {
  clearLoginState()
  token.value = ""
  roleCode.value = ""
  userInfo.value = {}
  fillForm({})
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

.action-stack,
.profile-actions {
  display: flex;
  gap: 18rpx;
  flex-direction: column;
  margin-top: 30rpx;
}

.info-list,
.edit-panel {
  margin-top: 30rpx;
  padding: 0 24rpx 24rpx;
  border: 1rpx solid #dfe8dc;
  border-radius: 12rpx;
  background: #fff;
}

.info-row {
  display: flex;
  justify-content: space-between;
  gap: 24rpx;
  padding: 24rpx 0;
  border-bottom: 1rpx solid #e5ece3;
  color: #1f2b24;
  font-size: 28rpx;
}

.block-row {
  display: block;
}

.intro {
  display: block;
  margin-top: 12rpx;
  color: #6c7668;
  line-height: 1.5;
}

.field {
  margin-bottom: 20rpx;
}

.textarea {
  box-sizing: border-box;
  width: 100%;
  min-height: 160rpx;
  padding: 20rpx 24rpx;
  border: 1rpx solid #d7dfd4;
  border-radius: 10rpx;
  background: #fff;
  color: #1f2b24;
  font-size: 28rpx;
}

.logout {
  margin-top: 24rpx;
}
</style>
