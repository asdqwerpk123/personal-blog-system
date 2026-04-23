import http from '@/api/http.js';

function compactParams(params) {
  return Object.fromEntries(
    Object.entries(params).filter(([, value]) => value !== '' && value !== null && value !== undefined)
  );
}

export function getArticlePage(filters = {}, config = {}) {
  const {
    page = 1,
    pageSize = 10,
    title = '',
    categoryId = '',
    categoryName = '',
    status = ''
  } = filters;

  return http.get('/admin/article/page', {
    ...config,
    params: compactParams({
      page,
      pageSize,
      title: title.trim(),
      categoryId,
      categoryName,
      status
    })
  });
}

export function getCategoryList(config = {}) {
  return http.get('/admin/category/list', config);
}

export function deleteArticle(id, config = {}) {
  return http.delete(`/admin/article/${id}`, config);
}

export function updateArticleStatus(id, status, config = {}) {
  return http.put(`/admin/article/${id}/status`, null, {
    ...config,
    params: {
      status
    }
  });
}
