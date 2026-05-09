# uni-app H5 Nginx 部署说明

## 1. 部署目标

生产环境建议由 Nginx 统一承载前端静态资源和接口代理：

- uni-app H5 构建产物由 Nginx 托管。
- `/api/` 反向代理到 Spring Boot 后端。
- `/uploads/` 静态资源或后端上传资源可通过公网访问。

## 2. 关键原则

- 后端 Controller 路径保持不变。
- 后端仍然使用 `/public/**`、`/user/**`、`/admin/**`。
- 前端生产 `baseURL` 可以配置为 `https://域名/api`。
- Nginx 负责去掉 `/api/` 前缀后转发到 Spring Boot。
- 不要为了部署代理去修改后端 Controller 路径。

## 3. 示例转发关系

| 浏览器请求 | Nginx 转发目标 |
| --- | --- |
| `https://域名/api/public/articles/page` | `http://127.0.0.1:8081/public/articles/page` |
| `https://域名/api/user/auth/login` | `http://127.0.0.1:8081/user/auth/login` |
| `https://域名/uploads/...` | 后端 `/uploads/...` 或服务器静态目录 |

## 4. 示例 Nginx 配置

以下配置用于参考，实际路径需要按服务器目录调整：

```nginx
server {
    listen 80;
    server_name example.com;

    root /var/www/personal-blog-uniapp/h5;
    index index.html;

    location / {
        try_files $uri $uri/ /index.html;
    }

    location /api/ {
        proxy_pass http://127.0.0.1:8081/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    location /uploads/ {
        proxy_pass http://127.0.0.1:8081/uploads/;
    }
}
```

如果上传目录直接挂在服务器文件系统，也可以用 `alias` 或 `root` 暴露静态目录，例如：

```nginx
location /uploads/ {
    alias /opt/personal-blog-system/uploads/;
}
```

使用静态目录方式时，需要确认后端实际写入目录与 Nginx 暴露目录一致。

## 5. H5 刷新 404 处理

H5 路由刷新时需要回退到 `index.html`：

```nginx
location / {
    try_files $uri $uri/ /index.html;
}
```

否则直接刷新文章详情、登录页、我的页面等前端路由时，Nginx 可能返回 404。

## 6. CORS 说明

- 如果 H5 页面和 API 都通过同一个域名访问，例如 `https://域名` 和 `https://域名/api`，通常可以减少 CORS 问题。
- 如果前端和后端使用不同域名，需要在后端跨域配置中允许前端域名。
- 生产环境不要使用过宽的跨域配置，建议只配置真实前端域名。

## 7. MinIO 与图片访问

- 如果后端返回 MinIO 绝对 URL，该 URL 必须能被浏览器、真机和小程序访问。
- 如果通过 Nginx 代理 MinIO，需要保持图片 URL 稳定，避免前端缓存或历史数据失效。
- MinIO bucket 权限和 CORS 需要正确配置，否则封面、头像、友链 Logo 可能无法加载。
- 如果后端返回 `/uploads/...` 相对路径，uni-app 会在 `utils/config.js` 中按 `baseURL` 补全资源地址。
