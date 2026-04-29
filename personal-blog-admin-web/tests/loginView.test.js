import { mount } from '@vue/test-utils';
import ElementPlus from 'element-plus';
import { createPinia } from 'pinia';
import { describe, expect, it, vi } from 'vitest';
import { createMemoryHistory, createRouter } from 'vue-router';

import { login } from '../src/api/auth.js';
import LoginView from '../src/views/LoginView.vue';

vi.mock('../src/api/auth.js', () => ({
  login: vi.fn()
}));

describe('LoginView', () => {
  it('renders the Chinese product title, primary action, and register entry', async () => {
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [
        {
          path: '/login',
          component: LoginView
        },
        {
          path: '/register',
          component: { template: '<div>register</div>' }
        }
      ]
    });

    router.push('/login');
    await router.isReady();

    const wrapper = mount(LoginView, {
      global: {
        plugins: [createPinia(), router, ElementPlus]
      }
    });

    expect(wrapper.text()).toContain('个人博客管理系统');
    expect(wrapper.text()).toContain('登录');
    expect(wrapper.text()).toContain('立即注册');
  });

  it('redirects USER login to author dashboard', async () => {
    login.mockResolvedValue({
      code: 200,
      data: {
        accessToken: 'user-token',
        id: 5,
        userName: 'jerry',
        nickName: 'Jerry',
        roleCode: 'USER',
        roleName: '普通用户'
      }
    });

    const router = createRouter({
      history: createMemoryHistory(),
      routes: [
        { path: '/login', component: LoginView },
        { path: '/author/dashboard', component: { template: '<div>author</div>' } },
        { path: '/admin/dashboard', component: { template: '<div>admin</div>' } }
      ]
    });

    router.push('/login');
    await router.isReady();

    const wrapper = mount(LoginView, {
      global: {
        plugins: [createPinia(), router, ElementPlus]
      }
    });

    wrapper.vm.form.userName = 'jerry';
    wrapper.vm.form.password = '123456';
    await wrapper.vm.handleLogin();

    expect(router.currentRoute.value.path).toBe('/author/dashboard');
  });
});
