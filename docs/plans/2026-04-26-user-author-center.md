# User Author Center Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在不破坏 `/admin/**` 管理员后台的前提下，为 `USER` 提供统一登录后的作者中心最小可用闭环。

**Architecture:** 保留现有管理员认证与后台路由，新增用户态 `/user/**` 接口和 `/author/**` 前端路由，统一复用现有 JWT、Axios、Pinia、Vue Router、MyBatis Plus、统一返回结构与操作日志能力。用户登录与注册走统一认证入口，前端按 `roleCode` 分流，后端对 `/admin/**` 与 `/user/**` 分别强制校验角色和资源归属。

**Tech Stack:** Spring Boot, MyBatis Plus, MySQL, Vue 3, Vite, JavaScript, Vue Router, Pinia, Axios, Element Plus

---

### Task 1: 用户态认证与权限骨架

**Files:**
- Modify: `personal-blog-admin/src/main/java/org/example/personalblogsystem/controller/AuthController.java`
- Modify: `personal-blog-admin/src/main/java/org/example/personalblogsystem/service/IAuthService.java`
- Modify: `personal-blog-admin/src/main/java/org/example/personalblogsystem/service/impl/AuthServiceImpl.java`
- Create: `personal-blog-admin/src/main/java/org/example/personalblogsystem/auth/UserAuthContext.java`
- Create: `personal-blog-admin/src/main/java/org/example/personalblogsystem/auth/UserAuthInterceptor.java`
- Create: `personal-blog-admin/src/main/java/org/example/personalblogsystem/config/UserAuthWebMvcConfig.java`
- Test: `personal-blog-admin/src/test/java/org/example/personalblogsystem/controller/AuthControllerTest.java`
- Test: `personal-blog-admin/src/test/java/org/example/personalblogsystem/auth/AdminAuthInterceptorTest.java`

- [ ] 新增统一登录允许 `SUPER_ADMIN`、`ADMIN`、`USER` 返回 token 与 `roleCode`
- [ ] 保持 `/admin/**` 继续只允许管理员角色访问
- [ ] 为 `/user/**` 建立用户态认证上下文与拦截器
- [ ] 新增普通用户注册接口并强制注册角色为 `USER`
- [ ] 覆盖注册、统一登录、USER 禁止访问 `/admin/**` 的测试

### Task 2: 用户态文章、评论、资料与仪表盘接口

**Files:**
- Create: `personal-blog-admin/src/main/java/org/example/personalblogsystem/controller/UserArticleController.java`
- Create: `personal-blog-admin/src/main/java/org/example/personalblogsystem/controller/UserCommentController.java`
- Create: `personal-blog-admin/src/main/java/org/example/personalblogsystem/controller/UserProfileController.java`
- Create: `personal-blog-admin/src/main/java/org/example/personalblogsystem/controller/UserFileController.java`
- Create: `personal-blog-admin/src/main/java/org/example/personalblogsystem/controller/UserDashboardController.java`
- Modify: `personal-blog-admin/src/main/java/org/example/personalblogsystem/service/IBlogArticleService.java`
- Modify: `personal-blog-admin/src/main/java/org/example/personalblogsystem/service/IBlogCommentService.java`
- Modify: `personal-blog-admin/src/main/java/org/example/personalblogsystem/service/ISysUserService.java`
- Modify: `personal-blog-admin/src/main/java/org/example/personalblogsystem/service/impl/BlogArticleServiceImpl.java`
- Modify: `personal-blog-admin/src/main/java/org/example/personalblogsystem/service/impl/BlogCommentServiceImpl.java`
- Modify: `personal-blog-admin/src/main/java/org/example/personalblogsystem/service/impl/SysUserServiceImpl.java`
- Modify: `personal-blog-admin/src/main/java/org/example/personalblogsystem/service/AvatarStorageService.java`
- Test: `personal-blog-admin/src/test/java/org/example/personalblogsystem/controller/UserArticleControllerTest.java`
- Test: `personal-blog-admin/src/test/java/org/example/personalblogsystem/controller/UserCommentControllerTest.java`
- Test: `personal-blog-admin/src/test/java/org/example/personalblogsystem/controller/UserProfileControllerTest.java`

- [ ] 新增只作用于当前用户的文章分页、详情、新增、编辑、状态变更、删除接口
- [ ] 新增文章详情评论列表与发表评论接口，评论默认 `PENDING`
- [ ] 新增我的评论分页与删除接口，并校验评论归属
- [ ] 新增作者首页统计接口，只统计当前用户内容
- [ ] 新增获取/修改个人资料、上传头像、修改密码接口
- [ ] 统一中文错误提示与关键操作日志记录
- [ ] 覆盖用户只能操作自己数据、头像上传、修改密码等测试

### Task 3: 前端统一登录、作者中心路由与页面

**Files:**
- Modify: `personal-blog-admin-web/src/router/index.js`
- Modify: `personal-blog-admin-web/src/stores/auth.js`
- Modify: `personal-blog-admin-web/src/api/auth.js`
- Modify: `personal-blog-admin-web/src/api/articles.js`
- Modify: `personal-blog-admin-web/src/api/comments.js`
- Modify: `personal-blog-admin-web/src/api/profile.js`
- Modify: `personal-blog-admin-web/src/api/dashboard.js`
- Modify: `personal-blog-admin-web/src/api/http.js`
- Modify: `personal-blog-admin-web/src/views/LoginView.vue`
- Create: `personal-blog-admin-web/src/views/RegisterView.vue`
- Create: `personal-blog-admin-web/src/layouts/AuthorLayout.vue`
- Create: `personal-blog-admin-web/src/router/authorMenu.js`
- Create: `personal-blog-admin-web/src/views/author/AuthorDashboardView.vue`
- Create: `personal-blog-admin-web/src/views/author/AuthorArticlesView.vue`
- Create: `personal-blog-admin-web/src/views/author/AuthorArticleEditorView.vue`
- Create: `personal-blog-admin-web/src/views/author/AuthorArticleDetailView.vue`
- Create: `personal-blog-admin-web/src/views/author/AuthorCommentsView.vue`
- Create: `personal-blog-admin-web/src/views/author/AuthorProfileView.vue`
- Modify: `personal-blog-admin-web/src/layouts/AdminLayout.vue`
- Modify: `personal-blog-admin-web/src/styles/index.css`
- Test: `personal-blog-admin-web/tests/router.test.js`
- Test: `personal-blog-admin-web/tests/loginView.test.js`
- Test: `personal-blog-admin-web/tests/adminLayoutProfile.test.js`
- Test: `personal-blog-admin-web/tests/adminProfileView.test.js`
- Create: `personal-blog-admin-web/tests/registerView.test.js`
- Create: `personal-blog-admin-web/tests/authorPages.test.js`

- [ ] 登录页保留统一视觉并增加注册入口，不新增第二套登录系统
- [ ] 登录成功按 `roleCode` 跳转 `/admin/dashboard` 或 `/author/dashboard`
- [ ] 新增 `/register` 与 `/author/**` 路由守卫
- [ ] 删除顶栏无用全局搜索框，同时保留业务页面内部筛选区
- [ ] 实现作者中心菜单、首页、文章列表、文章编辑/详情、评论、个人资料与密码弹窗
- [ ] 文章表单移除“封面 URL”，快捷操作与查看/编辑跳转真实可用
- [ ] 覆盖路由、登录跳转、注册入口、作者页面基本行为测试

### Task 4: 验证与手工验收

**Files:**
- Modify as needed: 测试文件与实现文件

- [ ] 运行 `./mvnw.cmd test`
- [ ] 运行 `npm test`
- [ ] 运行 `npm run build`
- [ ] 用真实浏览器核对登录、注册、作者中心、资料、评论与管理员后台回归
