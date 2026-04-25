import { flushPromises, mount } from '@vue/test-utils';
import ElementPlus from 'element-plus';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import {
  createArticle,
  getArticle,
  getArticlePage,
  getCategoryList,
  updateArticle
} from '../src/api/articles.js';
import ArticlesView from '../src/views/ArticlesView.vue';

vi.mock('../src/api/articles.js', () => ({
  createArticle: vi.fn(),
  deleteArticle: vi.fn(),
  getArticle: vi.fn(),
  getArticlePage: vi.fn(),
  getCategoryList: vi.fn(),
  updateArticle: vi.fn(),
  updateArticleStatus: vi.fn()
}));

describe('ArticlesView', () => {
  beforeEach(() => {
    getCategoryList.mockResolvedValue({
      code: 200,
      message: '操作成功',
      data: [
        { id: 1, categoryName: '前端开发' },
        { id: 2, categoryName: '后端开发' }
      ]
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
    getArticle.mockResolvedValue({
      code: 200,
      message: '操作成功',
      data: {
        id: 101,
        articleTitle: '深入理解 React 18 并发渲染机制',
        articleSlug: 'react-18-concurrency',
        categoryId: 1,
        articleStatus: 'PUBLISHED',
        articleSummary: 'React 18 摘要',
        coverUrl: '/cover.png',
        articleContent: 'React 18 正文',
        topFlag: true,
        allowComment: false,
        viewCount: 1250,
        publishedTime: '2024-04-10T14:30:00'
      }
    });
    createArticle.mockResolvedValue({ code: 200, message: '操作成功', data: { id: 200 } });
    updateArticle.mockResolvedValue({ code: 200, message: '操作成功', data: { id: 101 } });
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
    expect(wrapper.text()).toContain('序号');
    expect(wrapper.text()).not.toContain('ID');
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
      '后端开发'
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

  it('creates, views, and edits articles in admin dialogs', async () => {
    const wrapper = mount(ArticlesView, {
      global: {
        plugins: [ElementPlus]
      }
    });

    await flushPromises();

    wrapper.vm.handleCreate();
    await flushPromises();

    expect(wrapper.vm.articleDialogVisible).toBe(true);
    expect(wrapper.vm.articleDialogTitle).toBe('新增文章');

    wrapper.vm.articleForm.articleTitle = '后台新增文章';
    wrapper.vm.articleForm.articleSlug = 'admin-created-article';
    wrapper.vm.articleForm.categoryId = 1;
    wrapper.vm.articleForm.articleStatus = 'DRAFT';
    wrapper.vm.articleForm.articleSummary = '摘要';
    wrapper.vm.articleForm.coverUrl = '/cover.png';
    wrapper.vm.articleForm.articleContent = '正文内容';
    wrapper.vm.articleForm.topFlag = true;
    wrapper.vm.articleForm.allowComment = false;

    await wrapper.vm.submitArticle();

    expect(createArticle).toHaveBeenCalledWith({
      articleTitle: '后台新增文章',
      articleSlug: 'admin-created-article',
      categoryId: 1,
      articleStatus: 'DRAFT',
      articleSummary: '摘要',
      coverUrl: '/cover.png',
      articleContent: '正文内容',
      topFlag: true,
      allowComment: false
    });
    expect(wrapper.vm.articleDialogVisible).toBe(false);

    await wrapper.vm.handleView({ id: 101 });
    await flushPromises();

    expect(getArticle).toHaveBeenCalledWith(101);
    expect(wrapper.vm.detailDialogVisible).toBe(true);
    expect(wrapper.text()).toContain('React 18 正文');
    expect(wrapper.text()).toContain('1,250');

    await wrapper.vm.handleEdit({ id: 101 });
    await flushPromises();

    expect(wrapper.vm.articleDialogTitle).toBe('编辑文章');
    expect(wrapper.vm.articleForm.articleTitle).toBe('深入理解 React 18 并发渲染机制');

    wrapper.vm.articleForm.articleTitle = 'React 18 编辑版';
    wrapper.vm.articleForm.articleStatus = 'PRIVATE';
    await wrapper.vm.submitArticle();

    expect(updateArticle).toHaveBeenCalledWith(101, expect.objectContaining({
      articleTitle: 'React 18 编辑版',
      articleStatus: 'PRIVATE',
      articleContent: 'React 18 正文'
    }));
  });
});
