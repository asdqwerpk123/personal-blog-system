import { describe, expect, it } from 'vitest';

import { adminMenuRoutes } from '../src/router/adminMenu.js';

describe('admin routes', () => {
  it('contains the full first-stage admin menu', () => {
    expect(adminMenuRoutes.map((route) => route.title)).toEqual([
      '仪表盘',
      '文章管理',
      '分类管理',
      '标签管理',
      '评论管理',
      '友情链接',
      '操作日志',
      '用户管理',
      '角色管理'
    ]);
  });
});
