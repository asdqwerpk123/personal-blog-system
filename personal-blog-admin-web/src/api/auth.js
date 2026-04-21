import http from '@/api/http.js';

export function login(credentials) {
  return http.post('/admin/auth/login', credentials);
}
