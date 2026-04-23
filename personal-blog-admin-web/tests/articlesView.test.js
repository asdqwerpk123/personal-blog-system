import { flushPromises, mount } from '@vue/test-utils';
import ElementPlus from 'element-plus';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import {
  getArticlePage,
  getCategoryList
} from '../src/api/articles.js';
import ArticlesView from '../src/views/ArticlesView.vue';

vi.mock('../src/api/articles.js', () => ({
  deleteArticle: vi.fn(),
  getArticlePage: vi.fn(),
  getCategoryList: vi.fn(),
  updateArticleStatus: vi.fn()
}));

describe('ArticlesView', () => {
  beforeEach(() => {
    getCategoryList.mockResolvedValue({
      code: 200,
      message: '操作成功',
      data: []
    });
    getArticlePage.mockResolvedValue({
      code: 200,
      message: '操作成功',
      data: {
        records: [
          {
            id: 101,
            articleTitle: '深入理解 React 18 并发渲染机制',
            categoryName: '前端开发',
            articleStatus: 'PUBLISHED',
            viewCount: 1250,
            publishedTime: '2024-04-10T14:30:00'
          }
        ],
        total: 42,
        current: 1,
        size: 10,
        pages: 5
      }
    });
  });

  afterEach(() => {
    vi.clearAllMocks();
  });

  it('renders the real article management page and fallback categories', async () => {
    const wrapper = mount(ArticlesView, {
      global: {
        plugins: [ElementPlus]
      }
    });

    await flushPromises();

    expect(wrapper.text()).toContain('文章管理');
    expect(wrapper.text()).toContain('新增文章');
    expect(wrapper.text()).toContain('标题');
    expect(wrapper.text()).toContain('分类');
    expect(wrapper.text()).toContain('状态');
    expect(wrapper.text()).toContain('深入理解 React 18');
    expect(wrapper.text()).toContain('前端开发');
    expect(wrapper.text()).toContain('已发布');
    expect(wrapper.text()).toContain('1,250');
    expect(wrapper.text()).toContain('2024-04-10 14:30:00');
    expect(wrapper.text()).toContain('共 42 条记录');
    expect(wrapper.vm.categoryOptions.map((category) => category.label)).toEqual([
      '全部',
      '前端开发',
      '后端开发',
      '架构设计',
      '数据库',
      '运维部署'
    ]);

    expect(getCategoryList).toHaveBeenCalledTimes(1);
    expect(getArticlePage).toHaveBeenCalledWith({
      categoryId: '',
      categoryName: '',
      page: 1,
      pageSize: 10,
      status: '',
      title: ''
    });
  });
});
