const adminApiPrefixes = [
  '^/admin/auth($|/)',
  '^/admin/article($|/)',
  '^/admin/category($|/)',
  '^/admin/tag($|/)',
  '^/admin/comment($|/)',
  '^/admin/friend-link($|/)',
  '^/admin/log($|/)',
  '^/admin/user($|/)',
  '^/admin/role($|/)'
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
