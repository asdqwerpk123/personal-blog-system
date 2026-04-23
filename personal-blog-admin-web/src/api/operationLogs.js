import http from '@/api/http.js';

export function getOperationLogPage(params) {
  return http.get('/admin/operation-log/page', { params });
}
