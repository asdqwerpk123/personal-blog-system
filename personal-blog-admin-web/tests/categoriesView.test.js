import { flushPromises, mount } from '@vue/test-utils';
import ElementPlus from 'element-plus';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import {
  createCategory,
  deleteCategory,
  getCategoryPage,
  updateCategory
} from '../src/api/categories.js';
import CategoriesView from '../src/views/CategoriesView.vue';

vi.mock('../src/api/categories.js', () => ({
  createCategory: vi.fn(),
  deleteCategory: vi.fn(),
  getCategoryList: vi.fn(),
  getCategoryPage: vi.fn(),
  updateCategory: vi.fn()
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

describe('CategoriesView', () => {
  beforeEach(() => {
    getCategoryPage.mockResolvedValue({
      code: 200,
      message: '操作成功',
      data: {
        records: [
          {
            id: 1,
            categoryName: '前端开发',
            description: '前端技术分享，包括 React, Vue, TypeScript 等',
            sortNo: 1,
            createTime: '2024-01-15T10:23:00'
          },
          {
            id: 2,
            categoryName: '后端开发',
            description: 'Java, Spring Boot, Node.js',
            sortNo: 2,
            createTime: '2024-01-16T14:10:22'
          }
        ],
        total: 5,
        current: 1,
        size: 10,
        pages: 1
      }
    });
    createCategory.mockResolvedValue({ code: 200, message: '操作成功', data: {} });
    updateCategory.mockResolvedValue({ code: 200, message: '操作成功', data: {} });
    deleteCategory.mockResolvedValue({ code: 200, message: '操作成功', data: {} });
  });

  afterEach(() => {
    vi.clearAllMocks();
  });

  it('renders category rows and sends search/reset parameters', async () => {
    const wrapper = mount(CategoriesView, {
      global: {
        plugins: [ElementPlus]
      }
    });

    await flushPromises();

    expect(wrapper.text()).toContain('分类管理');
    expect(wrapper.text()).toContain('新增分类');
    expect(wrapper.text()).toContain('序号');
    expect(wrapper.text()).not.toContain('分类ID');
    expect(wrapper.text()).toContain('前端开发');
    expect(wrapper.text()).toContain('后端开发');
    expect(wrapper.text()).toContain('共 5 条记录');
    expect(getCategoryPage).toHaveBeenLastCalledWith({
      categoryName: '',
      page: 1,
      pageSize: 10
    });

    wrapper.vm.filters.categoryName = '前端';
    await wrapper.vm.handleSearch();

    expect(getCategoryPage).toHaveBeenLastCalledWith({
      categoryName: '前端',
      page: 1,
      pageSize: 10
    });

    await wrapper.vm.handleReset();

    expect(wrapper.vm.filters.categoryName).toBe('');
    expect(getCategoryPage).toHaveBeenLastCalledWith({
      categoryName: '',
      page: 1,
      pageSize: 10
    });
  });

  it('creates, edits, and deletes categories through real API wrappers', async () => {
    const wrapper = mount(CategoriesView, {
      global: {
        plugins: [ElementPlus]
      }
    });

    await flushPromises();

    wrapper.vm.openCreateDialog();
    await flushPromises();

    expect(wrapper.vm.dialogTitle).toBe('新增分类');
    expect(wrapper.vm.form.sortNo).toBe(3);
    expect(wrapper.text()).toContain('数字越小越靠前');

    wrapper.vm.form.categoryName = '数据库';
    wrapper.vm.form.description = 'MySQL';
    await wrapper.vm.submitCategory();

    expect(createCategory).toHaveBeenCalledWith({
      categoryName: '数据库',
      description: 'MySQL',
      sortNo: 3
    });

    wrapper.vm.openEditDialog({
      id: 2,
      categoryName: '后端开发',
      description: 'Java',
      sortNo: 2
    });
    expect(wrapper.vm.dialogTitle).toBe('编辑分类');
    expect(wrapper.vm.form.categoryName).toBe('后端开发');

    wrapper.vm.form.description = 'Spring Boot';
    await wrapper.vm.submitCategory();

    expect(updateCategory).toHaveBeenCalledWith(2, {
      categoryName: '后端开发',
      description: 'Spring Boot',
      sortNo: 2
    });

    await wrapper.vm.handleDelete({ id: 2, categoryName: '后端开发' });

    expect(deleteCategory).toHaveBeenCalledWith(2);
    expect(getCategoryPage).toHaveBeenCalledTimes(4);
  });
});
