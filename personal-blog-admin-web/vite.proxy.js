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
