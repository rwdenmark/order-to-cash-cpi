import express, { Request, Response } from "express";

const PORT = Number(process.env.PORT ?? 8090);
const SECRET = process.env.WAREHOUSE_SECRET ?? ""; // optional shared secret

// only regions the CPI Router actually routes to
const REGIONS = new Set(["WEST", "EAST"]);

const app = express();
app.use(express.text({ type: "*/*", limit: "1mb" }));

interface Capture {
  time: string;
  region: string;
  body: string;
}
const recent: Capture[] = [];

// require a shared secret header if WAREHOUSE_SECRET is set (optional)
function authorized(req: Request): boolean {
  return !SECRET || req.header("X-Shared-Secret") === SECRET;
}

app.post("/:region", (req: Request, res: Response) => {
  if (!authorized(req)) {
    return res.status(401).send("unauthorized");
  }

  const region = req.params.region.toUpperCase();
  if (!REGIONS.has(region)) {
    return res.status(404).send("unknown region");
  }

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
app.get("/log", (req: Request, res: Response) => {
  if (!authorized(req)) {
    return res.status(401).send("unauthorized");
  }
  res.json(recent);
});

// only listen when run directly so tests can import the app
if (require.main === module) {
  app.listen(PORT, () => console.log(`warehouse receiver listening on :${PORT}`));
}

export default app;
