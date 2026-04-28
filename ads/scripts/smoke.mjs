// Smoke-test: load each ad route headlessly and report any console errors / page errors.
import { chromium } from 'playwright';

const routes = ['', 'investors', 'clients'];
const browser = await chromium.launch({ headless: true });
let failed = 0;

for (const r of routes) {
  const page = await browser.newPage({ viewport: { width: 1920, height: 1080 } });
  const errors = [];
  page.on('pageerror', (e) => errors.push(`pageerror: ${e.message}`));
  page.on('console', (msg) => {
    if (msg.type() === 'error') errors.push(`console.error: ${msg.text()}`);
  });
  await page.goto(`http://localhost:5180/#/${r}`, { waitUntil: 'networkidle' });
  await page.waitForTimeout(1500);
  if (errors.length) {
    console.log(`✗  /#/${r || '(index)'}`);
    errors.forEach(e => console.log('   ' + e));
    failed++;
  } else {
    console.log(`✓  /#/${r || '(index)'}`);
  }
  await page.close();
}

await browser.close();
process.exit(failed ? 1 : 0);
