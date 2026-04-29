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

export function getAuthorCommentPage(params = {}) {
  return http.get('/user/comments/page', {
    params: {
      current: params.current ?? params.page ?? 1,
      size: params.size ?? params.pageSize ?? 10,
      keyword: params.keyword,
      status: params.status
    }
  });
}

export function createAuthorComment(data) {
  return http.post('/user/comments', data);
}

export function deleteAuthorComment(id) {
  return http.delete(`/user/comments/${id}`);
}
