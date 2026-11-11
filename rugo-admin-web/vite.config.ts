/// <reference types="vitest/config" />
import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// Сборка кладётся прямо в статику модуля rugo-admin: Spring отдаёт её по /admin.
export default defineConfig({
    base: '/admin/',
    plugins: [react()],
    build: {
        outDir: '../rugo-admin/src/main/resources/static/admin',
        emptyOutDir: true,
    },
    server: {
        host: true,
        port: 5174,
        strictPort: true,
        // Разрешаем внешние домены туннелей (ngrok, cloudflared)
        allowedHosts: [
            '.ngrok-free.app',
            '.ngrok-free.dev',
            '.ngrok.io',
            '.trycloudflare.com',
        ],
        // Прокси на локальный rugo-admin (API) и rugo-app (фото каталога)
        proxy: {
            '/admin/api': { target: 'http://localhost:8081', changeOrigin: true },
            '/uploads': { target: 'http://localhost:8080', changeOrigin: true },
        },
    },
    test: {
        environment: 'jsdom',
        globals: true,
        setupFiles: ['./src/setupTests.ts'],
        css: true,
    },
})
