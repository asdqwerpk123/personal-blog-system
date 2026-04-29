import { flushPromises, mount } from '@vue/test-utils';
import ElementPlus from 'element-plus';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import {
  createFriendLink,
  deleteFriendLink,
  getFriendLinkPage,
  updateFriendLink,
  updateFriendLinkStatus,
  uploadFriendLinkLogo
} from '../src/api/friendLinks.js';
import FriendLinkManagementView from '../src/views/admin/FriendLinkManagementView.vue';

vi.mock('../src/api/friendLinks.js', () => ({
  createFriendLink: vi.fn(),
  deleteFriendLink: vi.fn(),
  getFriendLinkPage: vi.fn(),
  updateFriendLink: vi.fn(),
  updateFriendLinkStatus: vi.fn(),
  uploadFriendLinkLogo: vi.fn()
}));

vi.mock('element-plus', async () => {
  const actual = await vi.importActual('element-plus');

  return {
    ...actual,
    ElMessage: {
      error: vi.fn(),
      success: vi.fn(),
      warning: vi.fn()
    },
    ElMessageBox: {
      confirm: vi.fn(() => Promise.resolve())
    }
  };
});

describe('FriendLinkManagementView', () => {
  beforeEach(() => {
    getFriendLinkPage.mockResolvedValue({
      code: 200,
      message: '操作成功',
      data: {
        records: [
          {
            id: 8,
            siteName: 'Open Source Study Notes',
            siteUrl: 'https://example.com/study-notes',
            siteLogo: '/uploads/friend-links/logo.png',
            ownerName: 'Sample Owner',
            contactEmail: 'contact@example.com',
            siteDesc: '示例友链',
            linkStatus: 'PENDING',
            updateTime: '2026-04-22T09:30:00'
          }
        ],
        total: 1,
        current: 1,
        size: 10
      }
    });
    createFriendLink.mockResolvedValue({ code: 200, message: '操作成功', data: { id: 9 } });
    updateFriendLink.mockResolvedValue({ code: 200, message: '操作成功', data: { id: 8 } });
    updateFriendLinkStatus.mockResolvedValue({ code: 200, message: '操作成功', data: { id: 8 } });
    deleteFriendLink.mockResolvedValue({ code: 200, message: '操作成功', data: null });
    uploadFriendLinkLogo.mockResolvedValue({
      code: 200,
      message: '操作成功',
      url: 'http://minio.example.com/bucket/new-logo.png'
    });
  });

  afterEach(() => {
    vi.clearAllMocks();
  });

  it('renders friend links with logo preview and status actions', async () => {
    const wrapper = mount(FriendLinkManagementView, {
      global: {
        plugins: [ElementPlus]
      }
    });

    await flushPromises();

    expect(wrapper.text()).toContain('友链管理');
    expect(wrapper.text()).toContain('新增友链');
    expect(wrapper.find('.page-heading .primary-action-button').exists()).toBe(true);
    expect(wrapper.text()).toContain('Open Source Study Notes');
    expect(wrapper.text()).toContain('待审核');
    expect(wrapper.find('img.friend-link-logo-thumb').attributes('src')).toBe('/uploads/friend-links/logo.png');

    await wrapper.vm.changeFriendLinkStatus({ id: 8, siteName: 'Open Source Study Notes' }, 'APPROVED');

    expect(updateFriendLinkStatus).toHaveBeenCalledWith(8, 'APPROVED');
    expect(getFriendLinkPage).toHaveBeenCalledTimes(2);
  });

  it('creates, uploads logo, edits, and deletes friend links without closing on upload failure', async () => {
    const wrapper = mount(FriendLinkManagementView, {
      global: {
        plugins: [ElementPlus]
      }
    });

    await flushPromises();

    wrapper.vm.openCreateDialog();
    wrapper.vm.form.siteName = 'New Site';
    wrapper.vm.form.siteUrl = 'https://new.example.com';
    wrapper.vm.form.ownerName = 'New Owner';
    wrapper.vm.form.contactEmail = 'new@example.com';
    wrapper.vm.form.siteDesc = '新友链';

    await wrapper.vm.uploadLogo({ file: new File(['logo'], 'logo.png', { type: 'image/png' }) });

    expect(wrapper.vm.form.siteLogo).toBe('http://minio.example.com/bucket/new-logo.png');

    await wrapper.vm.submitFriendLink();

    expect(createFriendLink).toHaveBeenCalledWith({
      siteName: 'New Site',
      siteUrl: 'https://new.example.com',
      siteLogo: 'http://minio.example.com/bucket/new-logo.png',
      ownerName: 'New Owner',
      contactEmail: 'new@example.com',
      siteDesc: '新友链',
      linkStatus: 'PENDING'
    });

    wrapper.vm.openEditDialog({
      id: 8,
      siteName: 'Open Source Study Notes',
      siteUrl: 'https://example.com/study-notes',
      siteLogo: '/uploads/friend-links/logo.png',
      ownerName: 'Sample Owner',
      contactEmail: 'contact@example.com',
      siteDesc: '示例友链',
      linkStatus: 'PENDING'
    });
    wrapper.vm.form.siteDesc = '已更新';
    await wrapper.vm.submitFriendLink();

    expect(updateFriendLink).toHaveBeenCalledWith(8, expect.objectContaining({
      siteName: 'Open Source Study Notes',
      siteDesc: '已更新',
      siteLogo: '/uploads/friend-links/logo.png'
    }));

    uploadFriendLinkLogo.mockRejectedValueOnce(new Error('不支持的图片格式'));
    wrapper.vm.openCreateDialog();
    await wrapper.vm.uploadLogo({ file: new File(['bad'], 'logo.txt', { type: 'text/plain' }) });

    expect(wrapper.vm.dialogVisible).toBe(true);

    await wrapper.vm.handleDelete({ id: 8, siteName: 'Open Source Study Notes' });

    expect(deleteFriendLink).toHaveBeenCalledWith(8);
  });
});
