<template>
  <ResourceManagementView
    title="分类管理"
    description="维护文章分类字典"
    short-title="分类"
    search-placeholder="搜索分类名称"
    :columns="columns"
    :form-fields="formFields"
    :fetch-page="getCategoryPage"
    :create-row="createCategory"
    :update-row="updateCategory"
    :delete-row="deleteCategory"
  />
</template>

<script setup>
import { createCategory, deleteCategory, getCategoryPage, updateCategory } from '@/api/categories.js';
import ResourceManagementView from './ResourceManagementView.vue';

const columns = [
  { label: '分类名称', prop: 'categoryName', keys: ['categoryName', 'name'], minWidth: 180 },
  { label: '排序', prop: 'sortNo', keys: ['sortNo', 'sort', 'sortOrder'], minWidth: 90 },
  { label: '状态', prop: 'status', keys: ['status', 'categoryStatus'], type: 'status', minWidth: 110 },
  { label: '更新时间', prop: 'updateTime', keys: ['updateTime', 'createTime'], minWidth: 170 }
];

const formFields = [
  { label: '分类名称', prop: 'categoryName' },
  { label: '排序', prop: 'sortNo' },
  { label: '描述', prop: 'description', type: 'textarea' }
];
</script>
