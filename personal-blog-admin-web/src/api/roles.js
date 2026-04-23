import http from '@/api/http.js';

export function getRoleList() {
  return http.get('/admin/role/list');
}

export function getRole(id) {
  return http.get(`/admin/role/${id}`);
}
