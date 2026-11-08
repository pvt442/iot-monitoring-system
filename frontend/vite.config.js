import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// Dev server chạy ở cổng 5173 và chuyển tiếp /api sang Backend Spring Boot (8080) -> không bị lỗi CORS.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': { target: process.env.VITE_PROXY_TARGET || 'http://localhost:8080', changeOrigin: true },
    },
  },
});
