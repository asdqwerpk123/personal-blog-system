import { mount } from '@vue/test-utils';
import ElementPlus from 'element-plus';
import { createPinia } from 'pinia';
import { describe, expect, it } from 'vitest';
import { nextTick } from 'vue';
import { createMemoryHistory, createRouter } from 'vue-router';

import AdminLayout from '../src/layouts/AdminLayout.vue';
import { useAuthStore } from '../src/stores/auth.js';

function createTestRouter() {
  return createRouter({
    history: createMemoryHistory(),
    routes: [
      {
        path: '/admin',
        component: AdminLayout,
        children: [
          { path: 'dashboard', component: { template: '<div>仪表盘</div>' } },
          { path: 'profile', component: { template: '<div>个人资料页</div>' } }
        ]
      },
      { path: '/login', component: { template: '<div>登录</div>' } }
    ]
  });
}

describe('AdminLayout profile menu', () => {
  it('removes the global topbar search and navigates from the profile dropdown commands', async () => {
    const router = createTestRouter();
    const pinia = createPinia();

    router.push('/admin/dashboard');
    await router.isReady();

    const wrapper = mount(AdminLayout, {
      global: {
        plugins: [pinia, router, ElementPlus]
      }
    });
    const authStore = useAuthStore();
    authStore.$patch({
      token: 'admin-token',
      userName: 'root',
      nickName: 'Root',
      avatarUrl: '/uploads/avatars/root.png'
    });
    await nextTick();

    expect(wrapper.find('.topbar-search').exists()).toBe(false);
    expect(wrapper.find('.icon-button').exists()).toBe(false);
    expect(wrapper.text()).toContain('Root');
    expect(wrapper.find('.profile__avatar-img').attributes('src')).toBe('/uploads/avatars/root.png');

    await wrapper.vm.handleProfileCommand('profile');
    expect(router.currentRoute.value.fullPath).toBe('/admin/profile');

    await wrapper.vm.handleProfileCommand('password');
    expect(router.currentRoute.value.fullPath).toBe('/admin/profile?tab=password');
  });
});
