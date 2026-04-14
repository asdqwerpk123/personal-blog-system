# personal-blog-system API Doc

## Runtime Docs

- OpenAPI JSON: `http://localhost:8080/v3/api-docs`
- Swagger UI: `http://localhost:8080/swagger-ui/index.html`

## Manual Endpoint List

### P0

- `GET /admin/user/{id}`
- `GET /admin/user/page`
- `GET /admin/role/{id}`
- `GET /admin/role/list`
- `GET /admin/category/{id}`
- `GET /admin/category/list`
- `GET /admin/category/page`
- `POST /admin/category`
- `PUT /admin/category/{id}`
- `DELETE /admin/category/{id}`
- `GET /admin/article/{id}`
- `GET /admin/article/page`
- `POST /admin/article`
- `PUT /admin/article/{id}`
- `PUT /admin/article/{id}/status`
- `DELETE /admin/article/{id}`

### P1

- `GET /admin/tag/page`
- `POST /admin/tag`
- `PUT /admin/tag/{id}`
- `DELETE /admin/tag/{id}`
- `GET /admin/article/{id}/tags`
- `PUT /admin/article/{id}/tags`
- `GET /admin/comment/page`
- `GET /admin/comment/article/{articleId}`
- `PUT /admin/comment/{id}/status`
- `DELETE /admin/comment/{id}`

### P2

- `GET /admin/friend-link/page`
- `POST /admin/friend-link`
- `PUT /admin/friend-link/{id}`
- `DELETE /admin/friend-link/{id}`
- `GET /admin/operation-log/page`
- `POST /admin/auth/login`

## Notes

- Unified response wrapper: `Result`
- Swagger/OpenAPI is the primary up-to-date reference
- P2 login is intentionally minimal: it only validates credentials and returns user info, without JWT or global request interception
