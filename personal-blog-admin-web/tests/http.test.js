import { beforeEach, describe, expect, it, vi } from 'vitest';

vi.mock('element-plus', () => ({
  ElMessage: {
    error: vi.fn()
  }
}));

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

  it('clears stored auth when wrapped admin responses are unauthorized', async () => {
    window.history.pushState({}, '', '/login');
    persistAuth({
      token: 'expired-token',
      userName: 'admin',
      remember: true
    });

    const adapter = (config) => Promise.resolve({
      config,
      data: {
        code: 401,
        message: '未授权'
      },
      headers: {},
      request: {},
      status: 200,
      statusText: 'OK'
    });

    await expect(http.get('/admin/article/1', { adapter }))
      .rejects
      .toThrow('未授权');

    expect(getStoredAuth().token).toBe('');
  });
});
