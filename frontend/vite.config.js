import { defineConfig, loadEnv } from 'vite'
import react from '@vitejs/plugin-react'

function websocketProxy(target, frontendOrigin) {
  return {
    target,
    changeOrigin: true,
    ws: true,
    configure: (proxy) => {
      proxy.on('proxyReqWs', (proxyReq) => {
        proxyReq.setHeader('origin', frontendOrigin)
      })
    }
  }
}

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const apiTarget = env.VITE_API_BASE_URL || 'http://localhost:8080'
  const wsTarget = new URL(env.VITE_WS_URL || 'ws://localhost:8080/ws')
  const frontendOrigin = env.VITE_FRONTEND_ORIGIN || 'http://localhost:5173'
  const proxy = {
    '/api': {
      target: apiTarget,
      changeOrigin: true,
      headers: { origin: frontendOrigin }
    },
    '/ws': websocketProxy(wsTarget.origin, frontendOrigin)
  }

  return {
    plugins: [react()],
    server: {
      host: true,
      allowedHosts: [
    'specific-volume-sheriff-neon.trycloudflare.com'
  ],
      proxy
    },
    preview: { host: true, proxy },
    test: { environment: 'jsdom', setupFiles: './src/test/setup.js' }
  }
})
