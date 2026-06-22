export function createAdminProxyConfig(target) {
  return {
    '^/api/(admin|user|public)($|/)': {
      target,
      changeOrigin: true,
      rewrite: (path) => path.replace(/^\/api/, '')
    },
    '^/uploads($|/)': {
      target,
      changeOrigin: true
    }
  };
}
