import { describe, expect, it } from 'vitest';

import { createAdminProxyConfig } from '../vite.proxy.js';

describe('vite dev proxy', () => {
  it('keeps admin SPA routes out of the backend proxy', () => {
    const proxy = createAdminProxyConfig('http://localhost:8081');
    const apiProxy = proxy['^/api/(admin|user|public)($|/)'];

    expect(proxy['/admin']).toBeUndefined();
    expect(proxy['/admin/article']).toBeUndefined();
    expect(proxy['^/admin/dashboard($|/)']).toBeUndefined();
    expect(proxy['^/admin/profile($|/)']).toBeUndefined();
    expect(apiProxy.target).toBe('http://localhost:8081');
    expect(apiProxy.rewrite('/api/admin/article/page')).toBe('/admin/article/page');
    expect(apiProxy.rewrite('/api/user/profile/me')).toBe('/user/profile/me');
    expect(apiProxy.rewrite('/api/public/articles/page')).toBe('/public/articles/page');
    expect(proxy['^/uploads($|/)'].target).toBe('http://localhost:8081');
  });
});
