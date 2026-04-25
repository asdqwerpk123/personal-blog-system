const BUSINESS_ERROR_MAP = [
  [/categoryname already exists/i, '分类名称已存在'],
  [/category is referenced by articles/i, '该分类下存在文章，不能删除'],
  [/tagname already exists/i, '标签名称已存在'],
  [/tag is referenced by articles/i, '该标签下存在文章，不能删除'],
  [/friend link is referenced/i, '该友链数据被引用，不能删除'],
  [/siteurl already exists/i, '站点地址已存在'],
  [/articleslug already exists/i, '文章短链接已存在'],
  [/username or password is incorrect/i, '用户名或密码错误'],
  [/cannot manage this user|cannot create super_admin|administrator can only manage user|cannot disable current user|cannot assign super_admin|cannot manage super_admin|cannot change own role/i, '无权限管理该用户'],
  [/old password is incorrect/i, '原密码错误'],
  [/unsupported image type/i, '不支持的图片格式'],
  [/file size exceeds limit/i, '图片大小不能超过限制'],
  [/unauthorized|未授权/i, '未登录或登录已过期'],
  [/forbidden/i, '无权限操作']
];

export function toChineseBusinessMessage(message, fallback = '操作失败，请稍后重试') {
  if (!message) {
    return fallback;
  }

  if (/[\u4e00-\u9fa5]/.test(message) && !/未授权/.test(message)) {
    return message;
  }

  const matched = BUSINESS_ERROR_MAP.find(([pattern]) => pattern.test(message));
  return matched ? matched[1] : fallback;
}
