import { describe, expect, it } from 'vitest';

import { adminMenuRoutes } from '../src/router/adminMenu.js';
import router from '../src/router/index.js';
import PlaceholderView from '../src/views/PlaceholderView.vue';

describe('routes', () => {
  it('contains the full first-stage admin menu with readable Chinese labels', () => {
    expect(adminMenuRoutes.map((route) => route.title)).toEqual([
      '仪表盘',
      '文章管理',
      '分类管理',
      '标签管理',
      '评论审核',
      '友链管理',
      '操作日志',
      '用户管理'
    ]);
  });

  it('maps P1 admin menu routes to real views instead of the placeholder', () => {
    const p1Paths = [
      '/admin/articles',
      '/admin/categories',
      '/admin/tags',
      '/admin/comments',
      '/admin/friend-links',
      '/admin/logs',
      '/admin/users',
      '/admin/profile'
    ];

    for (const path of p1Paths) {
      const leaf = router.resolve(path).matched.at(-1);

      expect(leaf?.components?.default).toBeTruthy();
      expect(leaf.components.default).not.toBe(PlaceholderView);
    }
  });

  it('does not expose the removed role dictionary route', () => {
    const adminRoute = router.getRoutes().find((route) => route.path === '/admin');
    const rolesChild = adminRoute.children.find((route) => route.path === 'roles');

    expect(rolesChild).toBeUndefined();
    expect(router.resolve('/admin/roles').matched.at(-1)?.redirect).toBe('/admin/dashboard');
  });

  it('registers author center routes as real views', () => {
    const authorPaths = [
      '/register',
      '/author/dashboard',
      '/author/articles',
      '/author/articles/new',
      '/author/articles/edit/3',
      '/author/articles/detail/3',
      '/author/comments',
      '/author/profile'
    ];

    for (const path of authorPaths) {
      const leaf = router.resolve(path).matched.at(-1);
      const component = leaf?.components?.default || leaf?.component;

      expect(component).toBeTruthy();
      expect(component).not.toBe(PlaceholderView);
    }
  });
});
