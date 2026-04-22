<template>
  <ResourceManagementView
    title="文章管理"
    description="维护文章内容、发布状态、标签关系和删除回收"
    short-title="文章"
    search-placeholder="搜索标题、摘要"
    :columns="columns"
    :form-fields="formFields"
    :fetch-page="getArticlePage"
    :create-row="createArticle"
    :update-row="updateArticle"
    :delete-row="deleteArticle"
    :update-status="updateArticleStatus"
    :fetch-tags="getTagPage"
    :fetch-row-tags="getArticleTags"
    :update-tags="updateArticleTags"
    can-publish
    can-tags
  />
</template>

<script setup>
import {
  createArticle,
  deleteArticle,
  getArticlePage,
  getArticleTags,
  updateArticle,
  updateArticleStatus,
  updateArticleTags
} from '@/api/articles.js';
import { getTagPage } from '@/api/tags.js';
import ResourceManagementView from './ResourceManagementView.vue';

const columns = [
  { label: '标题', prop: 'title', keys: ['title', 'articleTitle'], minWidth: 220 },
  { label: '分类', prop: 'categoryName', keys: ['categoryName', 'category', 'categoryId'], minWidth: 120 },
  { label: '状态', prop: 'status', keys: ['status', 'articleStatus'], type: 'status', minWidth: 110 },
  { label: '更新时间', prop: 'updateTime', keys: ['updateTime', 'createTime'], minWidth: 170 }
];

const formFields = [
  { label: '标题', prop: 'articleTitle' },
  { label: '短链接', prop: 'articleSlug' },
  { label: '摘要', prop: 'articleSummary', type: 'textarea' },
  { label: '内容', prop: 'articleContent', type: 'textarea' },
  { label: '分类 ID', prop: 'categoryId' }
];
</script>
