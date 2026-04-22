import http from '@/api/http.js';

export function getUserPage(params) {
  return http.get('/admin/user/page', { params });
}

export function getUser(id) {
  return http.get(`/admin/user/${id}`);
}

export function createUser(data) {
  return http.post('/admin/user', data);
}

export function updateUser(id, data) {
  return http.put(`/admin/user/${id}`, data);
}

export function updateUserStatus(id, userStatus) {
  return http.put(`/admin/user/${id}/status`, { userStatus });
}

export function resetUserPassword(id, newPassword) {
  return http.put(`/admin/user/${id}/password/reset`, { newPassword });
}
