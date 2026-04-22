import http from '@/api/http.js';

export function getCommentPage(params) {
  return http.get('/admin/comment/page', { params });
}

export function getArticleComments(articleId) {
  return http.get(`/admin/comment/article/${articleId}`);
}

export function updateCommentStatus(id, status) {
  return http.put(`/admin/comment/${id}/status`, null, { params: { status } });
}

export function deleteComment(id) {
  return http.delete(`/admin/comment/${id}`);
}
