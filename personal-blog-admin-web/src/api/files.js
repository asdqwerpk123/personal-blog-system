import http from '@/api/http.js';

function hasConfig(config) {
  return config && Object.keys(config).length > 0;
}

function uploadTo(path, file, config = {}) {
  const formData = new FormData();
  formData.append('file', file);

  return hasConfig(config)
    ? http.post(path, formData, config)
    : http.post(path, formData);
}

export function uploadArticleCover(file, config = {}) {
  return uploadTo('/admin/files/article-cover', file, config);
}

export function uploadFile(file, config = {}) {
  return uploadTo('/admin/files/upload', file, config);
}
