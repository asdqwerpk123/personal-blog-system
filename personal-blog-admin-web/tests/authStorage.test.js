import { beforeEach, describe, expect, it } from 'vitest';

import {
  clearStoredAuth,
  extractAccessToken,
  getStoredAuth,
  persistAuth
} from '../src/utils/authStorage.js';

describe('authStorage', () => {
  beforeEach(() => {
    localStorage.clear();
    sessionStorage.clear();
  });

  it('extracts the access token from the unified backend response', () => {
    const token = extractAccessToken({
      code: 200,
      message: '操作成功',
      data: {
        accessToken: 'Bearer admin-token'
      }
    });

    expect(token).toBe('admin-token');
  });

  it('persists remembered sessions in localStorage', () => {
    persistAuth({
      token: 'local-token',
      userName: 'admin',
      remember: true
    });

    expect(getStoredAuth()).toEqual({
      token: 'local-token',
      userName: 'admin',
      remember: true
    });
    expect(sessionStorage.getItem('PB_ADMIN_TOKEN')).toBeNull();
  });

  it('persists non-remembered sessions in sessionStorage', () => {
    persistAuth({
      token: 'session-token',
      userName: 'editor',
      remember: false
    });

    expect(getStoredAuth()).toEqual({
      token: 'session-token',
      userName: 'editor',
      remember: false
    });
    expect(localStorage.getItem('PB_ADMIN_TOKEN')).toBeNull();
  });

  it('clears auth data from both browser storage areas', () => {
    persistAuth({
      token: 'old-token',
      userName: 'admin',
      remember: true
    });

    clearStoredAuth();

    expect(getStoredAuth()).toEqual({
      token: '',
      userName: '',
      remember: true
    });
  });
});
