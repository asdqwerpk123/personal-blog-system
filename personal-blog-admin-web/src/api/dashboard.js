import http from '@/api/http.js';

export function getDashboardSummary() {
  return http.get('/admin/dashboard/summary');
}

export function getAuthorDashboardSummary() {
  return http.get('/user/dashboard/summary');
}
