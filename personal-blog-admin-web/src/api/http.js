import axios from 'axios';
import { ElMessage } from 'element-plus';

import { clearStoredAuth, getStoredToken } from '@/utils/authStorage.js';

const http = axios.create({
  baseURL: '',
  timeout: 15000
});

http.interceptors.request.use((config) => {
  const token = getStoredToken();
  const isLoginRequest = config.url === '/admin/auth/login';
  const isAdminRequest = config.url?.startsWith('/admin/');

  if (token && isAdminRequest && !isLoginRequest) {
    config.headers.Authorization = `Bearer ${token}`;
  }

  return config;
});

http.interceptors.response.use(
  (response) => {
    const payload = response.data;

    if (payload && typeof payload === 'object' && 'code' in payload) {
      if (payload.code === 200) {
        return payload;
      }

      return Promise.reject(new Error(payload.message || '请求失败'));
    }

    return payload;
  },
  (error) => {
    const status = error.response?.status;
    const requestUrl = error.config?.url || '';
    const isLoginRequest = requestUrl === '/admin/auth/login';
    const isAdminRequest = requestUrl.startsWith('/admin/');

    if (status === 401 && isAdminRequest && !isLoginRequest) {
      clearStoredAuth();
      ElMessage.error('登录已过期，请重新登录');
      window.location.href = `/login?redirect=${encodeURIComponent(window.location.pathname)}`;
    }

    return Promise.reject(error);
  }
);

export default http;
