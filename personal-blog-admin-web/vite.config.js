import { fileURLToPath, URL } from 'node:url';

import vue from '@vitejs/plugin-vue';
import { defineConfig, loadEnv } from 'vite';

import { createAdminProxyConfig } from './vite.proxy.js';

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '');
  const apiTarget = env.VITE_API_PROXY_TARGET || 'http://localhost:8081';

  return {
    plugins: [vue()],
    resolve: {
      alias: {
        '@': fileURLToPath(new URL('./src', import.meta.url))
      }
    },
    server: {
      port: 5173,
      proxy: createAdminProxyConfig(apiTarget)
    },
    build: {
      chunkSizeWarningLimit: 1100,
      rollupOptions: {
        output: {
          manualChunks(id) {
            if (id.includes('node_modules/element-plus') || id.includes('node_modules/@element-plus')) {
              return 'element-plus';
            }

            if (id.includes('node_modules/vue') || id.includes('node_modules/pinia')) {
              return 'vue-vendor';
            }

            if (id.includes('node_modules/axios')) {
              return 'axios';
            }

            return undefined;
          }
        }
      }
    },
    test: {
      environment: 'jsdom',
      globals: true
    }
  };
});
