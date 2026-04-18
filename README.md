# Personal Blog System

## 项目简介

`personal-blog-system` 是一个基于 `Java 17`、`Spring Boot 4.0.3` 和 `Maven` 构建的多模块个人博客系统后端项目。

当前仓库已经完成了课程设计可演示的后台 MVP：

1. Maven 父子模块结构搭建
2. MySQL 数据库设计与后端基础设施接入
3. P0 核心管理接口与 OpenAPI 文档
4. P1 标签、文章标签关联、评论管理
5. P2 友情链接、操作日志分页、最小登录接口

项目适合作为 `Web 应用实训`、课程设计、Spring Boot 入门练习以及后续博客系统功能扩展的基础工程。

## 技术栈

- `Java 17`
- `Spring Boot 4.0.3`
- `Maven`
- `MyBatis-Plus 3.5.16`
- `Druid Spring Boot 4 Starter 1.2.28`
- `MySQL 8.x`
- `JUnit 5`
- `Navicat`
- `IntelliJ IDEA`

## 项目结构

```text
personal-blog-system
├─ personal-blog-admin
│  ├─ src/main/java
│  ├─ src/main/resources
│  ├─ src/test/java
│  └─ pom.xml
├─ personal-blog-common
│  ├─ src/main/java
│  ├─ src/test/java
│  └─ pom.xml
├─ sql
│  └─ personal_blog_system.sql
├─ docs
├─ pom.xml
├─ mvnw
└─ mvnw.cmd
```

### 模块说明

#### `personal-blog-system`

父工程模块，负责统一管理版本、Java 配置和聚合子模块。

#### `personal-blog-admin`

后台主模块，当前包含：

- Spring Boot 启动类
- 数据源与多环境配置
- MyBatis-Plus、Druid 集成
- 控制器、持久层、测试代码

#### `personal-blog-common`

公共模块，当前包含：

- 统一返回结果 `Result`
- 错误码枚举 `ResultCodeEnum`
- 自定义业务异常 `BlogException`
- 全局异常处理 `GlobalExceptionHandler`

#### `sql`

数据库脚本目录，包含库表结构、初始化数据、触发器和存储过程。

## 当前进度

目前仓库已经完成的内容如下：

### 1. 多模块父子工程结构

- 根工程 `pom.xml` 已改为 `pom` 打包方式
- 已声明两个子模块：
  - `personal-blog-admin`
  - `personal-blog-common`
- `personal-blog-admin` 已依赖 `personal-blog-common`

### 2. 后端基础设施

- 已实现统一返回结果、错误码枚举、自定义异常和全局异常处理
- 已整合 `MyBatis-Plus`
- 已整合 `Druid` 数据源与监控
- 已完成 `dev / test / prod` 多环境数据源配置

### 3. 后台接口能力

当前已经完成的后台接口包括：

- 用户：`GET /admin/user/{id}`、`GET /admin/user/page`
- 角色：`GET /admin/role/{id}`、`GET /admin/role/list`
- 分类：`GET /admin/category/{id}`、`GET /admin/category/list`、`GET /admin/category/page`、`POST /admin/category`、`PUT /admin/category/{id}`、`DELETE /admin/category/{id}`
- 文章：`GET /admin/article/{id}`、`GET /admin/article/page`、`POST /admin/article`、`PUT /admin/article/{id}`、`PUT /admin/article/{id}/status`、`DELETE /admin/article/{id}`
- 标签：`GET /admin/tag/page`、`POST /admin/tag`、`PUT /admin/tag/{id}`、`DELETE /admin/tag/{id}`
- 文章标签：`GET /admin/article/{id}/tags`、`PUT /admin/article/{id}/tags`
- 评论：`GET /admin/comment/page`、`GET /admin/comment/article/{articleId}`、`PUT /admin/comment/{id}/status`、`DELETE /admin/comment/{id}`
- 友情链接：`GET /admin/friend-link/page`、`POST /admin/friend-link`、`PUT /admin/friend-link/{id}`、`DELETE /admin/friend-link/{id}`
- 操作日志：`GET /admin/operation-log/page`
- 最小登录：`POST /admin/auth/login`

### 4. 文档与测试验证

当前已经验证通过：

- 数据库连接测试
- `Mapper` / `Service` / `Controller` 主要用例测试
- OpenAPI 运行时文档：`/v3/api-docs`
- Swagger UI：`/swagger-ui/index.html`
- 整仓 `.\scripts\test-with-test-db.ps1`
- Druid 监控页访问验证

## 快速开始

### 环境要求

建议开发环境如下：

- `JDK 17`
- `Maven 3.9+`
- `MySQL 8.x`
- `Navicat`
- `IntelliJ IDEA`

### 1. 导入项目

使用 IntelliJ IDEA 打开项目根目录：

```text
D:\TOMCAT\webapps\personal-blog-system
```

然后以 Maven 项目方式导入。

### 2. 加载 Maven 依赖

在项目根目录执行：

```bash
./mvnw.cmd clean test
```

或者在 IDEA 中点击 Maven Reload。

### 3. 初始化数据库

数据库名称：

```sql
personal_blog_system
```

执行脚本：

```text
sql/personal_blog_system.sql
```

如果使用 Navicat，可直接打开 SQL 文件后执行全部脚本。

### 4. 配置数据源

`personal-blog-admin` 使用以下配置文件：

- `application.properties`
- `application-dev.yml`
- `application-test.yml`
- `application-prod.yml`

默认支持以下环境变量：

- `BLOG_DB_URL`
- `BLOG_DB_USERNAME`
- `BLOG_DB_PASSWORD`
- `BLOG_DB_TEST_URL`
- `BLOG_DB_TEST_USERNAME`
- `BLOG_DB_TEST_PASSWORD`
- `BLOG_DB_PROD_URL`
- `BLOG_DB_PROD_USERNAME`
- `BLOG_DB_PROD_PASSWORD`
- `BLOG_SERVER_PORT`

本地默认开发环境配置等价于：

```text
jdbc:mysql://localhost:3306/personal_blog_system?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false
username=root
password=123456
```

开发启动时请显式指定 `dev` profile，避免依赖任何隐式默认值：

```bash
./mvnw.cmd -pl personal-blog-admin spring-boot:run -Dspring-boot.run.profiles=dev
```

`test` profile 使用独立测试库契约，不再继承或回退到 `dev` 数据源配置。不要修改 `application-test.yml` 的 fail-fast 行为，也不要把测试回退到开发库。

第一次准备测试环境时，请先运行：

```powershell
.\scripts\setup-test-env.ps1
```

以后统一通过下面的脚本跑测试：

```powershell
.\scripts\test-with-test-db.ps1
```

详细初始化步骤、脚本说明、PowerShell / IDEA 注意事项和常见报错说明见 [docs/test-environment.md](docs/test-environment.md)。

### 5. 启动项目

项目启动类：

```text
org.example.personalblogsystem.PersonalBlogSystemApplication
```

在项目根目录执行：

```bash
./mvnw.cmd -pl personal-blog-admin spring-boot:run -Dspring-boot.run.profiles=dev
```

项目默认启动端口为 `8081`。如果后续需要临时切换端口，可通过环境变量 `BLOG_SERVER_PORT` 覆盖。

### 6. 运行测试

第一次准备测试环境：

```powershell
.\scripts\setup-test-env.ps1
```

以后统一通过脚本运行整仓测试：

```powershell
.\scripts\test-with-test-db.ps1
```

如果缺少任一 `BLOG_DB_TEST_*` 变量，测试仍会在 Spring Boot 启动阶段按设计快速失败；新脚本的作用是固定入口并提前把测试库环境变量装载到当前进程，而不是回退到开发库。

构建整个父子模块项目：

```bash
./mvnw.cmd clean package
```

## 数据库设计说明

### 核心业务表

项目数据库目前已设计以下核心表：

- `sys_role`：角色表
- `sys_user`：用户表
- `blog_category`：文章分类表
- `blog_tag`：文章标签表
- `blog_article`：文章表
- `blog_article_tag`：文章与标签关联表
- `blog_comment`：评论表
- `blog_friend_link`：友情链接表
- `sys_operation_log`：系统操作日志表

### 角色设计

系统当前设计了 3 种角色：

- `SUPER_ADMIN`：超级管理员
- `ADMIN`：管理员
- `USER`：普通用户

### 权限与约束设计

数据库层已经加入了比较明确的权限控制规则：

- 超级管理员不能被删除
- 管理员只能删除普通用户，不能删除其他管理员
- 普通用户只能删除自己的账号
- 普通用户只能删除自己的文章
- 普通用户只能删除自己的评论

### 触发器与存储过程

SQL 文件中包含多个触发器和存储过程，主要作用有：

- 阻止对关键表执行物理删除
- 在文章状态为已发布时自动补全发布时间
- 通过逻辑删除存储过程校验操作者权限

当前触发器主要针对：

- `sys_user`
- `blog_article`
- `blog_comment`

当前存储过程主要包括：

- `sp_delete_user`
- `sp_delete_article`
- `sp_delete_comment`

## 后端基础能力

### `personal-blog-common`

已实现：

- `Result`
- `ResultCodeEnum`
- `BlogException`
- `GlobalExceptionHandler`

### `personal-blog-admin`

已实现：

- MyBatis-Plus 接入
- Druid 数据源与监控接入
- OpenAPI / Swagger 文档
- 用户、角色、分类、文章后台接口
- 标签、文章标签、评论后台接口
- 友情链接、操作日志分页、最小登录接口
- 数据库连接与 CRUD / 接口测试

## Druid 监控页

启动 `personal-blog-admin` 后访问：

```text
http://localhost:8081/druid
```

默认开发环境账号：

```text
username: admin
password: admin123
```

当前项目已经验证 `/druid` 入口可以正常跳转到监控登录页。

## 当前实现范围

当前仓库已经覆盖以下业务表对应的后台代码：

- `sys_user`
- `sys_role`
- `blog_category`
- `blog_article`
- `blog_tag`
- `blog_article_tag`
- `blog_comment`
- `blog_friend_link`
- `sys_operation_log`

其中：

- P0 重点解决项目启动、统一返回、分页与 OpenAPI 文档
- P1 补齐标签、文章标签关联、评论管理
- P2 补齐友情链接、操作日志分页、最小登录接口
- P2 仍然没有引入 JWT、Spring Security、全局鉴权拦截

## 后续开发建议

后续可以继续补充：

- 登录后的鉴权拦截与权限控制
- 前台博客站点接口
- 更完整的参数校验与请求 DTO
- 操作日志的更多查询维度
- 友情链接审核流与展示页联调
- Vue 前端联调

## 作者说明

本项目用于 `Web 应用实训` 课程学习与个人博客系统课程设计实践。
