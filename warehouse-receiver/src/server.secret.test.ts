// separate file so the secret is set before the server module loads,
// node --test runs each file in its own process
process.env.WAREHOUSE_SECRET = "test-secret";

import test from "node:test";
import assert from "node:assert/strict";

function start(app: import("express").Express): Promise<{ base: string; close: () => void }> {
  return new Promise((resolve) => {
    const server = app.listen(0, () => {
      const addr = server.address();
      const port = typeof addr === "object" && addr ? addr.port : 0;
      resolve({ base: `http://127.0.0.1:${port}`, close: () => server.close() });
    });
  });
}

test("secret required on POST and GET /log when WAREHOUSE_SECRET is set", async () => {
  const { default: app } = await import("./server");
  const { base, close } = await start(app);
  try {
    const noHeaderPost = await fetch(`${base}/west`, { method: "POST", body: "<Order/>" });
    assert.equal(noHeaderPost.status, 401);

    const noHeaderLog = await fetch(`${base}/log`);
    assert.equal(noHeaderLog.status, 401);

    const goodPost = await fetch(`${base}/west`, {
      method: "POST",
      body: "<Order/>",
      headers: { "X-Shared-Secret": "test-secret" },
    });
    assert.equal(goodPost.status, 200);
    assert.equal(await goodPost.text(), "received by WEST");

    const goodLog = await fetch(`${base}/log`, {
      headers: { "X-Shared-Secret": "test-secret" },
    });
    assert.equal(goodLog.status, 200);
    const entries = (await goodLog.json()) as { region: string }[];
    assert.equal(entries.length, 1);
    assert.equal(entries[0].region, "WEST");
  } finally {
    close();
  }
});

test("wrong secrets are rejected, including a same-length one", async () => {
  const { default: app } = await import("./server");
  const { base, close } = await start(app);
  try {
    const wrong = await fetch(`${base}/west`, {
      method: "POST",
      body: "<Order/>",
      headers: { "X-Shared-Secret": "wrong" },
    });
    assert.equal(wrong.status, 401);

    // same length as test-secret, exercises the timingSafeEqual path
    const sameLength = await fetch(`${base}/west`, {
      method: "POST",
      body: "<Order/>",
      headers: { "X-Shared-Secret": "test-secreX" },
    });
    assert.equal(sameLength.status, 401);
  } finally {
    close();
  }
});
