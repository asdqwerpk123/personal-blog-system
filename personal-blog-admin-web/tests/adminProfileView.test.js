import { flushPromises, mount } from '@vue/test-utils';
import ElementPlus from 'element-plus';
import { createPinia } from 'pinia';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { createMemoryHistory, createRouter } from 'vue-router';

import { getMyProfile, updateMyPassword, updateMyProfile, uploadAvatar } from '../src/api/profile.js';
import AdminProfileView from '../src/views/admin/AdminProfileView.vue';

vi.mock('../src/api/profile.js', () => ({
  getMyProfile: vi.fn(),
  updateMyPassword: vi.fn(),
  updateMyProfile: vi.fn(),
  uploadAvatar: vi.fn()
}));

vi.mock('element-plus', async () => {
  const actual = await vi.importActual('element-plus');

  return {
    ...actual,
    ElMessage: {
      error: vi.fn(),
      success: vi.fn(),
      warning: vi.fn()
    }
  };
});

function createTestRouter(path = '/admin/profile') {
  return createRouter({
    history: createMemoryHistory(),
    routes: [{ path: '/admin/profile', component: AdminProfileView }]
  });
}

describe('AdminProfileView', () => {
  beforeEach(() => {
    getMyProfile.mockResolvedValue({
      code: 200,
      data: {
        id: 1,
        userName: 'root',
        nickName: 'Root',
        email: 'root@blog.local',
        phone: '13800000000',
        avatarUrl: '/uploads/avatars/root.png',
        introduction: '管理员'
      }
    });
    updateMyProfile.mockResolvedValue({ code: 200, data: {} });
    updateMyPassword.mockResolvedValue({ code: 200, data: {} });
    uploadAvatar.mockResolvedValue({
      code: 200,
      data: { url: '/uploads/avatars/new-avatar.png' }
    });
  });

  afterEach(() => {
    vi.clearAllMocks();
  });

  it('loads, edits, uploads avatar, and changes password for the current admin', async () => {
    const router = createTestRouter();
    router.push('/admin/profile?tab=password');
    await router.isReady();

    const wrapper = mount(AdminProfileView, {
      global: {
        plugins: [createPinia(), router, ElementPlus]
      }
    });

    await flushPromises();

    expect(getMyProfile).toHaveBeenCalledTimes(1);
    expect(wrapper.text()).toContain('个人资料');
    expect(wrapper.vm.profileForm.userName).toBe('root');
    expect(wrapper.vm.activeTab).toBe('password');

    await wrapper.vm.uploadAvatarRequest({ file: new File(['avatar'], 'avatar.png', { type: 'image/png' }) });
    expect(uploadAvatar).toHaveBeenCalled();
    expect(wrapper.vm.profileForm.avatarUrl).toBe('/uploads/avatars/new-avatar.png');

    wrapper.vm.profileForm.nickName = 'Root Updated';
    wrapper.vm.profileForm.email = 'root-updated@blog.local';
    wrapper.vm.profileForm.phone = '13800000009';
    wrapper.vm.profileForm.introduction = '更新简介';
    await wrapper.vm.saveProfile();

    expect(updateMyProfile).toHaveBeenCalledWith({
      nickName: 'Root Updated',
      email: 'root-updated@blog.local',
      phone: '13800000009',
      avatarUrl: '/uploads/avatars/new-avatar.png',
      introduction: '更新简介'
    });

    wrapper.vm.passwordForm.oldPassword = '123456';
    wrapper.vm.passwordForm.newPassword = '654321';
    wrapper.vm.passwordForm.confirmPassword = '654321';
    await wrapper.vm.changePassword();

    expect(updateMyPassword).toHaveBeenCalledWith({
      oldPassword: '123456',
      newPassword: '654321'
    });
  });
});
