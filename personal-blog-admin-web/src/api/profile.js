import http from '@/api/http.js';

export function getMyProfile() {
  return http.get('/admin/profile/me');
}

export function updateMyProfile(data) {
  return http.put('/admin/profile/me', data);
}

export function updateMyPassword(data) {
  return http.put('/admin/profile/password', data);
}

export function uploadAvatar(file, config = {}) {
  const formData = new FormData();
  formData.append('file', file);

  return Object.keys(config).length
    ? http.post('/admin/files/avatar', formData, config)
    : http.post('/admin/files/avatar', formData);
}
