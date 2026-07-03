// runs with no WAREHOUSE_SECRET set, auth checks live in server.secret.test.ts
import test from "node:test";
import assert from "node:assert/strict";
import app from "./server";

function start(): Promise<{ base: string; close: () => void }> {
  return new Promise((resolve) => {
    const server = app.listen(0, () => {
      const addr = server.address();
      const port = typeof addr === "object" && addr ? addr.port : 0;
      resolve({ base: `http://127.0.0.1:${port}`, close: () => server.close() });
    });
  });
}

test("POST to a known region captures and acks", async () => {
  const { base, close } = await start();
  try {
    const res = await fetch(`${base}/west`, { method: "POST", body: "<Order/>" });
    assert.equal(res.status, 200);
    assert.equal(await res.text(), "received by WEST");
  } finally {
    close();
  }
});

test("POST to an unknown region returns 404", async () => {
  const { base, close } = await start();
  try {
    const res = await fetch(`${base}/north`, { method: "POST", body: "<Order/>" });
    assert.equal(res.status, 404);
    assert.equal(await res.text(), "unknown region");
  } finally {
    close();
  }
});

test("GET /log returns captured entries", async () => {
  const { base, close } = await start();
  try {
    await fetch(`${base}/east`, { method: "POST", body: "<Order id='1'/>" });
    const res = await fetch(`${base}/log`);
    assert.equal(res.status, 200);
    const entries = (await res.json()) as { region: string; body: string }[];
    assert.ok(entries.length >= 1);
    assert.equal(entries[0].region, "EAST");
    assert.equal(entries[0].body, "<Order id='1'/>");
  } finally {
    close();
  }
});
