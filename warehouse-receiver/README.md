# warehouse-receiver

A tiny TypeScript/Express service that stands in for downstream "warehouse" systems in the [Order to Cash on SAP CPI](../) demo. The CPI Router posts each order here based on its region, and the service logs it and returns an acknowledgement.

It's self-hosted and exposed to the public internet (so SAP CPI's cloud runtime can reach it) via **Tailscale Funnel**, which strips the `/warehouse` path prefix, so the routes are defined at root.

## Endpoints

| Method | Path | Purpose |
|---|---|---|
| `POST` | `/:region` | Receive an order for a region (e.g. `west`, `east`). Logs it and returns `received by <REGION>`. |
| `GET` | `/log` | Returns the last 50 captured orders as JSON. |

Public URLs through Tailscale Funnel look like `https://<machine>.<tailnet>.ts.net/warehouse/west`, which the funnel maps to `/west` here.

## Run locally

```bash
npm install
npm run build          # tsc -> dist/server.js
npm start              # node dist/server.js  (listens on :8090)
```

## Configuration

| Env var | Default | Purpose |
|---|---|---|
| `PORT` | `8090` | Port to listen on. |
| `WAREHOUSE_SECRET` | _(unset)_ | If set, requests must send a matching `X-Shared-Secret` header, else 401. |

## Deploy (systemd)

Run it as a service with the included `warehouse-receiver.service` unit (adjust `WorkingDirectory`, `ExecStart`, and `User` to your paths), then expose it:

```bash
sudo systemctl enable --now warehouse-receiver
sudo tailscale funnel --bg --set-path=/warehouse 8090
```

## Notes

- WSL build note: use Linux-native Node inside WSL (the Windows Node on the PATH breaks native installs and `npm run`); if `npm run build` misbehaves, compile directly with `/usr/bin/node ./node_modules/typescript/bin/tsc`.
- This is a demo receiver, not a production service.
