const adminApiPrefixes = [
  '^/admin/auth($|/)',
  '^/admin/article($|/)',
  '^/admin/category($|/)',
  '^/admin/tag($|/)',
  '^/admin/comment($|/)',
  '^/admin/dashboard/summary($|/)',
  '^/admin/files($|/)',
  '^/admin/friend-link($|/)',
  '^/admin/log($|/)',
  '^/admin/operation-log($|/)',
  '^/admin/profile/(me|password)($|/)',
  '^/admin/user($|/)',
  '^/admin/role($|/)',
  '^/user/auth($|/)',
  '^/user/articles($|/)',
  '^/user/categories($|/)',
  '^/user/tags($|/)',
  '^/user/comments($|/)',
  '^/user/dashboard($|/)',
  '^/user/files($|/)',
  '^/user/profile($|/)',
  '^/uploads($|/)'
];

export function createAdminProxyConfig(target) {
  return Object.fromEntries(
    adminApiPrefixes.map((prefix) => [
      prefix,
      {
        target,
        changeOrigin: true
      }
    ])
  );
}
