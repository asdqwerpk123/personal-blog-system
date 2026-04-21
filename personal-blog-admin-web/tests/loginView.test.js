import { mount } from '@vue/test-utils';
import ElementPlus from 'element-plus';
import { createPinia } from 'pinia';
import { describe, expect, it } from 'vitest';
import { createMemoryHistory, createRouter } from 'vue-router';

import LoginView from '../src/views/LoginView.vue';

describe('LoginView', () => {
  it('renders the Chinese product title and primary action', async () => {
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [
        {
          path: '/login',
          component: LoginView
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
  });
});
