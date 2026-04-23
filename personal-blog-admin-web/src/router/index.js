import { createRouter, createWebHistory } from 'vue-router';

import AdminLayout from '@/layouts/AdminLayout.vue';
import { useAuthStore } from '@/stores/auth.js';
import ArticlesView from '@/views/ArticlesView.vue';
import CategoriesView from '@/views/CategoriesView.vue';
import DashboardView from '@/views/DashboardView.vue';
import LoginView from '@/views/LoginView.vue';
import PlaceholderView from '@/views/PlaceholderView.vue';
import { adminMenuRoutes } from './adminMenu.js';

const adminRouteComponents = {
  articles: ArticlesView,
  categories: CategoriesView,
  dashboard: DashboardView
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
      }))
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
