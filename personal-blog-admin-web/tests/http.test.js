import { beforeEach, describe, expect, it } from 'vitest';

import http from '../src/api/http.js';
import { getStoredAuth, persistAuth } from '../src/utils/authStorage.js';

describe('http client authorization', () => {
  beforeEach(() => {
    localStorage.clear();
    sessionStorage.clear();
  });

  it('adds Authorization only for admin API requests', async () => {
    persistAuth({
      token: 'admin-token',
      userName: 'admin',
      remember: true
    });

    const capturedConfigs = [];
    const adapter = (config) => {
      capturedConfigs.push(config);

      return Promise.resolve({
        config,
        data: { ok: true },
        headers: {},
        request: {},
        status: 200,
        statusText: 'OK'
      });
    };

    await http.get('/admin/article/1', { adapter });
    await http.get('/public/ping', { adapter });

    expect(capturedConfigs[0].headers.Authorization).toBe('Bearer admin-token');
    expect(capturedConfigs[1].headers.Authorization).toBeUndefined();
  });

  it('does not handle login 401 responses as expired sessions', async () => {
    persistAuth({
      token: 'existing-token',
      userName: 'admin',
      remember: true
    });

    const adapter = (config) => Promise.reject({
      config,
      response: {
        status: 401
      }
    });

    await expect(http.post('/admin/auth/login', { userName: 'admin', password: 'wrong' }, { adapter }))
      .rejects
      .toMatchObject({
        response: {
          status: 401
        }
      });

    expect(getStoredAuth().token).toBe('existing-token');
  });
});
