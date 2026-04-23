import http from '@/api/http.js';

function compactParams(params) {
  return Object.fromEntries(
    Object.entries(params).filter(([, value]) => value !== '' && value !== null && value !== undefined)
  );
}

function hasConfig(config) {
  return config && Object.keys(config).length > 0;
}

function articlePageParams(filters = {}) {
  const hasAdminParams = 'current' in filters || 'size' in filters || 'keyword' in filters;

  if (hasAdminParams) {
    return compactParams({
      current: filters.current ?? 1,
      size: filters.size ?? 10,
      keyword: filters.keyword?.trim?.() ?? filters.keyword,
      status: filters.status,
      articleId: filters.articleId
    });
  }

  return compactParams({
    page: filters.page ?? 1,
    pageSize: filters.pageSize ?? 10,
    title: filters.title?.trim?.() ?? filters.title ?? '',
    categoryId: filters.categoryId,
    categoryName: filters.categoryName?.trim?.() ?? filters.categoryName,
    status: filters.status
  });
}

export function getArticlePage(filters = {}, config = {}) {
  return http.get('/admin/article/page', {
    ...config,
    params: articlePageParams(filters)
  });
}

export function getArticle(id, config = {}) {
  return hasConfig(config) ? http.get(`/admin/article/${id}`, config) : http.get(`/admin/article/${id}`);
}

export function getCategoryList(config = {}) {
  return hasConfig(config) ? http.get('/admin/category/list', config) : http.get('/admin/category/list');
}

export function createArticle(data, config = {}) {
  return hasConfig(config) ? http.post('/admin/article', data, config) : http.post('/admin/article', data);
}

export function updateArticle(id, data, config = {}) {
  return hasConfig(config) ? http.put(`/admin/article/${id}`, data, config) : http.put(`/admin/article/${id}`, data);
}

export function updateArticleStatus(id, status, config = {}) {
  return http.put(`/admin/article/${id}/status`, null, {
    ...config,
    params: { status }
  });
}

export function deleteArticle(id, config = {}) {
  return hasConfig(config) ? http.delete(`/admin/article/${id}`, config) : http.delete(`/admin/article/${id}`);
}

export function getArticleTags(id, config = {}) {
  return hasConfig(config) ? http.get(`/admin/article/${id}/tags`, config) : http.get(`/admin/article/${id}/tags`);
}

export function updateArticleTags(id, tagIds, config = {}) {
  return hasConfig(config)
    ? http.put(`/admin/article/${id}/tags`, { tagIds }, config)
    : http.put(`/admin/article/${id}/tags`, { tagIds });
}
