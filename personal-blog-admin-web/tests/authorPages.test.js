import { mount } from '@vue/test-utils';
import ElementPlus from 'element-plus';
import { createPinia } from 'pinia';
import { describe, expect, it, vi } from 'vitest';
import { createMemoryHistory, createRouter } from 'vue-router';
import { nextTick } from 'vue';

import { getAuthorDashboardSummary } from '../src/api/dashboard.js';
import AuthorLayout from '../src/layouts/AuthorLayout.vue';
import { useAuthStore } from '../src/stores/auth.js';
import AuthorDashboardView from '../src/views/author/AuthorDashboardView.vue';
import AuthorArticleEditorView from '../src/views/author/AuthorArticleEditorView.vue';

vi.mock('../src/api/dashboard.js', () => ({
  getAuthorDashboardSummary: vi.fn()
}));

vi.mock('../src/api/articles.js', () => ({
  getAuthorArticle: vi.fn(),
  getAuthorCategories: vi.fn().mockResolvedValue({ code: 200, data: [] }),
  getAuthorTags: vi.fn().mockResolvedValue({ code: 200, data: [] }),
  createAuthorArticle: vi.fn(),
  updateAuthorArticle: vi.fn()
}));

describe('author pages', () => {
  it('renders dashboard quick actions with real author routes', async () => {
    getAuthorDashboardSummary.mockResolvedValue({
      code: 200,
      data: {
        articleCount: 1,
        publishedCount: 0,
        draftCount: 1,
        privateCount: 0,
        recentArticles: []
      }
    });

    const router = createRouter({
      history: createMemoryHistory(),
      routes: [
        { path: '/author/dashboard', component: AuthorDashboardView },
        { path: '/author/articles', component: { template: '<div>articles</div>' } },
        { path: '/author/articles/new', component: { template: '<div>new</div>' } }
      ]
    });
    router.push('/author/dashboard');
    await router.isReady();

    const wrapper = mount(AuthorDashboardView, {
      global: {
        plugins: [createPinia(), router, ElementPlus]
      }
    });

    expect(wrapper.find('.quick-actions .primary-action-button').exists()).toBe(true);
    expect(wrapper.text()).toContain('新建文章');
    expect(wrapper.text()).toContain('我的文章');
  });

  it('does not render a cover URL field in author article editor', async () => {
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [{ path: '/author/articles/new', component: AuthorArticleEditorView }]
    });
    router.push('/author/articles/new');
    await router.isReady();

    const wrapper = mount(AuthorArticleEditorView, {
      global: {
        plugins: [router, ElementPlus]
      }
    });

    expect(wrapper.text()).toContain('新建文章');
    expect(wrapper.text()).not.toContain('封面 URL');
  });

  it('renders the author layout without a notification button', async () => {
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [
        {
          path: '/author',
          component: AuthorLayout,
          children: [{ path: 'dashboard', component: { template: '<div>dashboard</div>' } }]
        },
        { path: '/login', component: { template: '<div>login</div>' } }
      ]
    });
    const pinia = createPinia();

    router.push('/author/dashboard');
    await router.isReady();

    const wrapper = mount(AuthorLayout, {
      global: {
        plugins: [pinia, router, ElementPlus]
      }
    });
    const authStore = useAuthStore();
    authStore.$patch({
      token: 'author-token',
      userName: 'writer',
      nickName: 'Writer'
    });
    await nextTick();

    expect(wrapper.find('.icon-button').exists()).toBe(false);
    expect(wrapper.text()).toContain('Writer');
  });
});
