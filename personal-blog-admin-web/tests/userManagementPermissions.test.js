import { flushPromises, mount } from '@vue/test-utils';
import ElementPlus from 'element-plus';
import { createPinia } from 'pinia';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { getRoleList } from '../src/api/roles.js';
import { getUserPage } from '../src/api/users.js';
import { useAuthStore } from '../src/stores/auth.js';
import UserManagementView from '../src/views/admin/UserManagementView.vue';

vi.mock('../src/api/users.js', () => ({
  createUser: vi.fn(),
  getUserPage: vi.fn(),
  resetUserPassword: vi.fn(),
  updateUser: vi.fn(),
  updateUserStatus: vi.fn()
}));

vi.mock('../src/api/roles.js', () => ({
  getRoleList: vi.fn()
}));

describe('UserManagementView permissions', () => {
  beforeEach(() => {
    getRoleList.mockResolvedValue({
      data: [
        { id: 1, roleCode: 'SUPER_ADMIN', roleName: '超级管理员' },
        { id: 2, roleCode: 'ADMIN', roleName: '管理员' },
        { id: 3, roleCode: 'USER', roleName: '普通用户' }
      ]
    });
    getUserPage.mockResolvedValue({
      data: {
        records: [
          { id: 2, userName: 'admin_zhang', roleId: 2, roleCode: 'ADMIN', roleName: '管理员', userStatus: 'ENABLED' },
          { id: 4, userName: 'tom', roleId: 3, roleCode: 'USER', roleName: '普通用户', userStatus: 'ENABLED' }
        ],
        total: 2
      }
    });
  });

  afterEach(() => {
    vi.clearAllMocks();
  });

  it('limits ADMIN role choices and user actions to normal users', async () => {
    const pinia = createPinia();
    const wrapper = mount(UserManagementView, {
      global: {
        plugins: [pinia, ElementPlus]
      }
    });
    useAuthStore().$patch({ userId: '2', roleCode: 'ADMIN' });

    await flushPromises();

    expect(wrapper.vm.assignableRoles.map((role) => role.roleCode)).toEqual(['USER']);
    expect(wrapper.vm.canManageUser({ id: 2, roleCode: 'ADMIN' })).toBe(false);
    expect(wrapper.vm.canManageUser({ id: 4, roleCode: 'USER' })).toBe(true);
    expect(wrapper.vm.canResetPassword({ id: 2, roleCode: 'ADMIN' })).toBe(false);
    expect(wrapper.vm.canToggleStatus({ id: 2, roleCode: 'ADMIN' })).toBe(false);
  });
});
