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
      this.remember = stored.remember;
    },
    async login({ userName, password, remember }) {
      const response = await loginApi({ userName, password });
      const token = extractAccessToken(response);

      if (!token) {
        throw new Error('登录响应缺少 accessToken');
      }

      this.token = token;
      this.userName = userName;
      this.remember = remember;
      persistAuth({ token, userName, remember });
    },
    logout() {
      clearStoredAuth();
      this.token = '';
      this.userName = '';
    }
  }
});
