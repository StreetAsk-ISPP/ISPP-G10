// Record one of the ads to a .webm file using Playwright's built-in video recorder.
// Usage: node scripts/record.mjs <investors|clients>
//
// Pre-req: dev server running on port 5180 (`npm run dev`).
// Output: ./recordings/<name>-<timestamp>.webm at 1920x1080.
//
// Total ad durations are derived from the SCENES array in each ad component.
// We add a small buffer so the final scene has time to settle before stopping.

import { chromium } from 'playwright';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));

const NAME = process.argv[2];
if (!['investors', 'clients'].includes(NAME)) {
  console.error('Usage: node scripts/record.mjs <investors|clients>');
  process.exit(1);
}

// Match the totals defined in src/scenes/*Ad.jsx (in seconds).
const DURATIONS = { investors: 60, clients: 60 };
const BUFFER_SECONDS = 1.5;

const outDir = path.resolve(__dirname, '..', 'recordings');
fs.mkdirSync(outDir, { recursive: true });

const url = `http://localhost:5180/#/${NAME}`;
const totalMs = (DURATIONS[NAME] + BUFFER_SECONDS) * 1000;

console.log(`▶  Recording ${NAME} from ${url} for ${totalMs / 1000}s ...`);

const browser = await chromium.launch({ headless: true });
const context = await browser.newContext({
  viewport: { width: 1920, height: 1080 },
  recordVideo: { dir: outDir, size: { width: 1920, height: 1080 } },
  deviceScaleFactor: 1,
});
const page = await context.newPage();

try {
  await page.goto(url, { waitUntil: 'networkidle' });
} catch (err) {
  console.error('✗  Could not load the dev server. Is `npm run dev` running on port 5180?');
  await context.close();
  await browser.close();
  process.exit(1);
}

// Give fonts a moment to settle so the first frame is not flashed un-styled.
await page.waitForTimeout(500);

await page.waitForTimeout(totalMs);

await context.close();
await browser.close();

// Playwright names the video randomly; rename it.
const files = fs.readdirSync(outDir).filter(f => f.endsWith('.webm'));
files.sort((a, b) => fs.statSync(path.join(outDir, b)).mtimeMs - fs.statSync(path.join(outDir, a)).mtimeMs);
const latest = files[0];
if (latest) {
  const ts = new Date().toISOString().replace(/[:.]/g, '-');
  const finalName = `${NAME}-${ts}.webm`;
  fs.renameSync(path.join(outDir, latest), path.join(outDir, finalName));
  console.log(`✓  Saved recordings/${finalName}`);
}
