export const dashboardStats = [
  {
    label: '文章总数',
    value: '1,284',
    icon: 'Document',
    tone: 'blue'
  },
  {
    label: '分类总数',
    value: '24',
    icon: 'Connection',
    tone: 'green'
  },
  {
    label: '评论总数',
    value: '8,421',
    icon: 'ChatLineSquare',
    tone: 'violet'
  },
  {
    label: '待处理评论',
    value: '12',
    icon: 'Clock',
    tone: 'orange'
  }
];

export const latestArticles = [
  {
    title: '深入理解 React 18 并发渲染机制',
    date: '2024-04-10',
    category: '前端开发',
    views: 1250,
    status: '已发布'
  },
  {
    title: 'Spring Boot 3.0 新特性全解析',
    date: '2024-04-09',
    category: '后端开发',
    views: 890,
    status: '已发布'
  },
  {
    title: '微服务架构下的分布式事务解决方案',
    date: '2024-04-05',
    category: '架构设计',
    views: 2100,
    status: '已发布'
  },
  {
    title: 'MySQL 索引优化实战指南',
    date: '2024-04-01',
    category: '数据库',
    views: 1540,
    status: '草稿'
  },
  {
    title: 'Kubernetes 集群部署踩坑记录',
    date: '2024-03-28',
    category: '运维部署',
    views: 760,
    status: '已发布'
  }
];

export const latestComments = [
  {
    author: '张三',
    avatar: '张',
    time: '10分钟前',
    content: '这篇文章写得太好了，解决了我很久以来的疑惑！',
    article: '深入理解 React 18...',
    status: '待审核'
  },
  {
    author: '李四',
    avatar: '李',
    time: '2小时前',
    content: '期待博主更新更多 Spring 相关的实战文章。',
    article: 'Spring Boot 3.0...',
    status: ''
  },
  {
    author: '王五',
    avatar: '王',
    time: '1天前',
    content: '关于 Seata 的配置部分好像有点问题，建议检查一下。',
    article: '微服务架构下的...',
    status: ''
  }
];

export const operationLogs = [
  {
    user: 'admin',
    action: '执行了 登录系统',
    time: '2024-04-12 09:00:12',
    ip: '192.168.1.100',
    state: 'success'
  },
  {
    user: 'admin',
    action: '执行了 修改文章',
    time: '2024-04-11 15:30:45',
    ip: '192.168.1.100',
    state: 'success'
  },
  {
    user: 'test_user',
    action: '执行了 尝试登录',
    time: '2024-04-11 14:20:10',
    ip: '10.0.0.52',
    state: 'danger'
  },
  {
    user: 'admin',
    action: '执行了 删除评论',
    time: '2024-04-10 11:15:33',
    ip: '192.168.1.100',
    state: 'success'
  }
];
