import { beforeEach, describe, expect, it } from 'vitest';

import {
  createCategory,
  deleteCategory,
  getCategoryList,
  getCategoryPage,
  updateCategory
} from '../src/api/categories.js';
import { persistAuth } from '../src/utils/authStorage.js';

describe('category admin API', () => {
  beforeEach(() => {
    localStorage.clear();
    sessionStorage.clear();
    persistAuth({
      token: 'admin-token',
      userName: 'admin',
      remember: true
    });
  });

  it('requests the backend category page contract with filters and pagination', async () => {
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

    await getCategoryPage({
      page: 2,
      pageSize: 20,
      categoryName: '前端'
    }, { adapter });

    expect(capturedConfigs[0].url).toBe('/admin/category/page');
    expect(capturedConfigs[0].method).toBe('get');
    expect(capturedConfigs[0].params).toEqual({
      page: 2,
      pageSize: 20,
      categoryName: '前端'
    });
    expect(capturedConfigs[0].headers.Authorization).toBe('Bearer admin-token');
  });

  it('uses real category list, create, update, and delete endpoints', async () => {
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
    await createCategory({ categoryName: '前端开发', sortNo: 1, description: 'Vue' }, { adapter });
    await updateCategory(6, { categoryName: '后端开发', sortNo: 2, description: 'Java' }, { adapter });
    await deleteCategory(6, { adapter });

    expect(capturedConfigs.map((config) => `${config.method} ${config.url}`)).toEqual([
      'get /admin/category/list',
      'post /admin/category',
      'put /admin/category/6',
      'delete /admin/category/6'
    ]);
    expect(JSON.parse(capturedConfigs[1].data)).toEqual({
      categoryName: '前端开发',
      sortNo: 1,
      description: 'Vue'
    });
    expect(JSON.parse(capturedConfigs[2].data)).toEqual({
      categoryName: '后端开发',
      sortNo: 2,
      description: 'Java'
    });
  });
});
