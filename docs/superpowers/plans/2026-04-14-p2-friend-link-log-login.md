# P2 Friend Link, Operation Log, and Minimal Login Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add the P2 scope for `blog_friend_link`, `sys_operation_log`, and a minimal `POST /admin/auth/login` endpoint on top of the current personal-blog-system codebase without introducing global authentication or breaking completed P0/P1 APIs.

**Architecture:** Keep the existing Spring Boot + MyBatis-Plus layered style. Implement `friend-link` as a normal admin CRUD module, `operation-log` as a read-only paging module, and `auth/login` as a thin credential-check endpoint that returns sanitized user information only. Do not add JWT, Spring Security, servlet filters, interceptors, or request guards in this phase.

**Tech Stack:** Java 17, Spring Boot 4.0.3, Maven multi-module, MyBatis-Plus, MySQL 8, Druid, JUnit 5, MockMvc

---

### Task 1: Lock the P2 baseline and add failing tests

**Files:**
- Modify: `personal-blog-admin/src/test/java/org/example/personalblogsystem/controller/OpenApiDocumentationTest.java`
- Create: `personal-blog-admin/src/test/java/org/example/personalblogsystem/controller/BlogFriendLinkControllerTest.java`
- Create: `personal-blog-admin/src/test/java/org/example/personalblogsystem/controller/SysOperationLogControllerTest.java`
- Create: `personal-blog-admin/src/test/java/org/example/personalblogsystem/controller/AuthControllerTest.java`
- Test: `./mvnw.cmd -pl personal-blog-admin "-Dtest=BlogFriendLinkControllerTest,SysOperationLogControllerTest,AuthControllerTest,OpenApiDocumentationTest" test`

- [ ] **Step 1: Write the failing Friend Link controller tests**

Use the same test style as `BlogCategoryControllerTest` and `BlogArticleControllerTest`: `@SpringBootTest`, `@Transactional`, `MockMvcBuilders.webAppContextSetup(...)`, JSON assertions on the unified `Result` shape.

Add tests that describe the exact P2 friend-link behavior:

```java
@Test
void shouldReturnPagedFriendLinks() throws Exception {
    mockMvc.perform(get("/admin/friend-link/page")
                    .param("current", "1")
                    .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.records.length()").value(1));
}

@Test
void shouldCreateUpdateAndDeleteFriendLink() throws Exception {
    BlogFriendLink request = new BlogFriendLink();
    request.setSiteName("Temp Link");
    request.setSiteUrl("https://example.org/" + UUID.randomUUID());
    request.setCreatedBy(2L);

    JsonNode created = performJson(post("/admin/friend-link")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)));

    long id = created.path("data").path("id").asLong();
    assertThat(id).isPositive();

    BlogFriendLink update = new BlogFriendLink();
    update.setSiteName("Temp Link Updated");
    update.setSiteUrl(request.getSiteUrl());
    update.setLinkStatus("APPROVED");

    mockMvc.perform(put("/admin/friend-link/{id}", id)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(update)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));

    mockMvc.perform(delete("/admin/friend-link/{id}", id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));
}
```

Also add negative tests for:
- blank `siteName`
- blank `siteUrl`
- missing `createdBy` on create
- `createdBy` does not exist
- duplicate `siteUrl`
- invalid `linkStatus`
- invalid page params
- update missing row -> `404`
- delete missing row -> `404`

- [ ] **Step 2: Write the failing Operation Log controller tests**

Add read-only paging tests for `GET /admin/operation-log/page`.

Use this minimal expected behavior:

```java
@Test
void shouldReturnPagedOperationLogs() throws Exception {
    mockMvc.perform(get("/admin/operation-log/page")
                    .param("current", "1")
                    .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.current").value(1))
            .andExpect(jsonPath("$.data.records.length()").value(3));
}

@Test
void shouldFilterOperationLogs() throws Exception {
    mockMvc.perform(get("/admin/operation-log/page")
                    .param("current", "1")
                    .param("size", "10")
                    .param("targetType", "ARTICLE")
                    .param("actionResult", "SUCCESS"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));
}
```

Negative coverage:
- `current <= 0`
- `size <= 0`
- `size > 100`
- invalid `actionResult`

- [ ] **Step 3: Write the failing Auth controller tests**

Create a dedicated `AuthControllerTest` that proves the minimal login contract before any implementation code exists.

Use a request DTO and response DTO in the test expectations:

```java
@Test
void shouldLoginWithValidCredentials() throws Exception {
    String body = """
            {
              "userName": "root",
              "password": "123456"
            }
            """;

    mockMvc.perform(post("/admin/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.userName").value("root"))
            .andExpect(jsonPath("$.data.roleCode").value("SUPER_ADMIN"))
            .andExpect(jsonPath("$.data.passwordHash").doesNotExist());
}
```

Add negative tests for:
- blank username
- blank password
- wrong password -> `400`
- disabled user -> `400`
- deleted/missing user -> `400`

For the disabled-user test, use `JdbcTemplate` to temporarily update `sys_user.user_status = 'DISABLED'` inside the transaction.

- [ ] **Step 4: Run the new controller tests and verify they fail for the expected reason**

Run:

```powershell
./mvnw.cmd -pl personal-blog-admin "-Dtest=BlogFriendLinkControllerTest,SysOperationLogControllerTest,AuthControllerTest,OpenApiDocumentationTest" test
```

Expected failure:
- compilation fails because `BlogFriendLink`, `SysOperationLog`, request/response DTOs, and controllers do not exist yet, or
- tests fail with `404` mappings because the new endpoints are not implemented yet

- [ ] **Step 5: Commit the red test state**

```bash
git add personal-blog-admin/src/test/java/org/example/personalblogsystem/controller
git commit -m "test: add failing coverage for p2 admin modules"
```

### Task 2: Implement Friend Link CRUD without touching existing P0/P1 behavior

**Files:**
- Create: `personal-blog-admin/src/main/java/org/example/personalblogsystem/entity/BlogFriendLink.java`
- Create: `personal-blog-admin/src/main/java/org/example/personalblogsystem/mapper/BlogFriendLinkMapper.java`
- Create: `personal-blog-admin/src/main/java/org/example/personalblogsystem/service/IBlogFriendLinkService.java`
- Create: `personal-blog-admin/src/main/java/org/example/personalblogsystem/service/impl/BlogFriendLinkServiceImpl.java`
- Create: `personal-blog-admin/src/main/java/org/example/personalblogsystem/controller/BlogFriendLinkController.java`
- Modify: `personal-blog-admin/src/main/java/org/example/personalblogsystem/PersonalBlogSystemApplication.java` only if package scanning needs no change; otherwise leave untouched
- Test: `./mvnw.cmd -pl personal-blog-admin "-Dtest=BlogFriendLinkControllerTest" test`

- [ ] **Step 1: Add the `BlogFriendLink` entity mapped to `blog_friend_link`**

Create the entity using the same Lombok + MyBatis-Plus annotations already used in `BlogCategory` and `BlogTag`.

Required fields:

```java
@TableId(value = "id", type = IdType.AUTO)
private Long id;

@TableField("site_name")
private String siteName;

@TableField("site_url")
private String siteUrl;

@TableField("site_logo")
private String siteLogo;

@TableField("site_desc")
private String siteDesc;

@TableField("owner_name")
private String ownerName;

@TableField("contact_email")
private String contactEmail;

@TableField("link_status")
private String linkStatus;

@TableField("created_by")
private Long createdBy;

@TableField("create_time")
private LocalDateTime createTime;

@TableField("update_time")
private LocalDateTime updateTime;

@TableLogic
private Boolean deleted;
```

- [ ] **Step 2: Add the mapper and service interface**

Keep the mapper as a plain `BaseMapper<BlogFriendLink>`:

```java
public interface BlogFriendLinkMapper extends BaseMapper<BlogFriendLink> {
}
```

Service methods should be:

```java
Page<BlogFriendLink> pageFriendLinks(long current, long size, String keyword, String status);
BlogFriendLink createFriendLink(BlogFriendLink friendLink);
BlogFriendLink updateFriendLink(Long id, BlogFriendLink friendLink);
boolean deleteFriendLink(Long id);
```

- [ ] **Step 3: Implement Friend Link service rules**

Implement `BlogFriendLinkServiceImpl` with the same style as `BlogCategoryServiceImpl` and `BlogTagServiceImpl`.

Rules to enforce:
- `siteName` non-blank
- `siteUrl` non-blank
- `createdBy` required on create and must exist in `sys_user`
- `siteUrl` unique among non-deleted rows
- `linkStatus` allowed values: `PENDING`, `APPROVED`, `REJECTED`
- default `linkStatus = PENDING` on create if missing
- page query orders by `updateTime desc, id desc`
- keyword matches `siteName`, `siteUrl`, `siteDesc`

Minimal validation helpers:

```java
private String normalizeStatus(String status, String defaultStatus) {
    String value = status == null ? null : status.trim();
    if (value == null || value.isEmpty()) {
        return defaultStatus;
    }
    String normalized = value.toUpperCase();
    if (!"PENDING".equals(normalized) && !"APPROVED".equals(normalized) && !"REJECTED".equals(normalized)) {
        throw new IllegalArgumentException("status must be one of PENDING, APPROVED, REJECTED");
    }
    return normalized;
}
```

- [ ] **Step 4: Implement the Friend Link controller**

Create `/admin/friend-link` endpoints with the same controller style already used in category/article:

```java
@RestController
@RequestMapping("/admin/friend-link")
public class BlogFriendLinkController {

    @GetMapping("/page")
    public Result<Page<BlogFriendLink>> page(...)

    @PostMapping
    public Result<BlogFriendLink> create(@RequestBody BlogFriendLink friendLink)

    @PutMapping("/{id}")
    public Result<BlogFriendLink> update(@PathVariable Long id, @RequestBody BlogFriendLink friendLink)

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id)
}
```

Use the same page validation rules already standardized in P0/P1:
- `current > 0`
- `size > 0`
- `size <= 100`

- [ ] **Step 5: Run the Friend Link tests and verify they pass**

Run:

```powershell
./mvnw.cmd -pl personal-blog-admin "-Dtest=BlogFriendLinkControllerTest" test
```

Expected result:
- `BUILD SUCCESS`
- all friend-link tests green

- [ ] **Step 6: Commit the Friend Link slice**

```bash
git add personal-blog-admin/src/main/java/org/example/personalblogsystem/entity/BlogFriendLink.java personal-blog-admin/src/main/java/org/example/personalblogsystem/mapper/BlogFriendLinkMapper.java personal-blog-admin/src/main/java/org/example/personalblogsystem/service/IBlogFriendLinkService.java personal-blog-admin/src/main/java/org/example/personalblogsystem/service/impl/BlogFriendLinkServiceImpl.java personal-blog-admin/src/main/java/org/example/personalblogsystem/controller/BlogFriendLinkController.java personal-blog-admin/src/test/java/org/example/personalblogsystem/controller/BlogFriendLinkControllerTest.java
git commit -m "feat: add admin friend link management"
```

### Task 3: Implement read-only Operation Log paging

**Files:**
- Create: `personal-blog-admin/src/main/java/org/example/personalblogsystem/entity/SysOperationLog.java`
- Create: `personal-blog-admin/src/main/java/org/example/personalblogsystem/mapper/SysOperationLogMapper.java`
- Create: `personal-blog-admin/src/main/java/org/example/personalblogsystem/service/ISysOperationLogService.java`
- Create: `personal-blog-admin/src/main/java/org/example/personalblogsystem/service/impl/SysOperationLogServiceImpl.java`
- Create: `personal-blog-admin/src/main/java/org/example/personalblogsystem/controller/SysOperationLogController.java`
- Test: `./mvnw.cmd -pl personal-blog-admin "-Dtest=SysOperationLogControllerTest" test`

- [ ] **Step 1: Add the `SysOperationLog` entity**

Map the table directly:

```java
@TableId(value = "id", type = IdType.AUTO)
private Long id;

@TableField("operator_user_id")
private Long operatorUserId;

@TableField("target_type")
private String targetType;

@TableField("target_id")
private Long targetId;

@TableField("action_type")
private String actionType;

@TableField("action_result")
private String actionResult;

@TableField("action_detail")
private String actionDetail;

@TableField("create_time")
private LocalDateTime createTime;

@TableField("update_time")
private LocalDateTime updateTime;

@TableLogic
private Boolean deleted;
```

- [ ] **Step 2: Add mapper and service interface**

Keep the mapper simple:

```java
public interface SysOperationLogMapper extends BaseMapper<SysOperationLog> {
}
```

Service method:

```java
Page<SysOperationLog> pageOperationLogs(long current, long size, Long operatorUserId, String targetType, String actionResult);
```

- [ ] **Step 3: Implement the paging service**

Use `LambdaQueryWrapper<SysOperationLog>` with optional filters:
- `operatorUserId`
- `targetType`
- `actionResult`

Normalize `actionResult` to `SUCCESS` / `FAILED` and reject any other value.

Sort order:

```java
queryWrapper.orderByDesc(SysOperationLog::getCreateTime, SysOperationLog::getId);
```

- [ ] **Step 4: Implement the controller**

Create `GET /admin/operation-log/page` only:

```java
@RestController
@RequestMapping("/admin/operation-log")
public class SysOperationLogController {

    @GetMapping("/page")
    public Result<Page<SysOperationLog>> page(@RequestParam long current,
                                              @RequestParam long size,
                                              @RequestParam(required = false) Long operatorUserId,
                                              @RequestParam(required = false) String targetType,
                                              @RequestParam(required = false) String actionResult) {
        // validate page and actionResult, then delegate
    }
}
```

- [ ] **Step 5: Run the Operation Log tests**

Run:

```powershell
./mvnw.cmd -pl personal-blog-admin "-Dtest=SysOperationLogControllerTest" test
```

Expected result:
- `BUILD SUCCESS`
- all operation-log tests green

- [ ] **Step 6: Commit the Operation Log slice**

```bash
git add personal-blog-admin/src/main/java/org/example/personalblogsystem/entity/SysOperationLog.java personal-blog-admin/src/main/java/org/example/personalblogsystem/mapper/SysOperationLogMapper.java personal-blog-admin/src/main/java/org/example/personalblogsystem/service/ISysOperationLogService.java personal-blog-admin/src/main/java/org/example/personalblogsystem/service/impl/SysOperationLogServiceImpl.java personal-blog-admin/src/main/java/org/example/personalblogsystem/controller/SysOperationLogController.java personal-blog-admin/src/test/java/org/example/personalblogsystem/controller/SysOperationLogControllerTest.java
git commit -m "feat: add admin operation log paging"
```

### Task 4: Implement minimal login without global auth

**Files:**
- Create: `personal-blog-admin/src/main/java/org/example/personalblogsystem/dto/LoginRequest.java`
- Create: `personal-blog-admin/src/main/java/org/example/personalblogsystem/dto/LoginUserResponse.java`
- Create: `personal-blog-admin/src/main/java/org/example/personalblogsystem/service/IAuthService.java`
- Create: `personal-blog-admin/src/main/java/org/example/personalblogsystem/service/impl/AuthServiceImpl.java`
- Create: `personal-blog-admin/src/main/java/org/example/personalblogsystem/controller/AuthController.java`
- Modify: `personal-blog-admin/src/main/java/org/example/personalblogsystem/mapper/SysUserMapper.java`
- Test: `./mvnw.cmd -pl personal-blog-admin "-Dtest=AuthControllerTest" test`

- [ ] **Step 1: Add request/response DTOs**

`LoginRequest`:

```java
@Getter
@Setter
public class LoginRequest {
    private String userName;
    private String password;
}
```

`LoginUserResponse`:

```java
@Getter
@Setter
public class LoginUserResponse {
    private Long id;
    private String userName;
    private String nickName;
    private String email;
    private String phone;
    private String avatarUrl;
    private String introduction;
    private Long roleId;
    private String roleCode;
    private String roleName;
    private String userStatus;
}
```

Do not put `passwordHash` on the response DTO.

- [ ] **Step 2: Extend `SysUserMapper` with a login query**

Add a minimal join query that fetches the user plus role metadata needed by the response:

```java
@Select("""
        select u.id,
               u.user_name,
               u.password_hash,
               u.nick_name,
               u.email,
               u.phone,
               u.avatar_url,
               u.introduction,
               u.role_id,
               u.user_status,
               r.role_code,
               r.role_name
        from sys_user u
        inner join sys_role r on u.role_id = r.id
        where u.user_name = #{userName}
          and u.deleted = 0
          and r.deleted = 0
        limit 1
        """)
LoginUserQueryRow selectLoginUserByUserName(@Param("userName") String userName);
```

If you prefer not to add an inner class to the mapper, create a small package-private query row DTO under `dto/` or `mapper/`.

- [ ] **Step 3: Implement `AuthServiceImpl` with SHA-256 password matching**

Keep the logic minimal and deterministic:

```java
public LoginUserResponse login(LoginRequest request) {
    validateRequest(request);
    LoginUserQueryRow row = sysUserMapper.selectLoginUserByUserName(request.getUserName().trim());
    if (row == null || !"ENABLED".equals(row.getUserStatus())) {
        throw new IllegalArgumentException("username or password is incorrect");
    }

    String hashedPassword = sha256Hex(request.getPassword());
    if (!hashedPassword.equalsIgnoreCase(row.getPasswordHash())) {
        throw new IllegalArgumentException("username or password is incorrect");
    }

    return toResponse(row);
}
```

Use Java SHA-256 to match the SQL seed data:

```java
private String sha256Hex(String rawPassword) {
    MessageDigest digest = MessageDigest.getInstance("SHA-256");
    byte[] hash = digest.digest(rawPassword.getBytes(StandardCharsets.UTF_8));
    StringBuilder builder = new StringBuilder();
    for (byte value : hash) {
        builder.append(String.format("%02x", value));
    }
    return builder.toString();
}
```

Validation rules:
- `userName` non-blank
- `password` non-blank
- disabled user returns the same generic error as wrong password

Do not generate token, session, cookie, or cache entry in P2.

- [ ] **Step 4: Implement `AuthController`**

Create the controller:

```java
@RestController
@RequestMapping("/admin/auth")
public class AuthController {

    @PostMapping("/login")
    public Result<LoginUserResponse> login(@RequestBody LoginRequest request) {
        return Result.ok(authService.login(request));
    }
}
```

All validation failures should flow through `IllegalArgumentException` into the existing global exception handler and return `code = 400`.

- [ ] **Step 5: Run the Auth tests**

Run:

```powershell
./mvnw.cmd -pl personal-blog-admin "-Dtest=AuthControllerTest" test
```

Expected result:
- valid login returns `200`
- invalid credentials return `400`
- response contains role info
- response does not expose `passwordHash`

- [ ] **Step 6: Commit the Auth slice**

```bash
git add personal-blog-admin/src/main/java/org/example/personalblogsystem/dto/LoginRequest.java personal-blog-admin/src/main/java/org/example/personalblogsystem/dto/LoginUserResponse.java personal-blog-admin/src/main/java/org/example/personalblogsystem/service/IAuthService.java personal-blog-admin/src/main/java/org/example/personalblogsystem/service/impl/AuthServiceImpl.java personal-blog-admin/src/main/java/org/example/personalblogsystem/controller/AuthController.java personal-blog-admin/src/main/java/org/example/personalblogsystem/mapper/SysUserMapper.java personal-blog-admin/src/test/java/org/example/personalblogsystem/controller/AuthControllerTest.java
git commit -m "feat: add minimal admin login endpoint"
```

### Task 5: Regression and OpenAPI verification

**Files:**
- Modify: `personal-blog-admin/src/test/java/org/example/personalblogsystem/controller/OpenApiDocumentationTest.java` if the current assertions need to tolerate the expanded API surface
- Test: `./mvnw.cmd test`

- [ ] **Step 1: Add OpenAPI presence assertions for the new P2 endpoints**

In `OpenApiDocumentationTest`, assert the generated API docs expose these paths:

```java
assertThat(json).contains("/admin/friend-link/page");
assertThat(json).contains("/admin/operation-log/page");
assertThat(json).contains("/admin/auth/login");
```

- [ ] **Step 2: Run the full test suite**

Run:

```powershell
./mvnw.cmd test
```

Expected result:
- `BUILD SUCCESS`
- no regressions in completed P0/P1 tests

- [ ] **Step 3: Manual smoke verification in IDEA or browser**

After tests pass, start `PersonalBlogSystemApplication` and verify:

```text
http://localhost:8080/swagger-ui/index.html
http://localhost:8080/v3/api-docs
```

Smoke-check:
- `GET /admin/friend-link/page`
- `GET /admin/operation-log/page`
- `POST /admin/auth/login`

- [ ] **Step 4: Commit the final verification-adjustment changes**

```bash
git add personal-blog-admin/src/test/java/org/example/personalblogsystem/controller/OpenApiDocumentationTest.java
git commit -m "test: cover p2 openapi endpoints"
```

### Task 6: Publish and document the P2 scope

**Files:**
- Modify: `README.md`
- Modify: `docs/api-doc.md` only if the repository now uses this file as the human-facing manual supplement
- Test: `git status --short`

- [ ] **Step 1: Update the README status section**

Add a concise status note that P2 now includes:
- friend-link CRUD
- operation-log page
- minimal login endpoint without global auth

Use wording like:

```markdown
当前已实现：
- 友情链接后台管理接口
- 操作日志分页查询接口
- 最小登录接口（仅校验用户名密码并返回用户信息，不做全局鉴权）
```

- [ ] **Step 2: If manual API notes are still maintained, update them to match the runtime API**

Only update the manual doc if it is still intended to be kept. If so, add:
- `GET /admin/friend-link/page`
- `POST /admin/friend-link`
- `PUT /admin/friend-link/{id}`
- `DELETE /admin/friend-link/{id}`
- `GET /admin/operation-log/page`
- `POST /admin/auth/login`

- [ ] **Step 3: Run a clean status check**

Run:

```powershell
git status --short
```

Expected result:
- only the intended P2 files remain modified before the final commit

- [ ] **Step 4: Commit the documentation updates**

```bash
git add README.md docs/api-doc.md
git commit -m "docs: update p2 feature status"
```

## Self-review checklist

- P2 stays limited to `friend-link`, `operation-log`, and minimal `login`
- No JWT, Spring Security, filter, interceptor, or request-guard work is introduced
- Existing P0/P1 route shapes remain unchanged
- Login response never exposes `passwordHash`
- Friend-link delete uses normal logical delete, not a new stored procedure
- Operation-log remains read-only in P2
- Full suite must end on `./mvnw.cmd test` green before publishing
