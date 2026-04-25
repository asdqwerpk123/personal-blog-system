import { beforeEach, describe, expect, it } from 'vitest';

import {
  createArticle,
  deleteArticle,
  getArticle,
  getArticlePage,
  getCategoryList,
  updateArticle,
  updateArticleStatus
} from '../src/api/articles.js';
import { persistAuth } from '../src/utils/authStorage.js';

describe('article admin API', () => {
  beforeEach(() => {
    localStorage.clear();
    sessionStorage.clear();
    persistAuth({
      token: 'admin-token',
      userName: 'admin',
      remember: true
    });
  });

  it('requests the backend article page contract with filters and pagination', async () => {
    const capturedConfigs = [];
    const adapter = (config) => {
      capturedConfigs.push(config);

      return Promise.resolve({
        config,
        data: { code: 200, message: '操作成功', data: { records: [], total: 0 } },
        headers: {},
        request: {},
        status: 200,
        statusText: 'OK'
      });
    };

    await getArticlePage({
      page: 2,
      pageSize: 20,
      title: 'Vue',
      categoryId: 3,
      status: 'PUBLISHED'
    }, { adapter });

    expect(capturedConfigs[0].url).toBe('/admin/article/page');
    expect(capturedConfigs[0].method).toBe('get');
    expect(capturedConfigs[0].params).toEqual({
      page: 2,
      pageSize: 20,
      title: 'Vue',
      categoryId: 3,
      status: 'PUBLISHED'
    });
    expect(capturedConfigs[0].headers.Authorization).toBe('Bearer admin-token');
  });

  it('uses real category list, detail, save, delete, and status endpoints', async () => {
    const capturedConfigs = [];
    const adapter = (config) => {
      capturedConfigs.push(config);

      return Promise.resolve({
        config,
        data: { code: 200, message: '操作成功', data: [] },
        headers: {},
        request: {},
        status: 200,
        statusText: 'OK'
      });
    };

    await getCategoryList({ adapter });
    await getArticle(101, { adapter });
    await createArticle({
      articleTitle: '新文章',
      articleSlug: 'new-article',
      articleStatus: 'DRAFT',
      articleContent: '正文'
    }, { adapter });
    await updateArticle(101, {
      articleTitle: '编辑文章',
      articleSlug: 'edit-article',
      articleStatus: 'PRIVATE',
      articleContent: '新正文'
    }, { adapter });
    await deleteArticle(101, { adapter });
    await updateArticleStatus(101, 'PRIVATE', { adapter });

    expect(capturedConfigs.map((config) => `${config.method} ${config.url}`)).toEqual([
      'get /admin/category/list',
      'get /admin/article/101',
      'post /admin/article',
      'put /admin/article/101',
      'delete /admin/article/101',
      'put /admin/article/101/status'
    ]);
    expect(JSON.parse(capturedConfigs[2].data)).toEqual({
      articleTitle: '新文章',
      articleSlug: 'new-article',
      articleStatus: 'DRAFT',
      articleContent: '正文'
    });
    expect(JSON.parse(capturedConfigs[3].data)).toEqual({
      articleTitle: '编辑文章',
      articleSlug: 'edit-article',
      articleStatus: 'PRIVATE',
      articleContent: '新正文'
    });
    expect(capturedConfigs[5].params).toEqual({ status: 'PRIVATE' });
  });
});
