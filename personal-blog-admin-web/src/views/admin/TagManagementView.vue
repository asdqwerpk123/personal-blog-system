<template>
  <ResourceManagementView
    title="标签管理"
    description="维护文章标签"
    short-title="标签"
    search-placeholder="搜索标签名称"
    :columns="columns"
    :form-fields="formFields"
    :fetch-page="getTagPage"
    :create-row="createTag"
    :update-row="updateTag"
    :delete-row="deleteTag"
  />
</template>

<script setup>
import { createTag, deleteTag, getTagPage, updateTag } from '@/api/tags.js';
import ResourceManagementView from './ResourceManagementView.vue';

const columns = [
  { label: '标签名称', prop: 'tagName', keys: ['tagName', 'name'], minWidth: 180 },
  { label: '文章数', prop: 'articleCount', keys: ['articleCount', 'count'], minWidth: 100 },
  { label: '更新时间', prop: 'updateTime', keys: ['updateTime', 'createTime'], minWidth: 170 }
];

const formFields = [
  { label: '标签名称', prop: 'tagName' },
  { label: '描述', prop: 'description', type: 'textarea' }
];
</script>
