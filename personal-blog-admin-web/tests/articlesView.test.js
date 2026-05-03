import { flushPromises, mount } from '@vue/test-utils';
import ElementPlus from 'element-plus';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { createMemoryHistory, createRouter } from 'vue-router';

import {
  createArticle,
  getArticle,
  getArticlePage,
  getCategoryList,
  updateArticle
} from '../src/api/articles.js';
import { uploadArticleCover } from '../src/api/files.js';
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

vi.mock('../src/api/files.js', () => ({
  uploadArticleCover: vi.fn()
}));

async function mountWithArticleRoute(query = {}) {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [{ path: '/admin/articles', component: ArticlesView }]
  });
  router.push({ path: '/admin/articles', query });
  await router.isReady();

  const wrapper = mount(ArticlesView, {
    global: {
      plugins: [router, ElementPlus]
    }
  });

  await flushPromises();

  return { router, wrapper };
}

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
    uploadArticleCover.mockResolvedValue({
      code: 200,
      message: '上传成功',
      data: { url: '/uploads/article-covers/article-cover.png' }
    });
  });

  afterEach(() => {
    vi.clearAllMocks();
  });

  it('renders the real article management page and fallback categories', async () => {
    const { wrapper } = await mountWithArticleRoute();

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
    const { wrapper } = await mountWithArticleRoute();

    wrapper.vm.handleCreate();
    await flushPromises();

    expect(wrapper.vm.articleDialogVisible).toBe(true);
    expect(wrapper.vm.articleDialogTitle).toBe('新增文章');
    expect(wrapper.find('.create-article-button.primary-action-button').exists()).toBe(true);

    await wrapper.vm.uploadCover({ file: new File(['cover'], 'cover.png', { type: 'image/png' }) });

    expect(uploadArticleCover).toHaveBeenCalledWith(expect.any(File));
    expect(wrapper.vm.articleForm.coverUrl).toBe('/uploads/article-covers/article-cover.png');

    wrapper.vm.articleForm.articleTitle = '后台新增文章';
    wrapper.vm.articleForm.articleSlug = 'admin-created-article';
    wrapper.vm.articleForm.categoryId = 1;
    wrapper.vm.articleForm.articleStatus = 'DRAFT';
    wrapper.vm.articleForm.articleSummary = '摘要';
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
      coverUrl: '/uploads/article-covers/article-cover.png',
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

  it('opens article view and edit actions from route query', async () => {
    const { wrapper: viewWrapper } = await mountWithArticleRoute({ action: 'view', id: '101' });

    expect(getArticle).toHaveBeenCalledWith('101');
    expect(viewWrapper.vm.detailDialogVisible).toBe(true);

    vi.clearAllMocks();
    getArticle.mockResolvedValueOnce({
      code: 200,
      message: '操作成功',
      data: {
        id: 101,
        articleTitle: 'Route Edit Article',
        articleSlug: 'route-edit-article',
        articleStatus: 'DRAFT',
        articleContent: 'Route edit content'
      }
    });

    const { wrapper: editWrapper } = await mountWithArticleRoute({ action: 'edit', id: '101' });

    expect(getArticle).toHaveBeenCalledWith('101');
    expect(editWrapper.vm.articleDialogVisible).toBe(true);
    expect(editWrapper.vm.articleDialogTitle).toBe('编辑文章');
    expect(editWrapper.vm.articleForm.articleTitle).toBe('Route Edit Article');
  });
});
