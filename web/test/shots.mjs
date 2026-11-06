import { launch, URL } from './lib.mjs';
import { mkdirSync } from 'fs';
mkdirSync('test/shots', { recursive: true });
const b = await launch();
const w = Number(process.env.W || 1440), h = Number(process.env.H || 900), tag = process.env.TAG || 'd';
const p = await b.newPage({ viewport: { width: w, height: h } });
const errs = []; p.on('pageerror', e => errs.push(e.message)); p.on('console', m => { if (['error','warning'].includes(m.type())) errs.push(m.text()); });
await p.goto(URL + '?construct=0'); await p.waitForTimeout(2000);
for (const s of ['hero','problema','publico','funciones','demo','seguridad','porque','descarga']) {
  await p.evaluate((id) => { const y = document.getElementById(id).getBoundingClientRect().top + scrollY; window.scrollTo({ top: y, behavior: 'instant' }); }, s);
  await p.waitForTimeout(1200);
  await p.screenshot({ path: `test/shots/${tag}-${s}.png` });
}
console.log('errors:', errs); await b.close();
