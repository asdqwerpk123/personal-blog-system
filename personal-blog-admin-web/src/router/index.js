import { createRouter, createWebHistory } from 'vue-router';

import AdminLayout from '@/layouts/AdminLayout.vue';
import AuthorLayout from '@/layouts/AuthorLayout.vue';
import { useAuthStore } from '@/stores/auth.js';
import ArticlesView from '@/views/ArticlesView.vue';
import CategoriesView from '@/views/CategoriesView.vue';
import CommentModerationView from '@/views/admin/CommentModerationView.vue';
import FriendLinkManagementView from '@/views/admin/FriendLinkManagementView.vue';
import AdminProfileView from '@/views/admin/AdminProfileView.vue';
import OperationLogView from '@/views/admin/OperationLogView.vue';
import TagManagementView from '@/views/admin/TagManagementView.vue';
import UserManagementView from '@/views/admin/UserManagementView.vue';
import AuthorArticleDetailView from '@/views/author/AuthorArticleDetailView.vue';
import AuthorArticleEditorView from '@/views/author/AuthorArticleEditorView.vue';
import AuthorArticlesView from '@/views/author/AuthorArticlesView.vue';
import AuthorCommentsView from '@/views/author/AuthorCommentsView.vue';
import AuthorDashboardView from '@/views/author/AuthorDashboardView.vue';
import AuthorProfileView from '@/views/author/AuthorProfileView.vue';
import DashboardView from '@/views/DashboardView.vue';
import LoginView from '@/views/LoginView.vue';
import PlaceholderView from '@/views/PlaceholderView.vue';
import RegisterView from '@/views/RegisterView.vue';
import { adminMenuRoutes } from './adminMenu.js';

const ADMIN_ROLES = ['SUPER_ADMIN', 'ADMIN'];

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

function roleHome(roleCode) {
  return roleCode === 'USER' ? '/author/dashboard' : '/admin/dashboard';
}

function isAdminRole(roleCode) {
  return ADMIN_ROLES.includes(roleCode);
}

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      redirect: '/login'
    },
    {
      path: '/login',
      name: 'login',
      component: LoginView,
      meta: { public: true }
    },
    {
      path: '/register',
      name: 'register',
      component: RegisterView,
      meta: { public: true }
    },
    {
      path: '/admin',
      component: AdminLayout,
      redirect: '/admin/dashboard',
      meta: { roles: ADMIN_ROLES },
      children: adminMenuRoutes.map((route) => ({
        path: route.path.replace('/admin/', ''),
        name: route.name,
        component: adminRouteComponents[route.name] || PlaceholderView,
        meta: {
          title: route.title,
          icon: route.icon,
          roles: ADMIN_ROLES
        }
      })).concat([
        {
          path: 'profile',
          name: 'profile',
          component: AdminProfileView,
          meta: {
            title: '个人资料',
            hidden: true,
            roles: ADMIN_ROLES
          }
        },
        {
          path: ':pathMatch(.*)*',
          redirect: '/admin/dashboard'
        }
      ])
    },
    {
      path: '/author',
      component: AuthorLayout,
      redirect: '/author/dashboard',
      meta: { roles: ['USER'] },
      children: [
        {
          path: 'dashboard',
          name: 'authorDashboard',
          component: AuthorDashboardView,
          meta: { title: '作者首页', roles: ['USER'] }
        },
        {
          path: 'articles',
          name: 'authorArticles',
          component: AuthorArticlesView,
          meta: { title: '我的文章', roles: ['USER'] }
        },
        {
          path: 'articles/new',
          name: 'authorArticleNew',
          component: AuthorArticleEditorView,
          meta: { title: '新建文章', roles: ['USER'] }
        },
        {
          path: 'articles/edit/:id',
          name: 'authorArticleEdit',
          component: AuthorArticleEditorView,
          meta: { title: '编辑文章', roles: ['USER'] }
        },
        {
          path: 'articles/detail/:id',
          name: 'authorArticleDetail',
          component: AuthorArticleDetailView,
          meta: { title: '文章详情', roles: ['USER'] }
        },
        {
          path: 'comments',
          name: 'authorComments',
          component: AuthorCommentsView,
          meta: { title: '我的评论', roles: ['USER'] }
        },
        {
          path: 'profile',
          name: 'authorProfile',
          component: AuthorProfileView,
          meta: { title: '个人资料', roles: ['USER'] }
        }
      ]
    },
    {
      path: '/:pathMatch(.*)*',
      redirect: '/login'
    }
  ],
  scrollBehavior() {
    return { top: 0 };
  }
});

router.beforeEach((to) => {
  const authStore = useAuthStore();
  const requiredRoles = to.matched.flatMap((record) => record.meta.roles || []);

  if (to.meta.public && authStore.isAuthenticated) {
    return roleHome(authStore.roleCode);
  }

  if (!to.meta.public && !authStore.isAuthenticated) {
    return {
      path: '/login',
      query: {
        redirect: to.fullPath
      }
    };
  }

  if (requiredRoles.length > 0 && !requiredRoles.includes(authStore.roleCode)) {
    return isAdminRole(authStore.roleCode) ? '/admin/dashboard' : '/author/dashboard';
  }

  return true;
});

export default router;
