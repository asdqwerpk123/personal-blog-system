# personal-blog-system API Doc

## Runtime Docs

- OpenAPI JSON: `http://localhost:8081/v3/api-docs`
- Swagger UI: `http://localhost:8081/swagger-ui/index.html`

## Manual Endpoint List

### P0

- `GET /admin/user/{id}`
- `GET /admin/user/page`
- `POST /admin/user`
- `PUT /admin/user/{id}`
- `PUT /admin/user/{id}/status`
- `PUT /admin/user/{id}/password/reset`
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
- `GET /admin/dashboard/summary`

## Key Endpoint Details

### Login

- `POST /admin/auth/login`
- Public endpoint.
- Request: `userName`, `password`
- Response data: sanitized `user`, `accessToken`, `tokenType`, `expiresAt`
- Only `SUPER_ADMIN` and `ADMIN` accounts can log in to the admin backend. Ordinary `USER` accounts receive `401`.
- Successful login records an `AUTH / LOGIN / SUCCESS` operation log.
- Failed login for an existing user records an `AUTH / LOGIN / FAILURE` operation log.

### User Management

- `POST /admin/user`
  - Creates a backend user.
  - Request fields: `userName`, `password`, `nickName`, `roleId`, `email`, `phone`, `userStatus`
  - `SUPER_ADMIN` can create `ADMIN` and `USER`; `ADMIN` can create `USER`; creating `SUPER_ADMIN` is not allowed.
- `PUT /admin/user/{id}`
  - Updates user profile and role assignment.
  - Request fields: `nickName`, `roleId`, `email`, `phone`, `userStatus`
  - `SUPER_ADMIN` can manage `ADMIN` and `USER`; `ADMIN` can manage `USER`.
- `PUT /admin/user/{id}/status`
  - Request field: `userStatus` (`ENABLED` or `DISABLED`)
  - Disabling the current user or a `SUPER_ADMIN` is not allowed.
- `PUT /admin/user/{id}/password/reset`
  - Request field: `newPassword`
  - Uses the same role visibility rules as user update.

### Dashboard

- `GET /admin/dashboard/summary`
- Requires backend admin token.
- Response data:
  - Counts: `articleCount`, `categoryCount`, `tagCount`, `commentCount`, `pendingCommentCount`, `friendLinkCount`
  - Lists: `latestArticles`, `latestComments`, `latestLogs`

### Operation Logs

- `GET /admin/operation-log/page`
- Query fields: `current`, `size`, `operatorUserId`, `targetType`, `actionResult`
- The system now records key backend actions automatically:
  - login success/failure for existing users
  - user create/update/status/password reset
  - article create/update/status/tag assignment
  - category create/update/delete
  - tag create/update/delete
  - comment status update
  - friend link create/update/delete

## Notes

- Unified response wrapper: `Result`
- Management endpoints under `/admin/**` require `Authorization: Bearer <accessToken>` by default
- Backend management endpoints are limited to `SUPER_ADMIN` and `ADMIN`; ordinary `USER` accounts are rejected from `/admin/**`
- `POST /admin/auth/login` remains public and returns sanitized user info plus `accessToken`, `tokenType`, and `expiresAt`
- CORS `OPTIONS` preflight requests are allowed without a token
- Swagger/OpenAPI is the primary up-to-date reference
