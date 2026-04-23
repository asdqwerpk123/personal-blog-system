import http from '@/api/http.js';

function compactParams(params) {
  return Object.fromEntries(
    Object.entries(params).filter(([, value]) => value !== '' && value !== null && value !== undefined)
  );
}

function hasConfig(config) {
  return config && Object.keys(config).length > 0;
}

function normalizePayload(payload) {
  const data = { ...payload };

  if (typeof data.categoryName === 'string') {
    data.categoryName = data.categoryName.trim();
  }
  if (typeof data.description === 'string') {
    data.description = data.description.trim();
  }
  if (data.sortNo !== '' && data.sortNo !== null && data.sortNo !== undefined) {
    data.sortNo = Number(data.sortNo);
  }

  return data;
}

function categoryPageParams(filters = {}) {
  const hasAdminParams = 'current' in filters || 'size' in filters || 'keyword' in filters;

  if (hasAdminParams) {
    return compactParams({
      current: filters.current ?? 1,
      size: filters.size ?? 10,
      keyword: filters.keyword?.trim?.() ?? filters.keyword
    });
  }

  return compactParams({
    page: filters.page ?? 1,
    pageSize: filters.pageSize ?? 10,
    categoryName: filters.categoryName?.trim?.() ?? filters.categoryName
  });
}

export function getCategoryList(config = {}) {
  return hasConfig(config) ? http.get('/admin/category/list', config) : http.get('/admin/category/list');
}

export function getCategory(id, config = {}) {
  return hasConfig(config) ? http.get(`/admin/category/${id}`, config) : http.get(`/admin/category/${id}`);
}

export function getCategoryPage(filters = {}, config = {}) {
  return http.get('/admin/category/page', {
    ...config,
    params: categoryPageParams(filters)
  });
}

export function createCategory(payload, config = {}) {
  const data = normalizePayload(payload);
  return hasConfig(config) ? http.post('/admin/category', data, config) : http.post('/admin/category', data);
}

export function updateCategory(id, payload, config = {}) {
  const data = normalizePayload(payload);
  return hasConfig(config) ? http.put(`/admin/category/${id}`, data, config) : http.put(`/admin/category/${id}`, data);
}

export function deleteCategory(id, config = {}) {
  return hasConfig(config) ? http.delete(`/admin/category/${id}`, config) : http.delete(`/admin/category/${id}`);
}
