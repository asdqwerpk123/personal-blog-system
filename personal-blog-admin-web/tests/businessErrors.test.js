import { describe, expect, it } from 'vitest';

import { toChineseBusinessMessage } from '../src/utils/businessErrors.js';

describe('business error messages', () => {
  it('translates common backend business errors to Chinese', () => {
    expect(toChineseBusinessMessage('categoryName already exists')).toBe('分类名称已存在');
    expect(toChineseBusinessMessage('category is referenced by articles')).toBe('该分类下存在文章，不能删除');
    expect(toChineseBusinessMessage('tagName already exists')).toBe('标签名称已存在');
    expect(toChineseBusinessMessage('friend link is referenced')).toBe('该友链数据被引用，不能删除');
    expect(toChineseBusinessMessage('unsupported image type')).toBe('不支持的图片格式');
    expect(toChineseBusinessMessage('file size exceeds limit')).toBe('图片大小不能超过 10MB');
    expect(toChineseBusinessMessage('cannot manage this user')).toBe('无权限管理该用户');
    expect(toChineseBusinessMessage('old password is incorrect')).toBe('原密码错误');
    expect(toChineseBusinessMessage('unauthorized')).toBe('未登录或登录已过期');
    expect(toChineseBusinessMessage('forbidden')).toBe('无权限操作');
  });

  it('keeps existing Chinese messages and falls back to generic Chinese failure', () => {
    expect(toChineseBusinessMessage('请选择图片文件')).toBe('请选择图片文件');
    expect(toChineseBusinessMessage('Request failed with status code 500')).toBe('操作失败，请稍后重试');
  });
});
