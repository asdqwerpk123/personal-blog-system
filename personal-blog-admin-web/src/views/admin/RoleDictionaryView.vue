<template>
  <section class="admin-page">
    <div class="page-heading">
      <div>
        <h1>角色字典</h1>
        <span>当前账号可分配角色，只读展示，不提供完整角色 CRUD</span>
      </div>
    </div>

    <div class="panel admin-list-panel">
      <el-table v-loading="loading" :data="roles" table-layout="fixed">
        <el-table-column prop="roleName" label="角色名称" min-width="160" />
        <el-table-column prop="roleCode" label="角色编码" min-width="160" />
        <el-table-column prop="remark" label="说明" min-width="220" />
      </el-table>
    </div>
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue';

import { getRoleList } from '@/api/roles.js';
import { unwrapData } from './pageData.js';

const loading = ref(false);
const roles = ref([]);

onMounted(async () => {
  loading.value = true;

  try {
    const data = unwrapData(await getRoleList());
    roles.value = Array.isArray(data) ? data : data.records || data.list || [];
  } finally {
    loading.value = false;
  }
});
</script>
