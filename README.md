# Personal Blog System

## 项目简介

`personal-blog-system` 是一个基于 `Java 17`、`Spring Boot 4.0.3` 和 `Maven` 构建的多模块个人博客系统后端项目。

当前仓库已经完成了两部分基础建设：

1. Maven 父子模块结构搭建
2. MySQL 数据库设计与后端基础设施接入

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

### 3. 代码生成

已基于 MyBatis-Plus 代码生成器生成以下核心表对应代码：

- `sys_user`
- `sys_role`
- `blog_article`
- `blog_category`

### 4. 测试验证

当前已经验证通过：

- 数据库连接测试
- `Mapper` 基础 CRUD 测试
- `Controller` 返回结构测试
- 整仓 `./mvnw.cmd test`
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

本地默认开发环境配置等价于：

```text
jdbc:mysql://localhost:3306/personal_blog_system?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false
username=root
password=123456
```

### 5. 启动项目

项目启动类：

```text
org.example.personalblogsystem.PersonalBlogSystemApplication
```

在项目根目录执行：

```bash
./mvnw.cmd -pl personal-blog-admin spring-boot:run
```

### 6. 运行测试

运行整仓测试：

```bash
./mvnw.cmd test
```

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
- 核心表代码生成
- 基础控制器、Mapper、Service 骨架
- 数据库连接与 CRUD 测试

## Druid 监控页

启动 `personal-blog-admin` 后访问：

```text
http://localhost:8080/druid
```

默认开发环境账号：

```text
username: admin
password: admin123
```

当前项目已经验证 `/druid` 入口可以正常跳转到监控登录页。

## 当前代码生成范围

本阶段仅生成并整理以下四张核心表对应代码：

- `sys_user`
- `sys_role`
- `blog_article`
- `blog_category`

暂未生成：

- `blog_tag`
- `blog_comment`
- `blog_friend_link`
- `sys_operation_log`

## 后续开发建议

后续可以继续补充：

- 登录注册与权限校验
- 文章、分类、标签、评论接口
- 管理端分页查询
- 统一参数校验
- Swagger / OpenAPI 文档
- Vue 前端联调

## 作者说明

本项目用于 `Web 应用实训` 课程学习与个人博客系统课程设计实践。
