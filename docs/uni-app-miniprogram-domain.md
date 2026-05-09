# uni-app 小程序域名配置说明

## 1. 小程序域名限制

小程序正式环境不能请求 `localhost`，也不能使用未配置的临时域名。正式环境必须满足：

- 使用 HTTPS 合法域名。
- 域名已在小程序后台配置。
- 证书有效且链路可信。
- 不使用自签名证书。

## 2. 需要配置的域名类型

| 域名类型 | 用途 | 示例 |
| --- | --- | --- |
| request 合法域名 | 调用文章、分类、标签、登录、评论、我的页面等 API | `https://api.example.com` |
| uploadFile 合法域名 | 上传头像 | `https://api.example.com` |
| downloadFile 合法域名 | 下载图片或文件 | `https://cdn.example.com` |
| 图片资源域名 | 展示封面、头像、友链 Logo | `https://cdn.example.com` 或 `https://api.example.com` |

如果 API 和图片都通过同一个 HTTPS 域名代理，也可以只维护一个统一域名，但需要确保 request、uploadFile、downloadFile、图片访问都在小程序后台正确配置。

## 3. 开发环境方案

开发阶段可以使用以下方式：

- 微信开发者工具中临时勾选“不校验合法域名、web-view 域名、TLS 版本以及 HTTPS 证书”。
- 使用局域网 IP 调试，例如 `http://192.168.1.100:8081`。
- 使用内网穿透工具暴露 HTTPS 测试域名。

注意：

- 这些方式只适合开发调试。
- 正式演示和提交审核建议使用 HTTPS 域名。
- 真机调试时，手机必须能访问后端所在网络地址。

## 4. 生产环境方案

推荐生产结构：

```text
小程序 -> https://api.example.com/api -> Nginx -> http://127.0.0.1:8081
图片 -> https://api.example.com/uploads/... 或 https://cdn.example.com/...
```

部署要点：

- API 域名可以使用 `https://api.example.com`。
- 前端 `baseURL` 可以配置为 `https://api.example.com/api`。
- Nginx `/api/` 代理到 Spring Boot。
- Nginx `/uploads/` 或 MinIO 图片域名必须公网可访问。
- 后端 Controller 路径仍保持 `/public/**`、`/user/**`、`/admin/**`。

## 5. baseURL 切换

| 环境 | baseURL 示例 | 说明 |
| --- | --- | --- |
| 本地 H5 开发 | `http://localhost:8081` | 仅适合浏览器本机预览 |
| 真机局域网调试 | `http://局域网IP:8081` | 手机与电脑需要在同一网络 |
| 生产或正式演示 | `https://域名/api` | 推荐使用 HTTPS 域名和 Nginx 代理 |

当前配置位置：

```text
personal-blog-uniapp/utils/config.js
```

## 6. 注意事项

- 证书必须有效，不能使用自签名证书。
- 小程序正式环境通常不能使用非标准或未备案场景下的临时端口。
- API 域名必须加入 request 合法域名。
- 头像上传接口所在域名必须加入 uploadFile 合法域名。
- 图片资源域名必须能被小程序访问。
- 如果后端返回 MinIO 绝对 URL，MinIO 域名也需要加入图片相关合法域名。
- 如果后端返回 `/uploads/...` 相对路径，前端会通过 `resolveAssetUrl(url)` 拼接 `baseURL`，需要确认拼接后的域名也已配置合法域名。
