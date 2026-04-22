import { describe, expect, it } from 'vitest';

import { adminMenuRoutes } from '../src/router/adminMenu.js';
import router from '../src/router/index.js';
import PlaceholderView from '../src/views/PlaceholderView.vue';

describe('admin routes', () => {
  it('contains the full first-stage admin menu with readable Chinese labels', () => {
    expect(adminMenuRoutes.map((route) => route.title)).toEqual([
      '仪表盘',
      '文章管理',
      '分类管理',
      '标签管理',
      '评论审核',
      '友链管理',
      '操作日志',
      '用户管理',
      '角色字典'
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
      '/admin/roles'
    ];

    for (const path of p1Paths) {
      const leaf = router.resolve(path).matched.at(-1);

      expect(leaf?.components?.default).toBeTruthy();
      expect(leaf.components.default).not.toBe(PlaceholderView);
    }
  });
});
