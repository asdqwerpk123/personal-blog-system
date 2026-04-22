import http from '@/api/http.js';

export function getTagPage(params) {
  return http.get('/admin/tag/page', { params });
}

export function createTag(data) {
  return http.post('/admin/tag', data);
}

export function updateTag(id, data) {
  return http.put(`/admin/tag/${id}`, data);
}

export function deleteTag(id) {
  return http.delete(`/admin/tag/${id}`);
}
