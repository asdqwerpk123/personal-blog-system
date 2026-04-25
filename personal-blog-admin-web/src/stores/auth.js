import { defineStore } from 'pinia';

import { login as loginApi } from '@/api/auth.js';
import {
  clearStoredAuth,
  extractAccessToken,
  getStoredAuth,
  persistAuth
} from '@/utils/authStorage.js';

export const useAuthStore = defineStore('auth', {
  state: () => {
    const stored = getStoredAuth();

    return {
      token: stored.token,
      userName: stored.userName,
      userId: stored.userId,
      nickName: stored.nickName,
      roleId: stored.roleId,
      roleCode: stored.roleCode,
      roleName: stored.roleName,
      avatarUrl: stored.avatarUrl,
      remember: stored.remember
    };
  },
  getters: {
    isAuthenticated: (state) => Boolean(state.token)
  },
  actions: {
    restore() {
      const stored = getStoredAuth();
      this.token = stored.token;
      this.userName = stored.userName;
      this.userId = stored.userId;
      this.nickName = stored.nickName;
      this.roleId = stored.roleId;
      this.roleCode = stored.roleCode;
      this.roleName = stored.roleName;
      this.avatarUrl = stored.avatarUrl;
      this.remember = stored.remember;
    },
    async login({ userName, password, remember }) {
      const response = await loginApi({ userName, password });
      const token = extractAccessToken(response);

      if (!token) {
        throw new Error('登录响应缺少 accessToken');
      }

      this.token = token;
      this.userName = response?.data?.userName || userName;
      this.userId = response?.data?.id ? String(response.data.id) : '';
      this.nickName = response?.data?.nickName || '';
      this.roleId = response?.data?.roleId ? String(response.data.roleId) : '';
      this.roleCode = response?.data?.roleCode || '';
      this.roleName = response?.data?.roleName || '';
      this.avatarUrl = response?.data?.avatarUrl || '';
      this.remember = remember;
      persistAuth({
        token,
        userName: this.userName,
        userId: this.userId,
        nickName: this.nickName,
        roleId: this.roleId,
        roleCode: this.roleCode,
        roleName: this.roleName,
        avatarUrl: this.avatarUrl,
        remember
      });
    },
    updateProfile(profile = {}) {
      this.nickName = profile.nickName ?? this.nickName;
      this.avatarUrl = profile.avatarUrl ?? this.avatarUrl;
      persistAuth({
        token: this.token,
        userName: this.userName,
        userId: this.userId,
        nickName: this.nickName,
        roleId: this.roleId,
        roleCode: this.roleCode,
        roleName: this.roleName,
        avatarUrl: this.avatarUrl,
        remember: this.remember
      });
    },
    logout() {
      clearStoredAuth();
      this.token = '';
      this.userName = '';
      this.userId = '';
      this.nickName = '';
      this.roleId = '';
      this.roleCode = '';
      this.roleName = '';
      this.avatarUrl = '';
    }
  }
});
