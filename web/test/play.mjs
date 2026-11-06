import { launch, URL } from './lib.mjs';
const b = await launch();
const p = await b.newPage({ viewport: { width: 1440, height: 900 } });
const errs = []; p.on('pageerror', e => errs.push(e.message)); p.on('console', m => { if (m.type()==='error') errs.push(m.text()); });
await p.addInitScript(() => addEventListener('construction:complete', () => { window.__done = performance.now(); }));
await p.goto(URL + '?lag=0&construct=1');
await p.waitForSelector('.cx-root');
const t0 = Date.now(); let i = 0;
const want = [2, 4, 6, 9, 12, 15, 18, 22, 26, 30, 34, 38];
while (!(await p.evaluate(() => window.__done)) && Date.now() - t0 < 90000) {
  const t = (Date.now() - t0) / 1000;
  if (i < want.length && t >= want[i]) { await p.screenshot({ path: `test/shots/c${String(want[i]).padStart(2,'0')}.png` }); i++; }
  await p.waitForTimeout(200);
}
console.log('done after', (Date.now() - t0) / 1000, 's; errors:', errs, 'log', JSON.stringify(await p.evaluate(() => window.__cxlog)));
await b.close();
