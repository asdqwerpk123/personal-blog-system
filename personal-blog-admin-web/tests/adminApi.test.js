import { beforeEach, describe, expect, it, vi } from 'vitest';

import http from '../src/api/http.js';
import {
  createArticle,
  deleteArticle,
  getArticleTags,
  getArticlePage,
  updateArticle,
  updateArticleStatus,
  updateArticleTags
} from '../src/api/articles.js';
import { createCategory, deleteCategory, getCategory, getCategoryList, getCategoryPage, updateCategory } from '../src/api/categories.js';
import { deleteComment, getArticleComments, getCommentPage, updateCommentStatus } from '../src/api/comments.js';
import { getDashboardSummary } from '../src/api/dashboard.js';
import { createFriendLink, deleteFriendLink, getFriendLinkPage, updateFriendLink } from '../src/api/friendLinks.js';
import { getOperationLogPage } from '../src/api/operationLogs.js';
import { getMyProfile, updateMyPassword, updateMyProfile, uploadAvatar } from '../src/api/profile.js';
import { getRole, getRoleList } from '../src/api/roles.js';
import { createTag, deleteTag, getTagPage, updateTag } from '../src/api/tags.js';
import {
  createUser,
  getUserPage,
  resetUserPassword,
  updateUser,
  updateUserStatus
} from '../src/api/users.js';

vi.mock('../src/api/http.js', () => ({
  default: {
    delete: vi.fn(),
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn()
  }
}));

describe('admin API modules', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('wraps user and role endpoints with stable params and bodies', () => {
    getUserPage({ current: 2, size: 20, keyword: 'alice' });
    createUser({ userName: 'alice' });
    updateUser(7, { nickName: 'Alice' });
    updateUserStatus(7, 'DISABLED');
    resetUserPassword(7, 'NewPass123');
    getRole(2);
    getRoleList();

    expect(http.get).toHaveBeenCalledWith('/admin/user/page', {
      params: { current: 2, size: 20, keyword: 'alice' }
    });
    expect(http.post).toHaveBeenCalledWith('/admin/user', { userName: 'alice' });
    expect(http.put).toHaveBeenCalledWith('/admin/user/7', { nickName: 'Alice' });
    expect(http.put).toHaveBeenCalledWith('/admin/user/7/status', { userStatus: 'DISABLED' });
    expect(http.put).toHaveBeenCalledWith('/admin/user/7/password/reset', { newPassword: 'NewPass123' });
    expect(http.get).toHaveBeenCalledWith('/admin/role/2');
    expect(http.get).toHaveBeenCalledWith('/admin/role/list');
  });

  it('wraps current profile endpoints', () => {
    const file = new File(['avatar'], 'avatar.png', { type: 'image/png' });

    getMyProfile();
    updateMyProfile({ nickName: 'Root' });
    updateMyPassword({ oldPassword: '123456', newPassword: '654321' });
    uploadAvatar(file);

    expect(http.get).toHaveBeenCalledWith('/admin/profile/me');
    expect(http.put).toHaveBeenCalledWith('/admin/profile/me', { nickName: 'Root' });
    expect(http.put).toHaveBeenCalledWith('/admin/profile/password', {
      oldPassword: '123456',
      newPassword: '654321'
    });
    expect(http.post).toHaveBeenCalledWith('/admin/files/avatar', expect.any(FormData));
  });

  it('wraps content, taxonomy, moderation, friend-link, and log endpoints', () => {
    getArticlePage({ current: 1, size: 10, keyword: 'Vue' });
    createArticle({ title: '新文章' });
    updateArticle(3, { title: '改名' });
    updateArticleStatus(3, 'PUBLISHED');
    getArticleTags(3);
    updateArticleTags(3, [1, 2]);
    deleteArticle(3);
    getCategoryList();
    getCategory(4);
    getCategoryPage({ current: 1, size: 10, keyword: '前端' });
    createCategory({ categoryName: '前端' });
    updateCategory(4, { categoryName: '后端' });
    deleteCategory(4);
    getTagPage({ current: 1, size: 10, keyword: 'Vue' });
    createTag({ tagName: 'Vue' });
    updateTag(5, { tagName: 'JavaScript' });
    deleteTag(5);
    getCommentPage({ current: 1, size: 10, keyword: 'spam', status: 'PENDING', articleId: 3 });
    getArticleComments(3);
    updateCommentStatus(6, 'APPROVED');
    deleteComment(6);
    getFriendLinkPage({ current: 1, size: 10, keyword: '站点', status: 'PENDING' });
    createFriendLink({ siteName: '友站', siteUrl: 'https://friend.example' });
    updateFriendLink(8, { siteName: '伙伴', siteUrl: 'https://partner.example' });
    deleteFriendLink(8);
    getOperationLogPage({ current: 1, size: 10, operatorUserId: 7, targetType: 'ARTICLE', actionResult: 'SUCCESS' });
    getDashboardSummary();

    expect(http.get).toHaveBeenCalledWith('/admin/article/page', {
      params: { current: 1, size: 10, keyword: 'Vue' }
    });
    expect(http.put).toHaveBeenCalledWith('/admin/article/3/status', null, { params: { status: 'PUBLISHED' } });
    expect(http.get).toHaveBeenCalledWith('/admin/article/3/tags');
    expect(http.put).toHaveBeenCalledWith('/admin/article/3/tags', { tagIds: [1, 2] });
    expect(http.get).toHaveBeenCalledWith('/admin/category/list');
    expect(http.get).toHaveBeenCalledWith('/admin/category/4');
    expect(http.get).toHaveBeenCalledWith('/admin/comment/article/3');
    expect(http.put).toHaveBeenCalledWith('/admin/comment/6/status', null, { params: { status: 'APPROVED' } });
    expect(http.get).toHaveBeenCalledWith('/admin/log/page', {
      params: { current: 1, size: 10, operatorUserId: 7, targetType: 'ARTICLE', actionResult: 'SUCCESS' }
    });
    expect(http.get).toHaveBeenCalledWith('/admin/dashboard/summary');
  });
});
