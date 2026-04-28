// Take a series of timestamped screenshots while an ad plays, so we can visually
// verify the look of each scene without watching the whole 60s in real time.
import { chromium } from 'playwright';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));

const NAME = process.argv[2];
const TIMES = process.argv.slice(3).map(Number); // seconds into the ad

if (!NAME || TIMES.length === 0) {
  console.error('Usage: node scripts/snap.mjs <investors|clients> <t1> <t2> ...');
  process.exit(1);
}

const outDir = path.resolve(__dirname, '..', 'snapshots');
fs.mkdirSync(outDir, { recursive: true });

const browser = await chromium.launch({ headless: true });
const context = await browser.newContext({
  viewport: { width: 1920, height: 1080 },
  deviceScaleFactor: 1,
});
const page = await context.newPage();
await page.goto(`http://localhost:5180/#/${NAME}`, { waitUntil: 'networkidle' });

let prev = 0;
for (const t of TIMES) {
  const wait = Math.max(0, t - prev) * 1000;
  await page.waitForTimeout(wait);
  prev = t;
  const file = path.join(outDir, `${NAME}-t${t}.png`);
  await page.screenshot({ path: file });
  console.log(`✓  ${file}`);
}

await browser.close();
