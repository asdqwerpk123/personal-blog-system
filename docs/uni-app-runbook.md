# uni-app 第一版运行手册

## 1. 功能范围

当前 uni-app 第一版支持以下课程项目闭环功能：

- 游客浏览公开文章
- 首页搜索、分类筛选、标签筛选
- 文章详情查看
- 评论分页查看
- 普通用户注册
- 普通用户登录
- 登录用户发表评论
- 我的页面
- 修改个人资料
- 头像上传
- 我的评论分页查看

uni-app 面向游客和普通 `USER` 用户，不替代 Vue 后台管理端，也不直接暴露 `/admin/**` 给移动端或小程序端。

## 2. 后端启动方式

在仓库根目录执行：

```powershell
.\mvnw.cmd -pl personal-blog-admin spring-boot:run -Dspring-boot.run.profiles=dev
```

默认后端地址：

```text
http://localhost:8081
```

启动前需要确认：

- 本机 MySQL 已启动。
- 已导入 `sql/personal_blog_system.sql`，或数据库中已有可演示的文章、分类、标签、评论和用户数据。
- `personal-blog-admin/src/main/resources/application-dev.yml` 中的数据源配置与本机 MySQL 一致。
- 如果 Maven 本地仓库权限失败，需要检查用户目录下 `.m2` 的读写权限，或切换到有写权限的用户目录。
- 如果 8081 端口被占用，需要停止占用进程，或通过 `BLOG_SERVER_PORT` 等端口配置调整后端端口。

## 3. uni-app H5 预览

推荐使用 HBuilderX 预览：

1. 打开 HBuilderX。
2. 选择并打开 `personal-blog-uniapp` 目录。
3. 选择“运行到浏览器”。
4. 浏览器中查看 H5 预览效果。

当前开发默认 `baseURL`：

```text
http://localhost:8081
```

如果后端不运行在 8081 端口，需要修改：

```text
personal-blog-uniapp/utils/config.js
```

注意：

- 浏览器 H5 本地预览可以使用 `localhost`。
- 真机调试和小程序正式环境不能直接使用 `localhost`。
- 真机需要使用局域网 IP 或 HTTPS 域名。
- 小程序正式环境必须使用已配置的 HTTPS 合法域名。

## 4. 常用接口检查

后端启动后，可先检查公开接口：

| 接口 | 用途 |
| --- | --- |
| `GET /public/articles/page` | 公开文章分页 |
| `GET /public/categories/list` | 公开分类列表 |
| `GET /public/tags/list` | 公开标签列表 |
| `GET /public/friend-links/list` | 公开友链列表 |
| `GET /public/articles/{id}` | 公开文章详情 |
| `GET /public/articles/{id}/comments?current=1&size=10` | 文章评论分页 |

示例：

```text
http://localhost:8081/public/articles/page
http://localhost:8081/public/categories/list
http://localhost:8081/public/tags/list
http://localhost:8081/public/friend-links/list
```

如果文章列表有数据，再取第一篇文章 ID 检查：

```text
http://localhost:8081/public/articles/2
http://localhost:8081/public/articles/2/comments?current=1&size=10
```

## 5. 演示流程

建议按以下顺序演示：

1. 启动 MySQL。
2. 确认已导入 `sql/personal_blog_system.sql` 或已有测试数据。
3. 启动 Spring Boot 后端。
4. 访问 `/public/articles/page`，确认公开文章接口可用。
5. 打开 HBuilderX。
6. 用 HBuilderX 打开 `personal-blog-uniapp`。
7. 运行 uni-app 到浏览器。
8. 查看首页文章列表。
9. 使用 keyword 搜索文章。
10. 使用分类筛选文章。
11. 使用标签筛选文章。
12. 进入文章详情页。
13. 查看评论分页。
14. 注册普通用户。
15. 登录普通用户。
16. 在文章详情页发表评论。
17. 进入我的页面。
18. 修改个人资料。
19. 上传头像。
20. 查看我的评论。
21. 退出登录，确认回到未登录态。
