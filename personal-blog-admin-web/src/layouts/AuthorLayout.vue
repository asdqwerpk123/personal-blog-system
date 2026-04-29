<template>
  <div class="author-shell">
    <aside class="author-sidebar">
      <RouterLink class="author-brand" to="/author/dashboard" aria-label="进入作者首页">
        <span class="author-brand__mark">
          <Notebook />
        </span>
        <span class="author-brand__text">博客后台</span>
      </RouterLink>

      <div class="author-card">
        <span class="author-avatar">
          <img v-if="authStore.avatarUrl" :src="authStore.avatarUrl" alt="头像" />
          <span v-else>{{ userInitial }}</span>
        </span>
        <div>
          <strong>{{ displayName }}</strong>
          <p>普通作者</p>
        </div>
      </div>

      <el-menu
        class="author-menu"
        :default-active="route.path"
        background-color="transparent"
        text-color="#b8c2d6"
        active-text-color="#ffffff"
        router
      >
        <el-menu-item v-for="item in authorMenuRoutes" :key="item.path" :index="item.path">
          <el-icon>
            <component :is="iconMap[item.icon]" />
          </el-icon>
          <span>{{ item.title }}</span>
        </el-menu-item>
      </el-menu>

      <button class="author-logout" type="button" @click="handleLogout">
        <el-icon><SwitchButton /></el-icon>
        <span>退出登录</span>
      </button>
    </aside>

    <section class="author-main">
      <header class="author-topbar">
        <div class="author-topbar__spacer" aria-hidden="true"></div>
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

      <main class="author-content">
        <RouterView />
      </main>
    </section>
  </div>
</template>

<script setup>
import {
  ArrowDown,
  ChatLineSquare,
  Document,
  House,
  Notebook,
  SwitchButton,
  User
} from '@element-plus/icons-vue';
import { computed } from 'vue';
import { RouterLink, RouterView, useRoute, useRouter } from 'vue-router';

import { authorMenuRoutes } from '@/router/authorMenu.js';
import { useAuthStore } from '@/stores/auth.js';

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();

const iconMap = {
  ChatLineSquare,
  Document,
  House,
  User
};

const displayName = computed(() => authStore.nickName || authStore.userName || '作者');
const userInitial = computed(() => displayName.value.slice(0, 1).toUpperCase());

function handleLogout() {
  authStore.logout();
  return router.push('/login');
}

function handleProfileCommand(command) {
  if (command === 'profile') {
    return router.push('/author/profile');
  }

  if (command === 'password') {
    return router.push('/author/profile?tab=password');
  }

  if (command === 'logout') {
    return handleLogout();
  }
}
</script>

<style scoped>
.author-shell {
  min-height: 100vh;
  display: flex;
  background: var(--color-bg);
}

.author-sidebar {
  width: 250px;
  min-height: 100vh;
  position: sticky;
  top: 0;
  display: flex;
  flex: 0 0 250px;
  flex-direction: column;
  background: #1d293d;
}

.author-brand {
  height: 76px;
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 0 24px;
  color: #ffffff;
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
}

.author-brand__mark {
  width: 36px;
  height: 36px;
  display: grid;
  place-items: center;
  border-radius: 12px;
  color: #ffffff;
  background: #4774ff;
}

.author-brand__text {
  font-size: 21px;
  font-weight: 800;
}

.author-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 22px 24px;
  color: #ffffff;
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
}

.author-card strong {
  display: block;
  font-size: 16px;
}

.author-card p {
  margin: 4px 0 0;
  color: #b8c2d6;
  font-size: 13px;
}

.author-avatar {
  width: 44px;
  height: 44px;
  display: grid;
  place-items: center;
  overflow: hidden;
  flex: 0 0 44px;
  border-radius: 999px;
  color: #ffffff;
  font-weight: 800;
  background: #5966ff;
}

.author-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.author-menu {
  flex: 1;
  padding: 18px 14px;
  border-right: 0;
}

.author-menu :deep(.el-menu-item) {
  height: 46px;
  margin-bottom: 8px;
  border-radius: 10px;
}

.author-menu :deep(.el-menu-item.is-active) {
  background: var(--color-primary);
}

.author-menu :deep(.el-menu-item:hover) {
  background: rgba(255, 255, 255, 0.08);
}

.author-menu :deep(.el-menu-item.is-active:hover) {
  background: var(--color-primary);
}

.author-logout {
  height: 68px;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 0 28px;
  border: 0;
  border-top: 1px solid rgba(255, 255, 255, 0.06);
  color: #c8d1e2;
  background: transparent;
  cursor: pointer;
}

.author-logout:hover {
  color: #ffffff;
}

.author-main {
  min-width: 0;
  flex: 1;
}

.author-topbar {
  height: 72px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 26px;
  background: #ffffff;
  border-bottom: 1px solid var(--color-border);
}

.author-topbar__spacer {
  min-width: 1px;
}

.author-content {
  min-width: 0;
  padding: 28px;
}

@media (max-width: 760px) {
  .author-shell {
    display: block;
  }

  .author-sidebar {
    width: 100%;
    min-height: auto;
    position: static;
  }

  .author-menu {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .author-topbar {
    padding: 0 16px;
  }

  .author-content {
    padding: 18px;
  }
}
</style>
