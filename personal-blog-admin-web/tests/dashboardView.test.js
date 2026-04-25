import { mount } from '@vue/test-utils';
import ElementPlus from 'element-plus';
import { describe, expect, it, vi } from 'vitest';

import { getDashboardSummary } from '../src/api/dashboard.js';
import DashboardView from '../src/views/DashboardView.vue';

vi.mock('../src/api/dashboard.js', () => ({
  getDashboardSummary: vi.fn(() => Promise.resolve({
    data: {
      articleCount: 12,
      categoryCount: 3,
      tagCount: 8,
      commentCount: 21,
      pendingCommentCount: 4,
      friendLinkCount: 2,
      latestArticles: [
        {
          id: 7,
          articleTitle: 'Real API Article',
          categoryName: 'Backend',
          viewCount: 88,
          articleStatus: 'PUBLISHED',
          updateTime: '2026-04-22T09:15:00'
        }
      ],
      latestComments: [
        {
          id: 9,
          nickName: 'Alice',
          commentContent: 'Needs review',
          commentStatus: 'PENDING',
          articleTitle: 'Real API Article',
          updateTime: '2026-04-22T09:20:00'
        }
      ],
      latestOperationLogs: [
        {
          id: 11,
          operatorUserId: 1,
          targetType: 'TAG',
          actionType: 'UPDATE',
          actionResult: 'SUCCESS',
          actionDetail: 'Update tag success',
          createTime: '2026-04-22T09:30:00'
        }
      ]
    }
  }))
}));

describe('DashboardView', () => {
  it('loads and renders dashboard summary from the admin API', async () => {
    const wrapper = mount(DashboardView, {
      global: {
        plugins: [ElementPlus]
      }
    });

    await vi.dynamicImportSettled();

    expect(getDashboardSummary).toHaveBeenCalledTimes(1);
    expect(wrapper.text()).toContain('12');
    expect(wrapper.text()).toContain('Real API Article');
    expect(wrapper.text()).toContain('Needs review');
    expect(wrapper.text()).toContain('Update tag success');
  });
});
