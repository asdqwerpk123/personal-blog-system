# uni-app H5 打包与部署说明

## 1. HBuilderX 打包 H5 步骤

1. 打开 HBuilderX。
2. 打开仓库中的 `personal-blog-uniapp` 目录。
3. 点击“发行”。
4. 选择“网站-H5手机版”。
5. 设置站点标题和发行路径。
6. 开始发行。
7. 在 HBuilderX 输出目录中找到 H5 构建产物，常见路径为 `dist/build/h5`，以 HBuilderX 实际输出为准。

不要使用普通 Vite Vue 项目替代 uni-app。第一版客户端必须保留 `manifest.json` 和 `pages.json`。

## 2. 生产 baseURL 配置

开发默认地址：

```text
http://localhost:8081
```

生产建议地址：

```text
https://域名/api
```

当前配置位置：

```text
personal-blog-uniapp/utils/config.js
```

后续如果需要更完整的环境切换，可以再扩展为按 HBuilderX 发行环境或构建变量读取配置。本阶段不新增复杂工程化。

## 3. 部署到 Nginx

部署步骤：

1. 将 H5 构建产物复制到 Nginx 站点目录，例如 `/var/www/personal-blog-uniapp/h5`。
2. 配置站点根目录指向该目录。
3. 配置 `/index.html` 刷新回退，避免 H5 页面刷新 404。
4. 配置 `/api/` 代理到 Spring Boot。
5. 配置 `/uploads/` 或 MinIO 图片访问。
6. 重载 Nginx。

核心配置可参考 `docs/uni-app-nginx-deploy.md`。

## 4. 部署后验收

| 验收项 | 预期结果 |
| --- | --- |
| 首页可打开 | H5 首页正常渲染 |
| 首页刷新不 404 | 刷新后仍回到 H5 应用 |
| 文章详情可打开 | 详情页可展示文章内容 |
| 分类筛选可用 | 分类切换后文章列表刷新 |
| 标签筛选可用 | 标签切换后文章列表刷新 |
| 登录注册可用 | 普通用户可登录、注册 |
| 头像上传可用 | 上传请求到达后端，头像 URL 可展示 |
| 图片可显示 | 封面、头像、友链 Logo 正常加载 |

## 5. 常见问题

| 问题 | 常见原因 | 处理建议 |
| --- | --- | --- |
| 打开空白页 | H5 产物路径错误或静态资源未复制完整 | 检查 Nginx `root` 和构建产物目录 |
| 刷新 404 | 未配置 H5 路由回退 | 配置 `try_files $uri $uri/ /index.html` |
| API 请求 404 | `baseURL` 或 `/api/` 代理错误 | 检查 `utils/config.js` 和 Nginx `location /api/` |
| API 跨域 | 前后端不同域且后端未允许来源 | 使用同域代理或配置后端 allowed origins |
| 图片不显示 | 图片 URL 不可访问、MinIO 权限或 `/uploads/` 映射错误 | 检查浏览器 Network、Nginx 图片路径、MinIO bucket 权限 |
| 头像上传失败 | `uploadFile` 域名、鉴权、文件大小或后端上传路径异常 | 检查请求头 Authorization、后端日志和上传目录权限 |
