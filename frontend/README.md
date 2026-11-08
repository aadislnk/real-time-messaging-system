# Sandesh frontend

The frontend is a React/Vite client for the Sandesh REST and STOMP APIs.

## Development

```bash
npm install
npm run dev
```

The local defaults target `http://localhost:8080` for REST and
`ws://localhost:8080/ws` for STOMP. Copy `.env.example` to `.env` to
override them:

```dotenv
VITE_API_BASE_URL=http://localhost:8080
VITE_WS_URL=ws://localhost:8080/ws
VITE_FRONTEND_ORIGIN=http://localhost:5173
```

## Validation and production build

```bash
npm test
npm run build
npm run preview
```

The backend must be running with its configured CORS/WebSocket origins
including the frontend development or deployment origin.

## Cloudflare Quick Tunnel testing

Start a backend Quick Tunnel and use its HTTPS hostname for REST and its
`wss://` equivalent for STOMP:

```dotenv
VITE_API_BASE_URL=https://your-backend.trycloudflare.com
VITE_WS_URL=wss://your-backend.trycloudflare.com/ws
VITE_FRONTEND_ORIGIN=http://localhost:5173
```

Restart Vite after changing `.env`, then share the Vite URL shown by
`npm run dev -- --host` with your friend. If the frontend itself is shared
through a Quick Tunnel, set `VITE_FRONTEND_ORIGIN` to that HTTPS frontend
hostname and configure the backend to allow that origin. Never put JWT
secrets or other credentials in `.env`; `VITE_*` values are public.
