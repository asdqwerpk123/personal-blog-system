import http from '@/api/http.js';

export function getArticlePage(params) {
  return http.get('/admin/article/page', { params });
}

export function getArticle(id) {
  return http.get(`/admin/article/${id}`);
}

export function createArticle(data) {
  return http.post('/admin/article', data);
}

export function updateArticle(id, data) {
  return http.put(`/admin/article/${id}`, data);
}

export function updateArticleStatus(id, status) {
  return http.put(`/admin/article/${id}/status`, null, { params: { status } });
}

export function deleteArticle(id) {
  return http.delete(`/admin/article/${id}`);
}

export function getArticleTags(id) {
  return http.get(`/admin/article/${id}/tags`);
}

export function updateArticleTags(id, tagIds) {
  return http.put(`/admin/article/${id}/tags`, { tagIds });
}
