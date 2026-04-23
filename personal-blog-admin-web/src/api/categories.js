import http from '@/api/http.js';

function compactParams(params) {
  return Object.fromEntries(
    Object.entries(params).filter(([, value]) => value !== '' && value !== null && value !== undefined)
  );
}

function normalizePayload(payload) {
  return {
    categoryName: payload.categoryName.trim(),
    description: payload.description?.trim() || '',
    sortNo: Number(payload.sortNo)
  };
}

export function getCategoryPage(filters = {}, config = {}) {
  const {
    page = 1,
    pageSize = 10,
    categoryName = ''
  } = filters;

  return http.get('/admin/category/page', {
    ...config,
    params: compactParams({
      page,
      pageSize,
      categoryName: categoryName.trim()
    })
  });
}

export function getCategoryList(config = {}) {
  return http.get('/admin/category/list', config);
}

export function createCategory(payload, config = {}) {
  return http.post('/admin/category', normalizePayload(payload), config);
}

export function updateCategory(id, payload, config = {}) {
  return http.put(`/admin/category/${id}`, normalizePayload(payload), config);
}

export function deleteCategory(id, config = {}) {
  return http.delete(`/admin/category/${id}`, config);
}
