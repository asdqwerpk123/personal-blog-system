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
        <div class="topbar-actions">
          <el-dropdown trigger="click" @command="handleProfileCommand">
            <button class="profile profile--button" type="button" aria-label="用户菜单">
              <span class="profile__avatar">
                <img v-if="authStore.avatarUrl" class="profile__avatar-img" :src="authStore.avatarUrl" alt="头像" />
                <span v-else>{{ userInitial }}</span>
              </span>
              <span class="profile__name">{{ displayName }}</span>
              <el-icon><ArrowDown /></el-icon>
            </button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">个人资料</el-dropdown-item>
                <el-dropdown-item command="password">修改密码</el-dropdown-item>
                <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
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
  ChatLineSquare,
  Connection,
  Document,
  Grid,
  Link,
  Notebook,
  PriceTag,
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
  PriceTag,
  TrendCharts,
  User
};

const displayName = computed(() => authStore.nickName || authStore.userName || '管理员');
const userInitial = computed(() => displayName.value.slice(0, 1).toUpperCase());

function handleLogout() {
  authStore.logout();
  return router.push('/login');
}

function handleProfileCommand(command) {
  if (command === 'profile') {
    return router.push('/admin/profile');
  }

  if (command === 'password') {
    return router.push('/admin/profile?tab=password');
  }

  if (command === 'logout') {
    return handleLogout();
  }
}

defineExpose({
  handleProfileCommand
});
</script>
