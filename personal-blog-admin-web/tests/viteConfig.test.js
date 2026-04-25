import { describe, expect, it } from 'vitest';

import { createAdminProxyConfig } from '../vite.proxy.js';

describe('vite dev proxy', () => {
  it('keeps admin SPA routes out of the backend proxy', () => {
    const proxy = createAdminProxyConfig('http://localhost:8081');

    expect(proxy['/admin']).toBeUndefined();
    expect(proxy['/admin/article']).toBeUndefined();
    expect(proxy['^/admin/article($|/)'].target).toBe('http://localhost:8081');
    expect(proxy['^/admin/category($|/)'].target).toBe('http://localhost:8081');
    expect(proxy['^/admin/dashboard($|/)']).toBeUndefined();
    expect(proxy['^/admin/dashboard/summary($|/)'].target).toBe('http://localhost:8081');
    expect(proxy['^/admin/files($|/)'].target).toBe('http://localhost:8081');
    expect(proxy['^/admin/profile($|/)']).toBeUndefined();
    expect(proxy['^/admin/profile/(me|password)($|/)'].target).toBe('http://localhost:8081');
    expect(proxy['^/uploads($|/)'].target).toBe('http://localhost:8081');
  });
});
