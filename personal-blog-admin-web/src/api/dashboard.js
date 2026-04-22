import http from '@/api/http.js';

export function getDashboardSummary() {
  return http.get('/admin/dashboard/summary');
}
