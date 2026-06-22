import axios from 'axios';
import { ElMessage } from 'element-plus';

import { toChineseBusinessMessage } from '@/utils/businessErrors.js';
import { clearStoredAuth, getStoredToken } from '@/utils/authStorage.js';

const apiBaseUrl = import.meta.env.VITE_API_BASE_URL || '/api';

const http = axios.create({
  baseURL: apiBaseUrl,
  timeout: 15000
});

http.interceptors.request.use((config) => {
  const token = getStoredToken();
  const isLoginRequest = config.url === '/admin/auth/login';
  const isRegisterRequest = config.url === '/user/auth/register';
  const isAdminRequest = config.url?.startsWith('/admin/');
  const isUserRequest = config.url?.startsWith('/user/');

  if (typeof FormData !== 'undefined' && config.data instanceof FormData) {
    if (typeof config.headers?.delete === 'function') {
      config.headers.delete('Content-Type');
      config.headers.set('Content-Type', false);
    } else if (config.headers) {
      config.headers['Content-Type'] = false;
    }
  }

  if (token && (isAdminRequest || isUserRequest) && !isLoginRequest && !isRegisterRequest) {
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
      handleUnauthorizedResponse(response.config, payload.code);

      return Promise.reject(new Error(toChineseBusinessMessage(payload.message || '请求失败')));
    }

    return payload;
  },
  (error) => {
    const status = error.response?.status;

    if (status === 401) {
      handleUnauthorizedResponse(error.config, status);
    }

    if (error?.message) {
      error.message = toChineseBusinessMessage(error.message);
    }

    return Promise.reject(error);
  }
);

function handleUnauthorizedResponse(config, code) {
  const requestUrl = config?.url || '';
  const isLoginRequest = requestUrl === '/admin/auth/login';
  const isRegisterRequest = requestUrl === '/user/auth/register';
  const isAdminRequest = requestUrl.startsWith('/admin/');
  const isUserRequest = requestUrl.startsWith('/user/');

  if (Number(code) === 401 && (isAdminRequest || isUserRequest) && !isLoginRequest && !isRegisterRequest) {
    clearStoredAuth();
    ElMessage.error('登录已过期，请重新登录');
    if (window.location.pathname !== '/login') {
      redirectToLogin(window.location.pathname);
    }
  }
}

function redirectToLogin(currentPath) {
  const target = `/login?redirect=${encodeURIComponent(currentPath)}`;

  if (window.navigator?.userAgent?.includes('jsdom')) {
    window.history.pushState({}, '', target);
    return;
  }

  window.location.href = target;
}

export default http;
