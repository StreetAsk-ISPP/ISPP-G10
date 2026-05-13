/**
 * High-quality ad recorder — deterministic frame-by-frame capture.
 *
 * Strategy: launch headless Chromium. Before any page script runs, inject an
 * `init script` that overrides the page's clock (Date.now, performance.now,
 * requestAnimationFrame, setTimeout, setInterval) so they only advance when
 * we explicitly call `window.__tick(ms)` from the recorder. Then we drive the
 * timeline one 1/60 s tick at a time, taking a JPEG screenshot per tick,
 * which ffmpeg encodes to a 60 fps H.264 MP4.
 *
 * Benefits:
 *   - Perfectly smooth 60 fps output regardless of host GPU speed.
 *   - Independent of host resolution, DPI scaling, or window focus.
 *   - No "don't touch the mouse" requirement.
 *
 * Trade-off: real-world recording takes ~2–4× the video length (capture
 * + JPEG serialization). A 60 s ad takes ~3 min to render.
 *
 * Requirements:
 *   - ffmpeg in PATH  (winget install ffmpeg)
 *   - Playwright Chromium installed  (`npx playwright install chromium`)
 *   - Dev server running on port 5180  (`npm run dev`)
 *
 * Usage:
 *   node scripts/record.mjs <investors|clients>
 *
 * Environment flags:
 *   DEBUG_FFMPEG=1   stream ffmpeg's stderr (useful when the encoder fails)
 *   FPS=30           override the default 60 fps for faster captures
 *   PORT=5181        override the dev-server port (default 5180)
 */

import { chromium } from 'playwright';
import { spawn, execSync } from 'node:child_process';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import os from 'node:os';

const __dirname = path.dirname(fileURLToPath(import.meta.url));

// ---------------------------------------------------------------------------
// Config
// ---------------------------------------------------------------------------

const NAME = process.argv[2];
if (!['investors', 'clients'].includes(NAME)) {
  console.error('Usage: node scripts/record.mjs <investors|clients>');
  process.exit(1);
}

const DURATIONS = { investors: 60, clients: 60 };  // seconds (matches SCENES arrays)
const BUFFER_S  = 1;
const WIDTH     = 1920;
const HEIGHT    = 1080;
const FPS       = Number(process.env.FPS) || 60;
const FRAME_MS  = 1000 / FPS;
const CRF       = 14;
const PRESET    = 'slow';
const JPEG_Q    = 92;
const PORT      = Number(process.env.PORT) || 5180;

// ---------------------------------------------------------------------------
// ffmpeg discovery
// ---------------------------------------------------------------------------

const FFMPEG_FALLBACKS = [
  path.join(os.homedir(), 'AppData/Local/Microsoft/WinGet/Packages/Gyan.FFmpeg_Microsoft.Winget.Source_8wekyb3d8bbwe/ffmpeg-8.1-full_build/bin/ffmpeg.exe'),
  path.join(os.homedir(), 'AppData/Local/CapCut/Apps/8.4.0.3562/ffmpeg.exe'),
];

function resolveFfmpeg() {
  try { execSync('ffmpeg -version', { stdio: 'pipe' }); return 'ffmpeg'; }
  catch { /* not in PATH */ }
  for (const p of FFMPEG_FALLBACKS) {
    if (fs.existsSync(p)) return p;
  }
  return null;
}

const FFMPEG_BIN = resolveFfmpeg();

if (!FFMPEG_BIN) {
  console.error([
    '✗  ffmpeg not found in PATH or known locations.',
    '   Install it with:',
    '     Windows : winget install ffmpeg',
    '     macOS   : brew install ffmpeg',
    '     Linux   : sudo apt install ffmpeg',
    '   Then restart your terminal.',
  ].join('\n'));
  process.exit(1);
}

if (FFMPEG_BIN !== 'ffmpeg') {
  console.log(`ℹ  Using ffmpeg at: ${FFMPEG_BIN}`);
}

// ---------------------------------------------------------------------------
// Paths
// ---------------------------------------------------------------------------

const outDir   = path.resolve(__dirname, '..', 'recordings');
fs.mkdirSync(outDir, { recursive: true });

const ts          = new Date().toISOString().replace(/[:.]/g, '-');
const finalMp4    = path.join(outDir, `${NAME}-${ts}.mp4`);
const totalFrames = (DURATIONS[NAME] + BUFFER_S) * FPS;
const url         = `http://localhost:${PORT}/?record#/${NAME}`;

console.log(`▶  Deterministic capture: ${totalFrames} frames @ ${FPS}fps  (${WIDTH}×${HEIGHT})`);
console.log(`   Output: recordings/${path.basename(finalMp4)}`);

// ---------------------------------------------------------------------------
// Launch headless Chromium with a clock-override init script
// ---------------------------------------------------------------------------

const browser = await chromium.launch({
  headless: true,
  args: [
    '--enable-gpu',
    '--ignore-gpu-blocklist',
    '--hide-scrollbars',
    '--no-first-run',
    '--no-default-browser-check',
    '--disable-backgrounding-occluded-windows',
    '--disable-renderer-backgrounding',
    '--disable-background-timer-throttling',
  ],
});

const context = await browser.newContext({
  viewport: { width: WIDTH, height: HEIGHT },
  deviceScaleFactor: 1,
});

// Inject the clock-override script. Runs in the page BEFORE any user script.
// Only activates when ?record is present in the URL, so normal browsing of
// the dev server is unaffected.
await context.addInitScript(() => {
  if (!new URLSearchParams(location.search).has('record')) return;

  // ---- Disable Web Animations API ----
  // Framer Motion v11 uses Element.prototype.animate (WAAPI) for opacity /
  // transform when available. WAAPI is timed by Chromium's compositor in
  // REAL time, ignoring our rAF / performance.now overrides.
  //
  // FM's capability probe is `Object.hasOwnProperty.call(Element.prototype,
  // "animate")` — it checks for the property's existence, not whether it
  // works. So we have to actually DELETE it from the prototype to force FM
  // onto its JS animator fallback (which uses our overridden rAF).
  delete Element.prototype.animate;
  delete HTMLElement.prototype.animate;

  let vt = 0;  // virtual time in ms since page load

  const rafQueue = new Map(); // id -> callback(virtualTime)
  let nextRafId = 1;

  // Timer = { fireAt, cb, args, repeat, interval }
  const timers = new Map();
  let nextTimerId = 1;

  // Overrides — keep originals around just in case anything internal needs them.
  performance.now = () => vt;
  Date.now = () => Math.floor(vt) + 1700000000000;  // arbitrary fixed epoch

  window.requestAnimationFrame = (cb) => {
    const id = nextRafId++;
    rafQueue.set(id, cb);
    return id;
  };
  window.cancelAnimationFrame = (id) => { rafQueue.delete(id); };

  window.setTimeout = (cb, delay = 0, ...args) => {
    const id = nextTimerId++;
    timers.set(id, { fireAt: vt + (Number(delay) || 0), cb, args, repeat: false });
    return id;
  };
  window.clearTimeout = (id) => { timers.delete(id); };

  window.setInterval = (cb, interval = 0, ...args) => {
    const id = nextTimerId++;
    const i = Number(interval) || 0;
    timers.set(id, { fireAt: vt + i, cb, args, repeat: true, interval: i });
    return id;
  };
  window.clearInterval = (id) => { timers.delete(id); };

  // Recorder API: advance the virtual clock by `deltaMs`, firing any due
  // timers and then all pending rAF callbacks (the same order a real browser
  // tick would).
  window.__tick = (deltaMs) => {
    const target = vt + deltaMs;
    vt = target;

    // Fire all timers whose deadline has passed.
    let fired;
    do {
      fired = false;
      const due = [];
      for (const [id, t] of timers) {
        if (t.fireAt <= vt) due.push([id, t]);
      }
      due.sort((a, b) => a[1].fireAt - b[1].fireAt);
      for (const [id, t] of due) {
        try { t.cb(...t.args); } catch (e) { console.error('[timer]', e); }
        fired = true;
        if (t.repeat) {
          t.fireAt += t.interval;
          if (t.fireAt > vt) timers.set(id, t);
        } else {
          timers.delete(id);
        }
      }
    } while (fired && timers.size > 0 && [...timers.values()].some(t => t.fireAt <= vt));

    // Fire pending rAF callbacks in submission order (matches spec).
    const callbacks = Array.from(rafQueue.entries());
    rafQueue.clear();
    for (const [_id, cb] of callbacks) {
      try { cb(vt); } catch (e) { console.error('[rAF]', e); }
    }
  };

  window.__virtualTime = () => vt;
});

const page = await context.newPage();

try {
  await page.goto(url, { waitUntil: 'load' });
} catch (err) {
  console.error(`✗  Could not reach the dev server at ${url}.`);
  console.error(`   Is \`npm run dev\` running? If it auto-picked another port,`);
  console.error(`   re-run with e.g.  $env:PORT=5181; npm run record:${NAME}`);
  if (process.env.DEBUG_FFMPEG) console.error('   Underlying error:', err?.message);
  await browser.close();
  process.exit(1);
}

// Sanity check: did the override actually attach?
const overrideActive = await page.evaluate(() => typeof window.__tick === 'function');
if (!overrideActive) {
  console.error('✗  Clock override did not attach. Make sure the URL includes ?record.');
  await browser.close();
  process.exit(1);
}

// Pump a couple of microtask cycles so React's mount effects queue their rAFs
// (no virtual time advances here — we just let synchronous JS settle).
await page.evaluate(() => new Promise((r) => queueMicrotask(() => queueMicrotask(r))));

// ---------------------------------------------------------------------------
// Spawn ffmpeg encoder (reads MJPEG frames from stdin)
// ---------------------------------------------------------------------------

console.log(`▶  Starting ffmpeg (libx264 CRF ${CRF}, preset ${PRESET}, tune animation)`);

const ffmpegArgs = [
  '-y',
  '-f', 'image2pipe',
  '-vcodec', 'mjpeg',
  '-framerate', String(FPS),
  '-i', '-',
  '-vcodec', 'libx264',
  '-pix_fmt', 'yuv420p',
  '-crf', String(CRF),
  '-preset', PRESET,
  '-tune', 'animation',
  '-r', String(FPS),
  '-movflags', '+faststart',
  finalMp4,
];

const ffmpeg = spawn(FFMPEG_BIN, ffmpegArgs, { stdio: ['pipe', 'ignore', 'pipe'] });

ffmpeg.stderr.on('data', (chunk) => {
  if (process.env.DEBUG_FFMPEG) process.stderr.write(chunk);
});

let ffmpegFailed = false;
ffmpeg.on('error', (err) => {
  console.error('\n✗  ffmpeg error:', err.message);
  ffmpegFailed = true;
});

// ---------------------------------------------------------------------------
// Frame loop
// ---------------------------------------------------------------------------

const tStart = Date.now();
console.log(`▶  Rendering frames …`);

for (let i = 0; i < totalFrames; i++) {
  if (ffmpegFailed) break;

  // Advance the page's virtual clock by one frame. This fires due timers and
  // all pending rAF callbacks inside the page synchronously.
  await page.evaluate((dt) => window.__tick(dt), FRAME_MS);

  // Give the renderer a microtask slice to commit any state updates from rAF
  // callbacks (React batches setState through MessageChannel).
  await page.evaluate(() => new Promise((r) => queueMicrotask(r)));

  const buf = await page.screenshot({ type: 'jpeg', quality: JPEG_Q });

  if (!ffmpeg.stdin.write(buf)) {
    await new Promise((resolve) => ffmpeg.stdin.once('drain', resolve));
  }

  if ((i + 1) % FPS === 0) {
    const secVirtual = (i + 1) / FPS;
    const secReal = ((Date.now() - tStart) / 1000).toFixed(1);
    const fps = (i + 1) / Math.max(0.1, (Date.now() - tStart) / 1000);
    process.stdout.write(
      `\r   ${String(secVirtual).padStart(2)}s rendered  ·  ${secReal}s elapsed  ·  ${fps.toFixed(1)} capture-fps     `
    );
  }
}

process.stdout.write('\n');

// ---------------------------------------------------------------------------
// Flush ffmpeg and close everything
// ---------------------------------------------------------------------------

ffmpeg.stdin.end();
await new Promise((resolve) => ffmpeg.on('close', resolve));

await context.close();
await browser.close();

if (fs.existsSync(finalMp4) && fs.statSync(finalMp4).size > 0) {
  const mb = (fs.statSync(finalMp4).size / 1_048_576).toFixed(1);
  console.log(`✓  Saved recordings/${path.basename(finalMp4)}  (${mb} MB)`);
} else {
  console.error('✗  Output missing or empty. Re-run with DEBUG_FFMPEG=1 to see encoder errors.');
  process.exit(1);
}
