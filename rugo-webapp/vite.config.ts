/// <reference types="vitest/config" />
import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
    plugins: [react()],
    server: {
        host: true,
        port: 5173,
        strictPort: true,
        // Разрешаем внешние домены туннелей (ngrok, cloudflared)
        allowedHosts: [
            '.ngrok-free.app',
            '.ngrok-free.dev',
            '.ngrok.io',
            '.trycloudflare.com',
        ],
        // Прокси на локальный бэкенд, чтобы фронт и API были на одном origin
        proxy: {
            '/api': { target: 'http://localhost:8080', changeOrigin: true },
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