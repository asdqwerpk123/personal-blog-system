import { mount } from '@vue/test-utils';
import ElementPlus from 'element-plus';
import { createPinia } from 'pinia';
import { describe, expect, it, vi } from 'vitest';

import UserManagementView from '../src/views/admin/UserManagementView.vue';
import ArticleManagementView from '../src/views/admin/ArticleManagementView.vue';
import CommentModerationView from '../src/views/admin/CommentModerationView.vue';
import OperationLogView from '../src/views/admin/OperationLogView.vue';

vi.mock('../src/api/users.js', () => ({
  createUser: vi.fn(),
  getUserPage: vi.fn(() => Promise.resolve({ data: { records: [], total: 0 } })),
  resetUserPassword: vi.fn(),
  updateUser: vi.fn(),
  updateUserStatus: vi.fn()
}));

vi.mock('../src/api/roles.js', () => ({
  getRoleList: vi.fn(() => Promise.resolve({ data: [{ id: 2, roleName: '管理员', roleCode: 'ADMIN' }] }))
}));

vi.mock('../src/api/articles.js', () => ({
  createArticle: vi.fn(),
  deleteArticle: vi.fn(),
  getArticlePage: vi.fn(() => Promise.resolve({
    data: {
      records: [{ id: 1, articleTitle: 'API Article', categoryId: 2, articleStatus: 'DRAFT' }],
      total: 1
    }
  })),
  getArticleTags: vi.fn(() => Promise.resolve({ data: [] })),
  updateArticle: vi.fn(),
  updateArticleStatus: vi.fn(),
  updateArticleTags: vi.fn()
}));

vi.mock('../src/api/categories.js', () => ({
  getCategoryList: vi.fn(() => Promise.resolve({ data: [] }))
}));

vi.mock('../src/api/comments.js', () => ({
  deleteComment: vi.fn(),
  getCommentPage: vi.fn(() => Promise.resolve({
    data: {
      records: [{ id: 1, commentContent: 'Needs check', articleId: 7, commentStatus: 'PENDING' }],
      total: 1
    }
  })),
  updateCommentStatus: vi.fn()
}));

vi.mock('../src/api/operationLogs.js', () => ({
  getOperationLogPage: vi.fn(() => Promise.resolve({
    data: {
      records: [{ id: 1, operatorUserId: 5, targetType: 'ARTICLE', actionType: 'CREATE', actionResult: 'SUCCESS' }],
      total: 1
    }
  }))
}));

describe('P1 admin pages', () => {
  it('renders user management with search, create, role assignment, status, and password actions', async () => {
    const wrapper = mount(UserManagementView, {
      global: {
        plugins: [createPinia(), ElementPlus]
      }
    });

    await vi.dynamicImportSettled();

    const text = wrapper.text();
    expect(text).toContain('用户管理');
    expect(text).toContain('新增用户');
    expect(wrapper.find('.page-heading .primary-action-button').exists()).toBe(true);
    expect(text).toContain('邮箱');
    expect(text).toContain('手机号');
    expect(text).toContain('角色');
    expect(text).toContain('启禁状态');
    expect(text).toContain('重置密码');
  });

  it('renders article management with table operations and tag editing affordance', async () => {
    const wrapper = mount(ArticleManagementView, {
      global: {
        plugins: [ElementPlus]
      }
    });

    await vi.dynamicImportSettled();

    const text = wrapper.text();
    expect(text).toContain('文章管理');
    expect(text).toContain('新增文章');
    expect(wrapper.find('.page-heading .primary-action-button').exists()).toBe(true);
    expect(text).toContain('标签');
    expect(text).toContain('发布');
    expect(text).toContain('删除');
    expect(text).toContain('API Article');
    expect(text).toContain('2');
  });

  it('renders comment moderation with status filters and audit actions', async () => {
    const wrapper = mount(CommentModerationView, {
      global: {
        plugins: [ElementPlus]
      }
    });

    await vi.dynamicImportSettled();

    const text = wrapper.text();
    expect(text).toContain('评论审核');
    expect(text).toContain('待审核');
    expect(text).toContain('通过');
    expect(text).toContain('驳回');
    expect(text).toContain('Needs check');
    expect(text).toContain('7');
  });

  it('renders operation logs with operator id fallback from the backend payload', async () => {
    const wrapper = mount(OperationLogView, {
      global: {
        plugins: [ElementPlus]
      }
    });

    await vi.dynamicImportSettled();

    const text = wrapper.text();
    expect(text).toContain('操作日志');
    expect(text).toContain('ARTICLE');
    expect(text).toContain('CREATE');
    expect(text).toContain('5');
  });
});
