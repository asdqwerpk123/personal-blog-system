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
      userId: 7,
      nickName: '站长',
      roleId: 1,
      roleCode: 'SUPER_ADMIN',
      roleName: '超级管理员',
      remember: true
    });

    expect(getStoredAuth()).toEqual({
      token: 'local-token',
      userName: 'admin',
      userId: '7',
      nickName: '站长',
      roleId: '1',
      roleCode: 'SUPER_ADMIN',
      roleName: '超级管理员',
      avatarUrl: '',
      remember: true
    });
    expect(sessionStorage.getItem('PB_ADMIN_TOKEN')).toBeNull();
  });

  it('persists non-remembered sessions in sessionStorage', () => {
    persistAuth({
      token: 'session-token',
      userName: 'editor',
      userId: 8,
      nickName: '编辑',
      roleId: 2,
      roleCode: 'ADMIN',
      roleName: '管理员',
      remember: false
    });

    expect(getStoredAuth()).toEqual({
      token: 'session-token',
      userName: 'editor',
      userId: '8',
      nickName: '编辑',
      roleId: '2',
      roleCode: 'ADMIN',
      roleName: '管理员',
      avatarUrl: '',
      remember: false
    });
    expect(localStorage.getItem('PB_ADMIN_TOKEN')).toBeNull();
  });

  it('clears auth data from both browser storage areas', () => {
    persistAuth({
      token: 'old-token',
      userName: 'admin',
      userId: 7,
      nickName: '站长',
      roleId: 1,
      roleCode: 'SUPER_ADMIN',
      roleName: '超级管理员',
      remember: true
    });

    clearStoredAuth();

    expect(getStoredAuth()).toEqual({
      token: '',
      userName: '',
      userId: '',
      nickName: '',
      roleId: '',
      roleCode: '',
      roleName: '',
      avatarUrl: '',
      remember: true
    });
  });

  it('extracts admin profile fields from login response data', () => {
    const response = {
      data: {
        accessToken: 'token',
        id: 9,
        userName: 'alice',
        nickName: 'Alice',
        roleId: 3,
        roleCode: 'USER',
        roleName: '普通用户'
      }
    };

    persistAuth({
      token: extractAccessToken(response),
      userName: response.data.userName,
      userId: response.data.id,
      nickName: response.data.nickName,
      roleId: response.data.roleId,
      roleCode: response.data.roleCode,
      roleName: response.data.roleName,
      remember: true
    });

    expect(getStoredAuth()).toMatchObject({
      userId: '9',
      userName: 'alice',
      nickName: 'Alice',
      roleId: '3',
      roleCode: 'USER',
      roleName: '普通用户'
    });
  });
});
