import http from '@/api/http.js';

export function getCategoryList() {
  return http.get('/admin/category/list');
}

export function getCategory(id) {
  return http.get(`/admin/category/${id}`);
}

export function getCategoryPage(params) {
  return http.get('/admin/category/page', { params });
}

export function createCategory(data) {
  return http.post('/admin/category', data);
}

export function updateCategory(id, data) {
  return http.put(`/admin/category/${id}`, data);
}

export function deleteCategory(id) {
  return http.delete(`/admin/category/${id}`);
}
