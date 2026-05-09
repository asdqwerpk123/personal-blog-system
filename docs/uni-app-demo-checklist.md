# uni-app 演示截图清单

## 1. 演示前准备截图

| 序号 | 截图内容 | 验收重点 | 建议文件名 |
| --- | --- | --- | --- |
| 1 | 后端启动成功 | 控制台显示 Spring Boot 启动完成，端口为 8081 或实际配置端口 | `01-backend-start.png` |
| 2 | `/public/articles/page` 访问成功 | HTTP 200，业务 `code=200`，返回分页结构 | `02-public-api-check.png` |
| 3 | HBuilderX 打开 `personal-blog-uniapp` | 项目根目录包含 `manifest.json` 和 `pages.json` | `03-hbuilderx-project.png` |

## 2. 功能截图

| 序号 | 截图内容 | 验收重点 | 建议文件名 |
| --- | --- | --- | --- |
| 4 | 首页文章列表 | 展示标题、摘要、封面、作者、分类、标签、时间 | `04-uniapp-home.png` |
| 5 | 搜索文章 | 输入 keyword 后列表刷新，筛选条件与列表一致 | `05-home-search.png` |
| 6 | 分类筛选 | 选择分类后只展示对应分类文章或空状态 | `06-category-filter.png` |
| 7 | 标签筛选 | 选择标签后只展示对应标签文章或空状态 | `07-tag-filter.png` |
| 8 | 文章详情 | 展示标题、作者、发布时间、封面、正文、标签 | `08-article-detail.png` |
| 9 | 评论分页 | 展示评论列表，加载更多正常 | `09-comment-page.png` |
| 10 | 注册页面 | 表单包含用户名、昵称、密码、确认密码 | `10-register.png` |
| 11 | 登录页面 | 普通用户可登录，失败时显示错误信息 | `11-login.png` |
| 12 | 发表评论 | 登录后提交评论，请求字段为 `commentContent` | `12-create-comment.png` |
| 13 | 我的页面 | 展示头像、用户名、昵称、简介等资料 | `13-profile.png` |
| 14 | 编辑资料 | 昵称、简介、邮箱、手机号可编辑并保存 | `14-edit-profile.png` |
| 15 | 头像上传 | 选择图片后头像更新展示 | `15-avatar-upload.png` |
| 16 | 我的评论 | 分页展示当前用户评论 | `16-my-comments.png` |
| 17 | 退出登录后未登录态 | token 清理后显示登录、注册入口 | `17-profile-logged-out.png` |

## 3. 异常场景截图

| 序号 | 截图内容 | 验收重点 | 建议文件名 |
| --- | --- | --- | --- |
| 18 | 未登录发表评论跳登录 | 未登录用户点击提交评论后进入登录页，并保留 redirect | `18-comment-login-redirect.png` |
| 19 | 管理员账号不能登录 uni-app | `ADMIN` 或 `SUPER_ADMIN` 调用 `POST /user/auth/login` 被拒绝或前端不保存登录态 | `19-admin-login-rejected.png` |
| 20 | public 接口携带 bad token 仍可访问 | `Authorization: Bearer bad-token` 访问 `/public/articles/page` 不返回 401 | `20-public-bad-token.png` |
| 21 | 图片为空时显示默认图 | 封面、头像或友链 Logo 为空时显示默认图或合理空态 | `21-default-image.png` |

## 4. 截图命名建议

统一使用两位序号加场景名称，便于放入课程报告或答辩材料：

```text
01-backend-start.png
02-public-api-check.png
03-hbuilderx-project.png
04-uniapp-home.png
05-home-search.png
06-category-filter.png
07-tag-filter.png
08-article-detail.png
09-comment-page.png
10-register.png
11-login.png
12-create-comment.png
13-profile.png
14-edit-profile.png
15-avatar-upload.png
16-my-comments.png
17-profile-logged-out.png
18-comment-login-redirect.png
19-admin-login-rejected.png
20-public-bad-token.png
21-default-image.png
```
