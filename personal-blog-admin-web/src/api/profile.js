import http from '@/api/http.js';

function uploadTo(path, file, config = {}) {
  const formData = new FormData();
  formData.append('file', file);

  return Object.keys(config).length
    ? http.post(path, formData, config)
    : http.post(path, formData);
}

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
  return uploadTo('/admin/files/avatar', file, config);
}

export function getAuthorProfile() {
  return http.get('/user/profile/me');
}

export function updateAuthorProfile(data) {
  return http.put('/user/profile/me', data);
}

export function updateAuthorPassword(data) {
  return http.put('/user/profile/password', {
    oldPassword: data.oldPassword ?? data.currentPassword,
    newPassword: data.newPassword
  });
}

export function uploadAuthorAvatar(file, config = {}) {
  return uploadTo('/user/files/avatar', file, config);
}
