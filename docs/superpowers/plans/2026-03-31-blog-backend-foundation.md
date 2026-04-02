# Blog Backend Foundation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the first runnable backend foundation for the personal blog system by adding shared response/error infrastructure, MyBatis-Plus + Druid + MySQL integration, code generation for core tables, and database-backed CRUD verification tests.

**Architecture:** Keep the project layered by responsibility: `personal-blog-common` holds reusable API response and exception infrastructure, while `personal-blog-admin` owns runtime configuration, persistence integration, generated blog domain code, and tests. Use MyBatis-Plus as the ORM layer, Druid as the datasource and monitoring layer, and generate code only for the core tables needed for this week’s assignment: `sys_user`, `sys_role`, `blog_article`, and `blog_category`.

**Tech Stack:** Java 17, Spring Boot 4.0.3, Maven multi-module, MySQL 8.x, Druid, MyBatis-Plus, MyBatis-Plus Generator, JUnit 5

---

### Task 1: Add backend infrastructure dependencies

**Files:**
- Modify: `pom.xml`
- Modify: `personal-blog-admin/pom.xml`
- Modify: `personal-blog-common/pom.xml`
- Test: `./mvnw.cmd -pl personal-blog-admin dependency:tree`

- [ ] **Step 1: Add version properties and shared dependency management to the parent POM**

Add version properties so all module versions stay centralized in the parent POM:

```xml
<properties>
    <java.version>17</java.version>
    <mybatis-plus.version>3.5.7</mybatis-plus.version>
    <druid.version>1.2.23</druid.version>
    <mysql.version>8.4.0</mysql.version>
</properties>
```

If needed, add `dependencyManagement` to keep child modules clean:

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>com.baomidou</groupId>
            <artifactId>mybatis-plus-spring-boot4-starter</artifactId>
            <version>${mybatis-plus.version}</version>
        </dependency>
        <dependency>
            <groupId>com.alibaba</groupId>
            <artifactId>druid-spring-boot-3-starter</artifactId>
            <version>${druid.version}</version>
        </dependency>
        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
            <version>${mysql.version}</version>
        </dependency>
    </dependencies>
</dependencyManagement>
```

- [ ] **Step 2: Add runtime and test dependencies to `personal-blog-admin/pom.xml`**

Keep the existing web and test starters, then add database and generator dependencies:

```xml
<dependency>
    <groupId>com.baomidou</groupId>
    <artifactId>mybatis-plus-spring-boot4-starter</artifactId>
</dependency>

<dependency>
    <groupId>com.alibaba</groupId>
    <artifactId>druid-spring-boot-3-starter</artifactId>
</dependency>

<dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
    <scope>runtime</scope>
</dependency>

<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <optional>true</optional>
</dependency>
```

Also add generator tooling in a way that keeps it available for manual generation without affecting runtime:

```xml
<dependency>
    <groupId>com.baomidou</groupId>
    <artifactId>mybatis-plus-generator</artifactId>
    <version>${mybatis-plus.version}</version>
</dependency>

<dependency>
    <groupId>org.apache.velocity</groupId>
    <artifactId>velocity-engine-core</artifactId>
    <version>2.3</version>
</dependency>
```

- [ ] **Step 3: Keep `personal-blog-common/pom.xml` minimal**

Add only the dependencies needed for shared DTO/exception code:

```xml
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <optional>true</optional>
</dependency>
```

Do not add datasource or MyBatis dependencies to `personal-blog-common`.

- [ ] **Step 4: Verify dependency resolution**

Run:

```bash
./mvnw.cmd -pl personal-blog-admin dependency:tree
```

Expected:
- Maven finishes successfully
- Dependency tree includes MyBatis-Plus starter, Druid starter, MySQL driver, and `personal-blog-common`

- [ ] **Step 5: Commit**

```bash
git add pom.xml personal-blog-admin/pom.xml personal-blog-common/pom.xml
git commit -m "build: add backend foundation dependencies"
```

### Task 2: Build shared response and exception infrastructure in `common`

**Files:**
- Create: `personal-blog-common/src/main/java/org/example/personalblogcommon/result/Result.java`
- Create: `personal-blog-common/src/main/java/org/example/personalblogcommon/result/ResultCodeEnum.java`
- Create: `personal-blog-common/src/main/java/org/example/personalblogcommon/exception/BlogException.java`
- Create: `personal-blog-common/src/main/java/org/example/personalblogcommon/handler/GlobalExceptionHandler.java`
- Test: `personal-blog-admin/src/test/java/org/example/personalblogsystem/handler/GlobalExceptionHandlerTest.java`

- [ ] **Step 1: Create the error code enum**

Use explicit numeric and text codes so controllers and exception handlers share one standard:

```java
package org.example.personalblogcommon.result;

import lombok.Getter;

@Getter
public enum ResultCodeEnum {
    SUCCESS(200, "操作成功"),
    FAIL(500, "操作失败"),
    PARAM_ERROR(400, "请求参数错误"),
    NOT_FOUND(404, "数据不存在"),
    UNAUTHORIZED(401, "未登录或无权限"),
    SYSTEM_ERROR(5000, "系统异常");

    private final Integer code;
    private final String message;

    ResultCodeEnum(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}
```

- [ ] **Step 2: Create the unified result wrapper**

Keep the contract simple and stable:

```java
package org.example.personalblogcommon.result;

import lombok.Data;

@Data
public class Result<T> {
    private Integer code;
    private String message;
    private T data;

    public static <T> Result<T> ok(T data) {
        Result<T> result = new Result<>();
        result.setCode(ResultCodeEnum.SUCCESS.getCode());
        result.setMessage(ResultCodeEnum.SUCCESS.getMessage());
        result.setData(data);
        return result;
    }

    public static <T> Result<T> fail(ResultCodeEnum codeEnum) {
        Result<T> result = new Result<>();
        result.setCode(codeEnum.getCode());
        result.setMessage(codeEnum.getMessage());
        return result;
    }

    public static <T> Result<T> fail(Integer code, String message) {
        Result<T> result = new Result<>();
        result.setCode(code);
        result.setMessage(message);
        return result;
    }
}
```

- [ ] **Step 3: Create the custom business exception**

Use a runtime exception that can carry both enum-based and custom messages:

```java
package org.example.personalblogcommon.exception;

import lombok.Getter;
import org.example.personalblogcommon.result.ResultCodeEnum;

@Getter
public class BlogException extends RuntimeException {
    private final Integer code;

    public BlogException(ResultCodeEnum resultCodeEnum) {
        super(resultCodeEnum.getMessage());
        this.code = resultCodeEnum.getCode();
    }

    public BlogException(Integer code, String message) {
        super(message);
        this.code = code;
    }
}
```

- [ ] **Step 4: Create the global exception handler**

Put it in `common` so `admin` can reuse it immediately:

```java
package org.example.personalblogcommon.handler;

import org.example.personalblogcommon.exception.BlogException;
import org.example.personalblogcommon.result.Result;
import org.example.personalblogcommon.result.ResultCodeEnum;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BlogException.class)
    public Result<Object> handleBlogException(BlogException exception) {
        return Result.fail(exception.getCode(), exception.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public Result<Object> handleIllegalArgumentException(IllegalArgumentException exception) {
        return Result.fail(ResultCodeEnum.PARAM_ERROR.getCode(), exception.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public Result<Object> handleException(Exception exception) {
        return Result.fail(ResultCodeEnum.SYSTEM_ERROR.getCode(), ResultCodeEnum.SYSTEM_ERROR.getMessage());
    }
}
```

- [ ] **Step 5: Add a focused exception handler test in `admin`**

Create a lightweight test controller inside the test package and verify response shape:

```java
package org.example.personalblogsystem.handler;

import org.example.personalblogcommon.exception.BlogException;
import org.example.personalblogcommon.result.ResultCodeEnum;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(GlobalExceptionHandlerTest.TestController.class)
@Import(org.example.personalblogcommon.handler.GlobalExceptionHandler.class)
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldReturnBusinessErrorResponse() throws Exception {
        mockMvc.perform(get("/test/exception"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCodeEnum.PARAM_ERROR.getCode()))
                .andExpect(jsonPath("$.message").value(ResultCodeEnum.PARAM_ERROR.getMessage()));
    }

    @RestController
    static class TestController {
        @GetMapping("/test/exception")
        public String throwException() {
            throw new BlogException(ResultCodeEnum.PARAM_ERROR);
        }
    }
}
```

- [ ] **Step 6: Run the focused test**

Run:

```bash
./mvnw.cmd -pl personal-blog-admin -Dtest=GlobalExceptionHandlerTest test
```

Expected:
- Test passes
- Returned JSON contains `code`, `message`, and no stack trace output to the client

- [ ] **Step 7: Commit**

```bash
git add personal-blog-common/src/main/java personal-blog-admin/src/test/java
git commit -m "feat: add shared result and exception handling"
```

### Task 3: Add datasource, MyBatis-Plus, and Druid monitoring configuration

**Files:**
- Modify: `personal-blog-admin/src/main/resources/application.properties`
- Create: `personal-blog-admin/src/main/resources/application-dev.yml`
- Create: `personal-blog-admin/src/main/resources/application-test.yml`
- Create: `personal-blog-admin/src/main/resources/application-prod.yml`
- Create: `personal-blog-admin/src/main/java/org/example/personalblogsystem/config/MybatisPlusConfig.java`
- Create: `personal-blog-admin/src/main/java/org/example/personalblogsystem/config/DruidConfig.java`
- Test: `personal-blog-admin/src/test/java/org/example/personalblogsystem/config/DataSourceConnectionTest.java`

- [ ] **Step 1: Move from single properties file to profile-driven YAML**

Keep `application.properties` minimal:

```properties
spring.application.name=personal-blog-admin
spring.profiles.active=dev
```

Create `application-dev.yml` with local database and Druid settings:

```yaml
spring:
  datasource:
    druid:
      url: jdbc:mysql://localhost:3306/personal_blog_system?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai
      username: root
      password: 123456
      driver-class-name: com.mysql.cj.jdbc.Driver
      initial-size: 5
      min-idle: 5
      max-active: 20
      test-while-idle: true
      validation-query: SELECT 1
      stat-view-servlet:
        enabled: true
        url-pattern: /druid/*
        login-username: admin
        login-password: admin123
      web-stat-filter:
        enabled: true

mybatis-plus:
  mapper-locations: classpath:/mapper/*.xml
  configuration:
    map-underscore-to-camel-case: true
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
```

Create `application-test.yml` with a dedicated local test database or the same database if coursework constraints require it:

```yaml
spring:
  datasource:
    druid:
      url: jdbc:mysql://localhost:3306/personal_blog_system?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai
      username: root
      password: 123456
      driver-class-name: com.mysql.cj.jdbc.Driver

mybatis-plus:
  mapper-locations: classpath:/mapper/*.xml
  configuration:
    map-underscore-to-camel-case: true
```

Create `application-prod.yml` as a placeholder template for later deployment:

```yaml
spring:
  datasource:
    druid:
      url: jdbc:mysql://localhost:3306/personal_blog_system
      username: root
      password: change-me
      driver-class-name: com.mysql.cj.jdbc.Driver
```

- [ ] **Step 2: Add MyBatis-Plus configuration**

Create a simple config class that enables mapper scanning:

```java
package org.example.personalblogsystem.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan("org.example.personalblogsystem.mapper")
public class MybatisPlusConfig {
}
```

- [ ] **Step 3: Add Druid servlet/filter registration config only if starter auto-config is not enough**

Prefer configuration-only setup first. If the monitoring page does not open at `/druid`, add:

```java
package org.example.personalblogsystem.config;

import com.alibaba.druid.support.http.StatViewServlet;
import com.alibaba.druid.support.http.WebStatFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DruidConfig {

    @Bean
    public ServletRegistrationBean<StatViewServlet> statViewServlet() {
        ServletRegistrationBean<StatViewServlet> bean =
                new ServletRegistrationBean<>(new StatViewServlet(), "/druid/*");
        bean.addInitParameter("loginUsername", "admin");
        bean.addInitParameter("loginPassword", "admin123");
        return bean;
    }

    @Bean
    public FilterRegistrationBean<WebStatFilter> webStatFilter() {
        FilterRegistrationBean<WebStatFilter> bean = new FilterRegistrationBean<>(new WebStatFilter());
        bean.addUrlPatterns("/*");
        bean.addInitParameter("exclusions", "*.js,*.css,*.png,*.jpg,*.ico,/druid/*");
        return bean;
    }
}
```

- [ ] **Step 4: Add a database connection test**

Create:

```java
package org.example.personalblogsystem.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.sql.DataSource;
import java.sql.Connection;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class DataSourceConnectionTest {

    @Autowired
    private DataSource dataSource;

    @Test
    void shouldConnectToDatabase() throws Exception {
        assertNotNull(dataSource);
        try (Connection connection = dataSource.getConnection()) {
            assertNotNull(connection);
        }
    }
}
```

- [ ] **Step 5: Run the connection test**

Run:

```bash
./mvnw.cmd -pl personal-blog-admin -Dspring.profiles.active=test -Dtest=DataSourceConnectionTest test
```

Expected:
- Test passes
- `DataSource` is injected successfully
- No authentication or driver errors

- [ ] **Step 6: Manually verify the Druid page**

Run:

```bash
./mvnw.cmd -pl personal-blog-admin spring-boot:run
```

Then open:

```text
http://localhost:8080/druid
```

Expected:
- Login page or monitoring page appears
- Using `admin/admin123` opens the monitoring dashboard

- [ ] **Step 7: Commit**

```bash
git add personal-blog-admin/src/main/resources personal-blog-admin/src/main/java personal-blog-admin/src/test/java
git commit -m "feat: configure datasource mybatis-plus and druid"
```

### Task 4: Generate MyBatis-Plus code for core tables

**Files:**
- Create: `personal-blog-admin/src/test/java/org/example/personalblogsystem/generator/MyBatisPlusGenerator.java`
- Create: `personal-blog-admin/src/main/java/org/example/personalblogsystem/entity/*.java`
- Create: `personal-blog-admin/src/main/java/org/example/personalblogsystem/mapper/*.java`
- Create: `personal-blog-admin/src/main/java/org/example/personalblogsystem/service/*.java`
- Create: `personal-blog-admin/src/main/java/org/example/personalblogsystem/service/impl/*.java`
- Create: `personal-blog-admin/src/main/java/org/example/personalblogsystem/controller/*.java`
- Create: `personal-blog-admin/src/main/resources/mapper/*.xml`
- Test: `./mvnw.cmd -pl personal-blog-admin -Dtest=MyBatisPlusGenerator test`

- [ ] **Step 1: Create the generator entry point**

Generate only these tables:
- `sys_user`
- `sys_role`
- `blog_article`
- `blog_category`

Use one deterministic generator class:

```java
package org.example.personalblogsystem.generator;

import com.baomidou.mybatisplus.generator.FastAutoGenerator;
import com.baomidou.mybatisplus.generator.config.OutputFile;

import java.util.Collections;

public class MyBatisPlusGenerator {

    public static void main(String[] args) {
        FastAutoGenerator.create(
                        "jdbc:mysql://localhost:3306/personal_blog_system?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai",
                        "root",
                        "123456")
                .globalConfig(builder -> builder
                        .author("student")
                        .outputDir(System.getProperty("user.dir") + "/src/main/java")
                        .disableOpenDir())
                .packageConfig(builder -> builder
                        .parent("org.example.personalblogsystem")
                        .entity("entity")
                        .mapper("mapper")
                        .service("service")
                        .serviceImpl("service.impl")
                        .controller("controller")
                        .pathInfo(Collections.singletonMap(
                                OutputFile.xml,
                                System.getProperty("user.dir") + "/src/main/resources/mapper")))
                .strategyConfig(builder -> builder
                        .addInclude("sys_user", "sys_role", "blog_article", "blog_category")
                        .entityBuilder().enableLombok()
                        .controllerBuilder().enableRestStyle())
                .execute();
    }
}
```

- [ ] **Step 2: Run the generator once**

Run from the module root:

```bash
../mvnw.cmd -pl personal-blog-admin -DskipTests test-compile
java -cp target/test-classes;target/classes;%USERPROFILE%\.m2\repository\* org.example.personalblogsystem.generator.MyBatisPlusGenerator
```

Expected:
- Entity, mapper, service, controller, and XML files are generated for the four core tables
- No files are generated for tags, comments, friend links, or logs

- [ ] **Step 3: Clean up generated code to match the project conventions**

Immediately normalize the generated code:

```java
@TableName("sys_user")
public class SysUser {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String userName;
    private String nickName;
    private Long roleId;
    private String userStatus;
}
```

Controller return types should use the shared `Result` wrapper:

```java
@RestController
@RequestMapping("/admin/user")
public class SysUserController {

    private final ISysUserService sysUserService;

    public SysUserController(ISysUserService sysUserService) {
        this.sysUserService = sysUserService;
    }

    @GetMapping("/{id}")
    public Result<SysUser> getById(@PathVariable Long id) {
        return Result.ok(sysUserService.getById(id));
    }
}
```

- [ ] **Step 4: Verify XML and package scan alignment**

Check:
- XML files are under `personal-blog-admin/src/main/resources/mapper`
- Mapper interfaces are under `org.example.personalblogsystem.mapper`
- `@MapperScan` package matches exactly

Run:

```bash
./mvnw.cmd -pl personal-blog-admin test-compile
```

Expected:
- Build passes
- No mapper resource not found errors

- [ ] **Step 5: Commit**

```bash
git add personal-blog-admin/src/main/java personal-blog-admin/src/main/resources/mapper personal-blog-admin/src/test/java
git commit -m "feat: generate mybatis-plus code for core tables"
```

### Task 5: Add CRUD verification tests for generated persistence code

**Files:**
- Create: `personal-blog-admin/src/test/java/org/example/personalblogsystem/mapper/SysUserMapperTest.java`
- Create: `personal-blog-admin/src/test/java/org/example/personalblogsystem/mapper/BlogCategoryMapperTest.java`
- Modify: generated mapper/service/controller classes as needed
- Test: `./mvnw.cmd -pl personal-blog-admin -Dspring.profiles.active=test test`

- [ ] **Step 1: Add a read test for existing seed data**

Use seeded SQL rows rather than inserting everything manually:

```java
package org.example.personalblogsystem.mapper;

import org.example.personalblogsystem.entity.SysUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = "spring.profiles.active=test")
class SysUserMapperTest {

    @Autowired
    private SysUserMapper sysUserMapper;

    @Test
    void shouldQueryExistingUser() {
        SysUser user = sysUserMapper.selectById(1L);
        assertNotNull(user);
        assertEquals("root", user.getUserName());
    }
}
```

- [ ] **Step 2: Add insert/update/delete verification for a safe table**

Use `blog_category` for write tests to avoid colliding with permission logic in user tables:

```java
package org.example.personalblogsystem.mapper;

import org.example.personalblogsystem.entity.BlogCategory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = "spring.profiles.active=test")
class BlogCategoryMapperTest {

    @Autowired
    private BlogCategoryMapper blogCategoryMapper;

    @Test
    void shouldInsertUpdateAndDeleteCategory() {
        BlogCategory category = new BlogCategory();
        category.setCategoryName("Test Category");
        category.setDescription("test");
        category.setSortNo(99);
        category.setCreatedBy(2L);
        category.setDeleted(0);

        int insertRows = blogCategoryMapper.insert(category);
        assertEquals(1, insertRows);
        assertNotNull(category.getId());

        category.setDescription("updated");
        int updateRows = blogCategoryMapper.updateById(category);
        assertEquals(1, updateRows);

        BlogCategory updated = blogCategoryMapper.selectById(category.getId());
        assertEquals("updated", updated.getDescription());

        int deleteRows = blogCategoryMapper.deleteById(category.getId());
        assertEquals(1, deleteRows);
    }
}
```

- [ ] **Step 3: Add one controller smoke test using the shared `Result` wrapper**

Verify that generated controller code is actually wired:

```java
package org.example.personalblogsystem.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.profiles.active=test")
@AutoConfigureMockMvc
class SysUserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldReturnWrappedUserResult() throws Exception {
        mockMvc.perform(get("/admin/user/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(1));
    }
}
```

- [ ] **Step 4: Run the full admin test suite**

Run:

```bash
./mvnw.cmd -pl personal-blog-admin -Dspring.profiles.active=test test
```

Expected:
- Database connection test passes
- Exception handling test passes
- Mapper CRUD tests pass
- Controller smoke test passes

- [ ] **Step 5: Run the full multi-module test suite**

Run:

```bash
./mvnw.cmd test
```

Expected:
- Parent project, `personal-blog-common`, and `personal-blog-admin` all build successfully

- [ ] **Step 6: Commit**

```bash
git add personal-blog-admin/src/test/java personal-blog-admin/src/main/java personal-blog-admin/src/main/resources/mapper
git commit -m "test: verify datasource and core crud flows"
```

### Task 6: Document usage and developer workflow

**Files:**
- Modify: `README.md`
- Test: manual documentation review

- [ ] **Step 1: Add backend foundation usage notes to the README**

Document:
- Required MySQL version
- How to import `sql/personal_blog_system.sql`
- How to set datasource credentials
- How to run the admin module
- How to open the Druid page
- Which tables are currently code-generated

Add a section like:

```md
## Backend Foundation Progress

- Shared `Result` response wrapper completed
- Global exception handling completed
- MyBatis-Plus integrated
- Druid datasource and monitor integrated
- Core generated modules: `sys_user`, `sys_role`, `blog_article`, `blog_category`
```

- [ ] **Step 2: Add command examples**

Include these commands exactly:

```bash
./mvnw.cmd test
./mvnw.cmd -pl personal-blog-admin spring-boot:run
```

And mention:

```text
http://localhost:8080/druid
```

- [ ] **Step 3: Review the README for consistency**

Check that:
- The documented startup class still matches the code
- The documented module names match the POMs
- The documented table names match the generator scope

- [ ] **Step 4: Commit**

```bash
git add README.md
git commit -m "docs: document backend foundation workflow"
```

## Self-Review

- The plan covers all four agreed requirements: shared response/exception foundation, datasource integration, code generation, and runtime verification tests.
- The scope is intentionally limited to the four core tables `sys_user`, `sys_role`, `blog_article`, and `blog_category`; tags, comments, friend links, and logs are intentionally out of scope for this week.
- `personal-blog-common` is kept infrastructure-only in this plan; all generated persistence and controller code stays in `personal-blog-admin`.
- The plan assumes local MySQL credentials remain `root/123456` unless the student has already changed them in Navicat or MySQL.
- If Spring Boot 4 starter coordinates differ from the selected MyBatis-Plus or Druid artifacts, resolve the exact compatible artifact version first before implementing later tasks.
