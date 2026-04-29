import http from '@/api/http.js';

function hasConfig(config) {
  return config && Object.keys(config).length > 0;
}

export function uploadFile(file, config = {}) {
  const formData = new FormData();
  formData.append('file', file);

  return hasConfig(config)
    ? http.post('/admin/files/upload', formData, config)
    : http.post('/admin/files/upload', formData);
}
