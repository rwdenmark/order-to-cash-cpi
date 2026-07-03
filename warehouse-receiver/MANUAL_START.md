# Manual start for SAP CPI testing

The warehouse-receiver is not auto-started on boot. Bring it up by hand when you're testing the Order-to-Cash CPI flow. Run these inside Ubuntu (`wsl -d Ubuntu`).

## Bring it up

```bash
# 1. Start the service (runs on :8090)
sudo systemctl start warehouse-receiver

# verify locally
curl -X POST localhost:8090/west -d 'hi'          # -> received by WEST

# 2. Publish it through Tailscale Funnel at /warehouse
tailscale funnel --bg --set-path /warehouse localhost:8090

# verify it's mapped and reachable from the public URL CPI uses
tailscale funnel status                            # should list /warehouse -> http://localhost:8090
curl -X POST https://<your-funnel-host>/warehouse/west -d 'hi'   # -> received by WEST
```

Tailscale strips the `/warehouse` prefix, so CPI posts to `https://<your-funnel-host>/warehouse/<region>` and the app receives `/<region>` (matches `app.post("/:region")`).

## Take it down when done

```bash
sudo systemctl stop warehouse-receiver
tailscale funnel reset      # clears the /warehouse funnel mapping
```

## Notes

- A clean 404 from `/warehouse/...` usually means the funnel mapping is missing (re-run step 2). A 502 or connection error usually means the service on 8090 isn't running (re-run step 1).
- To enable auto-start on boot instead, run `sudo systemctl enable warehouse-receiver`.
