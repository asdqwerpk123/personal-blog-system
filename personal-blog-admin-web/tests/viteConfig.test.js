import { describe, expect, it } from 'vitest';

import { createAdminProxyConfig } from '../vite.proxy.js';

describe('vite dev proxy', () => {
  it('keeps admin SPA routes out of the backend proxy', () => {
    const proxy = createAdminProxyConfig('http://localhost:8081');

    expect(proxy['/admin']).toBeUndefined();
    expect(proxy['/admin/article']).toBeUndefined();
    expect(proxy['^/admin/article($|/)'].target).toBe('http://localhost:8081');
    expect(proxy['^/admin/category($|/)'].target).toBe('http://localhost:8081');
  });
});
