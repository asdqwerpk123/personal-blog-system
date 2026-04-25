<template>
  <ResourceManagementView
    title="操作日志"
    description="查看后台操作记录、对象类型和执行结果"
    short-title="日志"
    search-placeholder="日志关键词"
    :columns="columns"
    :fetch-page="getOperationLogPage"
    :result-options="resultOptions"
    show-operator-filter
    show-target-filter
    readonly
  />
</template>

<script setup>
import { getOperationLogPage } from '@/api/operationLogs.js';
import ResourceManagementView from './ResourceManagementView.vue';

const columns = [
  { label: '操作者', prop: 'operatorUserName', keys: ['operatorUserName', 'operatorName', 'userName', 'operatorUserId'], minWidth: 130 },
  { label: '对象类型', prop: 'targetType', minWidth: 120 },
  { label: '动作', prop: 'actionName', keys: ['actionName', 'actionType', 'operation'], minWidth: 150 },
  { label: '详情', prop: 'actionDetail', keys: ['actionDetail', 'detail'], minWidth: 220 },
  { label: '结果', prop: 'actionResult', type: 'status', minWidth: 100 },
  { label: '时间', prop: 'createTime', keys: ['createTime', 'operationTime'], minWidth: 170 }
];

const resultOptions = [
  { label: '成功', value: 'SUCCESS' },
  { label: '失败', value: 'FAILURE' }
];
</script>
