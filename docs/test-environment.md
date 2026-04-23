# Test Environment Guide

## Why the `test` Profile Is Isolated

`test` profile 现在强制使用独立测试库配置，目的是防止测试误连 `dev` 开发库。

当前契约由以下文件共同定义：

- `personal-blog-admin/src/main/resources/application-test.yml`
- `personal-blog-admin/src/main/java/org/example/personalblogsystem/config/TestProfileDatabaseEnvValidationConfig.java`

这意味着 `BLOG_DB_TEST_URL`、`BLOG_DB_TEST_USERNAME`、`BLOG_DB_TEST_PASSWORD` 三个环境变量必须同时存在；缺少任一项时，Spring Boot 会在测试启动阶段快速失败。

## Prepare a Dedicated Test Database

推荐测试 schema 名称：`personal_blog_system_test`

### 方式 A：从现有本地库复制到独立测试库

1. 先确认本地 `personal_blog_system` 已完成当前版本初始化。
2. 在 MySQL 中创建 `personal_blog_system_test`。
3. 将 `personal_blog_system` 的结构和当前种子数据复制到 `personal_blog_system_test`。
4. 把 `BLOG_DB_TEST_URL` 指向 `personal_blog_system_test`，不要指向 `personal_blog_system`。

### 方式 B：基于 SQL 脚本导入到测试库

1. 将 `sql/personal_blog_system.sql` 复制到仓库外的临时文件。
2. 把文件顶部的数据库名从 `personal_blog_system` 改为 `personal_blog_system_test`：

```sql
CREATE DATABASE IF NOT EXISTS personal_blog_system_test
DEFAULT CHARACTER SET utf8mb4
COLLATE utf8mb4_general_ci;

USE personal_blog_system_test;
```

3. 执行这份临时 SQL 文件初始化测试库。
4. 保持仓库内的 `sql/personal_blog_system.sql` 不变，不要把测试库名称直接写回正式脚本。

## Configure `BLOG_DB_TEST_*`

推荐做法是先运行仓库脚本，让它一次性把三个环境变量写入当前进程和当前用户环境：

```powershell
.\scripts\setup-test-env.ps1
```

默认会写入：

- `BLOG_DB_TEST_URL=jdbc:mysql://localhost:3306/personal_blog_system_test?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false`
- `BLOG_DB_TEST_USERNAME=root`
- `BLOG_DB_TEST_PASSWORD=123456`

如果你需要临时覆盖默认值，可以显式传参：

```powershell
.\scripts\setup-test-env.ps1 `
  -DbUrl 'jdbc:mysql://localhost:3306/personal_blog_system_test?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false' `
  -DbUsername 'root' `
  -DbPassword '123456'
```

只有在你明确不想使用脚本时，才手工设置环境变量：

```powershell
$env:BLOG_DB_TEST_URL='jdbc:mysql://localhost:3306/personal_blog_system_test?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false'
$env:BLOG_DB_TEST_USERNAME='root'
$env:BLOG_DB_TEST_PASSWORD='123456'
```

## Run Tests from PowerShell / Maven

以后统一通过脚本跑测试：

```powershell
.\scripts\test-with-test-db.ps1
```

这个脚本会先从当前进程或当前用户环境中加载 `BLOG_DB_TEST_URL`、`BLOG_DB_TEST_USERNAME`、`BLOG_DB_TEST_PASSWORD`，再执行：

```powershell
./mvnw.cmd test
```

期望结果：测试库可连接且 `BLOG_DB_TEST_*` 配置完整时，Maven 以 `BUILD SUCCESS` 结束。

如果脚本提示缺少持久化环境变量，请先运行：

```powershell
.\scripts\setup-test-env.ps1
```

## Run Tests from IntelliJ IDEA

推荐先在 PowerShell 中执行一次：

```powershell
.\scripts\setup-test-env.ps1
```

这样新的 IDEA 进程可以继承 `BLOG_DB_TEST_*`。

如果你使用已有的 IDEA 进程，请打开 `Run | Edit Configurations...`，在测试配置的 `Environment variables` 中显式加入：

```text
BLOG_DB_TEST_URL=...;BLOG_DB_TEST_USERNAME=root;BLOG_DB_TEST_PASSWORD=123456
```

不要把这三个值写进 `VM options`。如果你刚更新了持久化环境变量，请先重启 IDEA，再重新运行测试。

## Expected Fail-Fast Error

缺少任一变量时，测试在启动阶段失败是预期行为。常见报错片段如下：

```text
Missing required test database environment variables: BLOG_DB_TEST_URL, BLOG_DB_TEST_USERNAME, BLOG_DB_TEST_PASSWORD
```

这表示测试环境尚未初始化完成，不表示业务代码回归。修复方式是补齐 `BLOG_DB_TEST_*` 变量并确认测试库可连接；推荐先运行 `.\scripts\setup-test-env.ps1`，然后通过 `.\scripts\test-with-test-db.ps1` 执行测试，而不是把 `application-test.yml` 改回 `dev` 回退逻辑。

## Troubleshooting Checklist

- `Unknown database`：说明测试 schema 还没准备好，先初始化 `personal_blog_system_test`。
- `Access denied`：重新检查 `BLOG_DB_TEST_USERNAME` 和 `BLOG_DB_TEST_PASSWORD`。
- 仍然读取旧值：如果使用了持久化环境变量，重开 PowerShell 或 IntelliJ IDEA。
- 不要把 `BLOG_DB_TEST_URL` 指向 `personal_blog_system`；测试库应与开发库分离。
- 在新终端里仍然报缺少 `BLOG_DB_TEST_*`：先重新执行 `.\scripts\setup-test-env.ps1`，再运行 `.\scripts\test-with-test-db.ps1`；如果你刚刚写入了用户级环境变量，旧 PowerShell / IDEA 进程可能还没有重新加载。
