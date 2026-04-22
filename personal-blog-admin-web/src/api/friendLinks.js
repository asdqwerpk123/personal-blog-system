import http from '@/api/http.js';

export function getFriendLinkPage(params) {
  return http.get('/admin/friend-link/page', { params });
}

export function createFriendLink(data) {
  return http.post('/admin/friend-link', data);
}

export function updateFriendLink(id, data) {
  return http.put(`/admin/friend-link/${id}`, data);
}

export function deleteFriendLink(id) {
  return http.delete(`/admin/friend-link/${id}`);
}
