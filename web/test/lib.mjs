import { chromium } from 'playwright';
import { readdirSync, existsSync } from 'fs';
import { homedir } from 'os';
export function chromePath() {
  const base = homedir() + '/.cache/ms-playwright';
  for (const d of readdirSync(base).filter(d => d.startsWith('chromium-'))) {
    for (const sub of ['chrome-linux64/chrome', 'chrome-linux/chrome']) { const p = `${base}/${d}/${sub}`; if (existsSync(p)) return p; }
  }
}
export async function launch(opts = {}) {
  return chromium.launch({ executablePath: chromePath(), headless: true, args: ['--use-gl=angle', '--use-angle=swiftshader', '--enable-unsafe-swiftshader', '--ignore-gpu-blocklist', '--no-sandbox'], ...opts });
}
export const URL = process.env.SITE_URL || 'http://127.0.0.1:5173/CipherKeyV2/';
