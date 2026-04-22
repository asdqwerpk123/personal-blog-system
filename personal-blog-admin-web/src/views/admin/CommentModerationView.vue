<template>
  <ResourceManagementView
    title="评论审核"
    description="筛选待审核评论并执行通过、驳回或删除"
    short-title="评论"
    search-placeholder="搜索评论内容、用户"
    :columns="columns"
    :fetch-page="getCommentPage"
    :delete-row="deleteComment"
    :update-status="updateCommentStatus"
    :status-options="statusOptions"
    show-article-filter
    can-audit
  />
</template>

<script setup>
import { deleteComment, getCommentPage, updateCommentStatus } from '@/api/comments.js';
import ResourceManagementView from './ResourceManagementView.vue';

const columns = [
  { label: '评论内容', prop: 'content', keys: ['content', 'commentContent'], minWidth: 240 },
  { label: '文章', prop: 'articleTitle', keys: ['articleTitle', 'articleName', 'articleId'], minWidth: 160 },
  { label: '状态', prop: 'commentStatus', keys: ['commentStatus', 'status', 'auditStatus'], type: 'status', minWidth: 110 },
  { label: '创建时间', prop: 'createTime', minWidth: 170 }
];

const statusOptions = [
  { label: '待审核', value: 'PENDING' },
  { label: '已通过', value: 'APPROVED' },
  { label: '已驳回', value: 'REJECTED' }
];
</script>
