export function unwrapData(response) {
  return response?.data ?? response ?? {};
}

export function normalizePage(response) {
  const data = unwrapData(response);
  const records = data.records || data.list || data.rows || [];
  const total = data.total ?? data.totalCount ?? records.length;

  return {
    records: Array.isArray(records) ? records : [],
    total: Number(total) || 0
  };
}

export function rowId(row) {
  return row?.id ?? row?.userId ?? row?.articleId ?? row?.categoryId ?? row?.tagId ?? row?.linkId ?? row?.logId;
}

export function pick(row, keys, fallback = '-') {
  for (const key of keys) {
    if (row?.[key] !== undefined && row[key] !== null && row[key] !== '') {
      return row[key];
    }
  }

  return fallback;
}

export function statusType(status) {
  const value = String(status || '').toUpperCase();

  if (['ENABLED', 'NORMAL', 'PUBLISHED', 'APPROVED', 'SUCCESS', 'ACTIVE'].includes(value)) {
    return 'success';
  }

  if (['DISABLED', 'REJECTED', 'FAILED', 'FAILURE', 'DELETED'].includes(value)) {
    return 'danger';
  }

  if (['PENDING', 'DRAFT'].includes(value)) {
    return 'warning';
  }

  return 'info';
}
