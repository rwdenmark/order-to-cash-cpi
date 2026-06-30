import express, { Request, Response } from "express";

const PORT = Number(process.env.PORT ?? 8090);
const SECRET = process.env.WAREHOUSE_SECRET ?? ""; // optional shared secret

const app = express();
app.use(express.text({ type: "*/*", limit: "1mb" }));

interface Capture {
  time: string;
  region: string;
  body: string;
}
const recent: Capture[] = [];

app.post("/:region", (req: Request, res: Response) => {
  // optional: require a shared secret header if WAREHOUSE_SECRET is set
  if (SECRET && req.header("X-Shared-Secret") !== SECRET) {
    return res.status(401).send("unauthorized");
  }

  const region = req.params.region.toUpperCase();
  const entry: Capture = {
    time: new Date().toISOString(),
    region,
    body: typeof req.body === "string" ? req.body : "",
  };

  recent.unshift(entry);
  if (recent.length > 50) recent.pop();

  console.log(`[${entry.time}] ORDER for ${region}:\n${entry.body}\n`);
  res.send(`received by ${region}`);
});

// quick way to view the last captured orders in a browser
app.get("/log", (_req: Request, res: Response) => res.json(recent));

app.listen(PORT, () => console.log(`warehouse receiver listening on :${PORT}`));
