import { beforeEach, describe, expect, it } from 'vitest';

import {
  createFriendLink,
  deleteFriendLink,
  getFriendLinkPage,
  updateFriendLink,
  updateFriendLinkStatus,
  uploadFriendLinkLogo
} from '../src/api/friendLinks.js';
import { persistAuth } from '../src/utils/authStorage.js';

describe('friend link admin API', () => {
  beforeEach(() => {
    localStorage.clear();
    sessionStorage.clear();
    persistAuth({
      token: 'admin-token',
      userName: 'admin',
      remember: true
    });
  });

  it('uses friend link CRUD, status, and logo upload endpoints', async () => {
    const capturedConfigs = [];
    const adapter = (config) => {
      capturedConfigs.push(config);

      return Promise.resolve({
        config,
        data: { code: 200, message: '操作成功', data: { url: '/uploads/friend-links/logo.png' } },
        headers: {},
        request: {},
        status: 200,
        statusText: 'OK'
      });
    };

    const payload = {
      siteName: 'Open Source',
      siteUrl: 'https://example.com',
      siteLogo: '/uploads/friend-links/logo.png',
      ownerName: 'Admin',
      contactEmail: 'admin@example.com',
      siteDesc: '示例站点',
      linkStatus: 'PENDING'
    };
    const file = new File(['logo'], 'logo.png', { type: 'image/png' });

    await getFriendLinkPage({ current: 1, size: 10, keyword: 'Open' }, { adapter });
    await createFriendLink(payload, { adapter });
    await updateFriendLink(8, payload, { adapter });
    await updateFriendLinkStatus(8, 'APPROVED', { adapter });
    await uploadFriendLinkLogo(file, { adapter });
    await deleteFriendLink(8, { adapter });

    expect(capturedConfigs.map((config) => `${config.method} ${config.url}`)).toEqual([
      'get /admin/friend-link/page',
      'post /admin/friend-link',
      'put /admin/friend-link/8',
      'put /admin/friend-link/8/status',
      'post /admin/files/friend-link-logo',
      'delete /admin/friend-link/8'
    ]);
    expect(capturedConfigs[0].params).toEqual({ current: 1, size: 10, keyword: 'Open' });
    expect(capturedConfigs[3].params).toEqual({ status: 'APPROVED' });
    expect(capturedConfigs[4].data).toBeInstanceOf(FormData);
    expect(capturedConfigs[4].headers.Authorization).toBe('Bearer admin-token');
    expect(capturedConfigs[4].headers.toJSON()['Content-Type']).toBeUndefined();
  });
});
