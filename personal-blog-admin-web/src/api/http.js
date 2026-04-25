import axios from 'axios';
import { ElMessage } from 'element-plus';

import { toChineseBusinessMessage } from '@/utils/businessErrors.js';
import { clearStoredAuth, getStoredToken } from '@/utils/authStorage.js';

const http = axios.create({
  baseURL: '',
  timeout: 15000
});

http.interceptors.request.use((config) => {
  const token = getStoredToken();
  const isLoginRequest = config.url === '/admin/auth/login';
  const isAdminRequest = config.url?.startsWith('/admin/');

  if (typeof FormData !== 'undefined' && config.data instanceof FormData) {
    if (typeof config.headers?.delete === 'function') {
      config.headers.delete('Content-Type');
      config.headers.set('Content-Type', false);
    } else if (config.headers) {
      config.headers['Content-Type'] = false;
    }
  }

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
      handleUnauthorizedAdminResponse(response.config, payload.code);

      return Promise.reject(new Error(toChineseBusinessMessage(payload.message || '请求失败')));
    }

    return payload;
  },
  (error) => {
    const status = error.response?.status;

    if (status === 401) {
      handleUnauthorizedAdminResponse(error.config, status);
    }

    if (error?.message) {
      error.message = toChineseBusinessMessage(error.message);
    }

    return Promise.reject(error);
  }
);

function handleUnauthorizedAdminResponse(config, code) {
  const requestUrl = config?.url || '';
  const isLoginRequest = requestUrl === '/admin/auth/login';
  const isAdminRequest = requestUrl.startsWith('/admin/');

  if (Number(code) === 401 && isAdminRequest && !isLoginRequest) {
    clearStoredAuth();
    ElMessage.error('登录已过期，请重新登录');
    if (window.location.pathname !== '/login') {
      window.location.href = `/login?redirect=${encodeURIComponent(window.location.pathname)}`;
    }
  }
}

export default http;
