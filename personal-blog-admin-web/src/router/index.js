import { createRouter, createWebHistory } from 'vue-router';

import AdminLayout from '@/layouts/AdminLayout.vue';
import { useAuthStore } from '@/stores/auth.js';
import ArticlesView from '@/views/ArticlesView.vue';
import CategoriesView from '@/views/CategoriesView.vue';
import CommentModerationView from '@/views/admin/CommentModerationView.vue';
import FriendLinkManagementView from '@/views/admin/FriendLinkManagementView.vue';
import AdminProfileView from '@/views/admin/AdminProfileView.vue';
import OperationLogView from '@/views/admin/OperationLogView.vue';
import TagManagementView from '@/views/admin/TagManagementView.vue';
import UserManagementView from '@/views/admin/UserManagementView.vue';
import DashboardView from '@/views/DashboardView.vue';
import LoginView from '@/views/LoginView.vue';
import PlaceholderView from '@/views/PlaceholderView.vue';
import { adminMenuRoutes } from './adminMenu.js';

const adminRouteComponents = {
  articles: ArticlesView,
  categories: CategoriesView,
  comments: CommentModerationView,
  dashboard: DashboardView,
  friendLinks: FriendLinkManagementView,
  logs: OperationLogView,
  tags: TagManagementView,
  users: UserManagementView
};

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      redirect: '/admin/dashboard'
    },
    {
      path: '/login',
      name: 'login',
      component: LoginView,
      meta: { public: true }
    },
    {
      path: '/admin',
      component: AdminLayout,
      redirect: '/admin/dashboard',
      children: adminMenuRoutes.map((route) => ({
        path: route.path.replace('/admin/', ''),
        name: route.name,
        component: adminRouteComponents[route.name] || PlaceholderView,
        meta: {
          title: route.title,
          icon: route.icon
        }
      })).concat([
        {
          path: 'profile',
          name: 'profile',
          component: AdminProfileView,
          meta: {
            title: '个人资料',
            hidden: true
          }
        }
      ])
    },
    {
      path: '/:pathMatch(.*)*',
      redirect: '/admin/dashboard'
    }
  ],
  scrollBehavior() {
    return { top: 0 };
  }
});

router.beforeEach((to) => {
  const authStore = useAuthStore();

  if (to.meta.public && authStore.isAuthenticated) {
    return '/admin/dashboard';
  }

  if (!to.meta.public && !authStore.isAuthenticated) {
    return {
      path: '/login',
      query: {
        redirect: to.fullPath
      }
    };
  }

  return true;
});

export default router;
