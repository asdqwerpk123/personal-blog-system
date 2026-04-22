<template>
  <ResourceManagementView
    title="友链管理"
    description="维护友情链接申请和展示状态"
    short-title="友链"
    search-placeholder="搜索站点名称"
    :columns="columns"
    :form-fields="formFields"
    :fetch-page="getFriendLinkPage"
    :create-row="createFriendLink"
    :update-row="updateFriendLink"
    :delete-row="deleteFriendLink"
    :status-options="statusOptions"
  />
</template>

<script setup>
import { createFriendLink, deleteFriendLink, getFriendLinkPage, updateFriendLink } from '@/api/friendLinks.js';
import ResourceManagementView from './ResourceManagementView.vue';

const columns = [
  { label: '站点名称', prop: 'siteName', keys: ['siteName', 'linkName', 'name'], minWidth: 180 },
  { label: '链接', prop: 'siteUrl', keys: ['siteUrl', 'linkUrl', 'url'], minWidth: 220 },
  { label: '状态', prop: 'status', keys: ['status', 'linkStatus'], type: 'status', minWidth: 110 },
  { label: '更新时间', prop: 'updateTime', keys: ['updateTime', 'createTime'], minWidth: 170 }
];

const formFields = [
  { label: '站点名称', prop: 'siteName' },
  { label: '链接地址', prop: 'siteUrl' },
  { label: '站点 Logo', prop: 'siteLogo' },
  { label: '站长名称', prop: 'ownerName' },
  { label: '联系邮箱', prop: 'contactEmail' },
  { label: '站点描述', prop: 'siteDesc', type: 'textarea' }
];

const statusOptions = [
  { label: '待审核', value: 'PENDING' },
  { label: '已通过', value: 'APPROVED' },
  { label: '已驳回', value: 'REJECTED' }
];
</script>
