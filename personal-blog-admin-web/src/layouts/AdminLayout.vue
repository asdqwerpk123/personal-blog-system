<template>
  <div class="admin-shell">
    <aside class="admin-sidebar">
      <RouterLink class="brand" to="/admin/dashboard" aria-label="进入仪表盘">
        <span class="brand__mark">
          <Notebook />
        </span>
        <span class="brand__text">博客后台</span>
      </RouterLink>

      <el-menu
        class="admin-menu"
        :default-active="route.path"
        background-color="transparent"
        text-color="#a8b3c7"
        active-text-color="#ffffff"
        router
      >
        <el-menu-item v-for="item in adminMenuRoutes" :key="item.path" :index="item.path">
          <el-icon>
            <component :is="iconMap[item.icon]" />
          </el-icon>
          <span>{{ item.title }}</span>
        </el-menu-item>
      </el-menu>

      <button class="sidebar-logout" type="button" @click="handleLogout">
        <el-icon><SwitchButton /></el-icon>
        <span>退出登录</span>
      </button>
    </aside>

    <section class="admin-main">
      <header class="admin-topbar">
        <div class="topbar-search">
          <el-icon><Search /></el-icon>
          <input type="search" placeholder="搜索..." aria-label="搜索" />
        </div>

        <div class="topbar-actions">
          <button class="icon-button" type="button" aria-label="通知">
            <Bell />
            <span class="notice-dot"></span>
          </button>
          <div class="profile">
            <span class="profile__avatar">{{ userInitial }}</span>
            <span class="profile__name">{{ displayName }}</span>
            <el-icon><ArrowDown /></el-icon>
          </div>
        </div>
      </header>

      <main class="admin-content">
        <RouterView />
      </main>
    </section>
  </div>
</template>

<script setup>
import {
  ArrowDown,
  Bell,
  ChatLineSquare,
  Connection,
  Document,
  Grid,
  Link,
  Lock,
  Notebook,
  PriceTag,
  Search,
  SwitchButton,
  TrendCharts,
  User
} from '@element-plus/icons-vue';
import { computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import { adminMenuRoutes } from '@/router/adminMenu.js';
import { useAuthStore } from '@/stores/auth.js';

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();

const iconMap = {
  ChatLineSquare,
  Connection,
  Document,
  Grid,
  Link,
  Lock,
  PriceTag,
  TrendCharts,
  User
};

const displayName = computed(() => authStore.nickName || authStore.userName || '管理员');
const userInitial = computed(() => displayName.value.slice(0, 1).toUpperCase());

function handleLogout() {
  authStore.logout();
  router.push('/login');
}
</script>
