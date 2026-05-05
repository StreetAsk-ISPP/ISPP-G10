/**
 * High-quality ad recorder — Playwright (headed) + ffmpeg screen capture.
 *
 * Strategy: launch a real Chromium window with GPU compositing enabled so
 * Framer Motion animations render at full frame-rate, then capture that
 * window with ffmpeg at 60 fps / high-bitrate H.264.  Output is an MP4
 * that can be uploaded directly to LinkedIn / social platforms.
 *
 * Requirements:
 *   - ffmpeg in PATH  (winget install ffmpeg  OR  choco install ffmpeg)
 *   - Dev server running on port 5180  (`npm run dev`)
 *
 * Usage:
 *   node scripts/record.mjs <investors|clients>
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

const DURATIONS   = { investors: 60, clients: 60 };   // seconds, from scene arrays
const BUFFER_S    = 2;                                  // extra tail so last frame settles
const FPS         = 60;
const WIDTH       = 1920;
const HEIGHT      = 1080;
const CRF         = 14;   // 0 = lossless, 18 = visually lossless, 23 = default; 14 = very high
const PRESET      = 'slow'; // encoding speed vs compression trade-off (slow = smaller & better)

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

// Known fallback locations when ffmpeg is not in PATH (winget, CapCut, etc.)
const FFMPEG_FALLBACKS = [
  // winget install ffmpeg
  path.join(os.homedir(), 'AppData/Local/Microsoft/WinGet/Packages/Gyan.FFmpeg_Microsoft.Winget.Source_8wekyb3d8bbwe/ffmpeg-8.1-full_build/bin/ffmpeg.exe'),
  // CapCut bundles its own ffmpeg
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

/** Returns the best ffmpeg input args for capturing a window/screen on this OS. */
function ffmpegCaptureArgs(x, y) {
  const platform = os.platform();

  if (platform === 'win32') {
    // gdigrab can target the whole desktop; we crop to the browser window via
    // an ffmpeg crop filter instead of trying to grab a named window title.
    return [
      '-f', 'gdigrab',
      '-framerate', String(FPS),
      '-offset_x', String(x),
      '-offset_y', String(y),
      '-video_size', `${WIDTH}x${HEIGHT}`,
      '-draw_mouse', '0',
      '-i', 'desktop',
    ];
  }

  if (platform === 'darwin') {
    // avfoundation: "1" is typically the primary display
    return [
      '-f', 'avfoundation',
      '-framerate', String(FPS),
      '-capture_cursor', '0',
      '-i', `1:none`,
      '-vf', `crop=${WIDTH}:${HEIGHT}:${x}:${y}`,
    ];
  }

  // Linux — X11
  return [
    '-f', 'x11grab',
    '-framerate', String(FPS),
    '-video_size', `${WIDTH}x${HEIGHT}`,
    '-i', `${process.env.DISPLAY || ':0'}+${x},${y}`,
  ];
}

// ---------------------------------------------------------------------------
// Main
// ---------------------------------------------------------------------------

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

const outDir = path.resolve(__dirname, '..', 'recordings');
fs.mkdirSync(outDir, { recursive: true });

const ts       = new Date().toISOString().replace(/[:.]/g, '-');
const outFile  = path.join(outDir, `${NAME}-${ts}.mp4`);
const totalMs  = (DURATIONS[NAME] + BUFFER_S) * 1000;
const url      = `http://localhost:5180/#/${NAME}`;

console.log(`▶  Launching browser for ${NAME} (${DURATIONS[NAME]}s + ${BUFFER_S}s buffer) …`);

// Launch Chromium with GPU compositing so CSS/canvas animations are smooth.
const browser = await chromium.launch({
  headless: false,
  args: [
    // Position & size — top-left corner so gdigrab offset is 0,0
    '--window-position=0,0',
    `--window-size=${WIDTH},${HEIGHT}`,

    // Force GPU compositing (prevents software-rendered janky frames)
    '--enable-gpu',
    '--enable-gpu-rasterization',
    '--enable-zero-copy',
    '--ignore-gpu-blocklist',
    '--disable-gpu-sandbox',          // needed on some Windows setups

    // Smooth animations — disable vsync throttle in background
    '--disable-backgrounding-occluded-windows',
    '--disable-renderer-backgrounding',
    '--disable-background-timer-throttling',

    // Remove chrome UI chrome so the viewport fills the window exactly
    '--app=about:blank',              // kiosk-like: no tab bar / address bar
    '--disable-infobars',
    '--no-first-run',
    '--no-default-browser-check',
    '--hide-scrollbars',

    // Keep 60 fps even when the window loses focus
    '--force-device-scale-factor=1',
  ],
});

const context = await browser.newContext({
  viewport: { width: WIDTH, height: HEIGHT },
  deviceScaleFactor: 1,
});
const page = await context.newPage();

// Navigate and wait for fonts / first paint to settle.
try {
  await page.goto(url, { waitUntil: 'networkidle' });
} catch {
  console.error('✗  Could not reach the dev server. Is `npm run dev` running on port 5180?');
  await browser.close();
  process.exit(1);
}

// Extra settle time: fonts + first animation frame.
await page.waitForTimeout(800);

// ---------------------------------------------------------------------------
// Start ffmpeg capture
// ---------------------------------------------------------------------------

// On Windows with --app= the window starts at 0,0 but Chrome may add a tiny
// title-bar.  We grab from (0,0) and size exactly to WIDTH×HEIGHT — the
// --app flag removes the address bar so the viewport IS the window content.
const captureArgs = ffmpegCaptureArgs(0, 0);

const ffmpegArgs = [
  '-y',                              // overwrite output without asking
  ...captureArgs,

  // Video codec — libx264 high-quality
  '-vcodec', 'libx264',
  '-pix_fmt', 'yuv420p',            // broadest playback compatibility
  '-crf', String(CRF),
  '-preset', PRESET,
  '-tune', 'animation',             // optimises for smooth gradients & flat areas

  // Colour / framerate passthrough
  '-r', String(FPS),
  '-movflags', '+faststart',        // move moov atom to front (streaming-friendly)

  outFile,
];

console.log(`▶  ffmpeg capturing at ${FPS}fps, CRF ${CRF}, preset ${PRESET} …`);

const ffmpegProc = spawn(FFMPEG_BIN, ffmpegArgs, { stdio: ['ignore', 'pipe', 'pipe'] });

ffmpegProc.stderr.on('data', (chunk) => {
  // ffmpeg writes progress to stderr; suppress unless debugging
  if (process.env.DEBUG_FFMPEG) process.stderr.write(chunk);
});

ffmpegProc.on('error', (err) => {
  console.error('✗  ffmpeg error:', err.message);
});

// ---------------------------------------------------------------------------
// Wait for ad to finish, then stop everything
// ---------------------------------------------------------------------------

console.log(`⏳  Recording for ${totalMs / 1000}s …`);
await page.waitForTimeout(totalMs);

// Close browser first (stops new frames), then signal ffmpeg to flush & exit.
await context.close();
await browser.close();

// Send 'q' to ffmpeg stdin to trigger a clean exit (graceful flush).
// Since we set stdin to 'ignore', we kill it directly — ffmpeg will flush on SIGINT.
await new Promise((resolve) => {
  ffmpegProc.on('close', resolve);
  // SIGINT triggers ffmpeg's graceful shutdown (writes remaining frames).
  try { ffmpegProc.kill('SIGINT'); } catch { ffmpegProc.kill(); }
  // Safety timeout: force-kill after 10 s if it hasn't exited.
  setTimeout(() => { try { ffmpegProc.kill(); } catch {} }, 10_000);
});

if (fs.existsSync(outFile)) {
  const mb = (fs.statSync(outFile).size / 1_048_576).toFixed(1);
  console.log(`✓  Saved recordings/${path.basename(outFile)}  (${mb} MB)`);
} else {
  console.error('✗  Output file not found — ffmpeg may have failed. Re-run with DEBUG_FFMPEG=1 for details.');
  process.exit(1);
}
