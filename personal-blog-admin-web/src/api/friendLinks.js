import http from '@/api/http.js';

function hasConfig(config) {
  return config && Object.keys(config).length > 0;
}

export function getFriendLinkPage(params, config = {}) {
  return http.get('/admin/friend-link/page', {
    ...config,
    params
  });
}

export function createFriendLink(data, config = {}) {
  return hasConfig(config) ? http.post('/admin/friend-link', data, config) : http.post('/admin/friend-link', data);
}

export function updateFriendLink(id, data, config = {}) {
  return hasConfig(config)
    ? http.put(`/admin/friend-link/${id}`, data, config)
    : http.put(`/admin/friend-link/${id}`, data);
}

export function updateFriendLinkStatus(id, status, config = {}) {
  return http.put(`/admin/friend-link/${id}/status`, null, {
    ...config,
    params: { status }
  });
}

export function uploadFriendLinkLogo(file, config = {}) {
  const formData = new FormData();
  formData.append('file', file);

  return hasConfig(config)
    ? http.post('/admin/files/friend-link-logo', formData, config)
    : http.post('/admin/files/friend-link-logo', formData);
}

export function deleteFriendLink(id, config = {}) {
  return hasConfig(config) ? http.delete(`/admin/friend-link/${id}`, config) : http.delete(`/admin/friend-link/${id}`);
}
