import { launch, URL } from './lib.mjs';

let pass = 0, fail = 0;
const ok = (cond, msg, extra = '') => { (cond ? pass++ : fail++); console.log(`${cond ? '  ✓' : '  ✗'} ${msg}${!cond && extra ? '  → ' + extra : ''}`); };
const group = (t) => console.log(`\n■ ${t}`);

const browser = await launch();
const Q = '?lag=0';

async function newPage(opts = {}) {
  const ctx = await browser.newContext({ viewport: { width: 1440, height: 900 }, ...opts });
  const page = await ctx.newPage();
  page.errs = [];
  page.on('pageerror', (e) => page.errs.push(e.message));
  page.on('console', (m) => { if (m.type() === 'error') page.errs.push(m.text()); });
  await page.addInitScript(() => { addEventListener('construction:complete', () => { window.__done = performance.now(); }); });
  return page;
}
const done = (p, t = 90000) => p.waitForFunction(() => window.__done, null, { timeout: t });

/** Everything that must be true once the intro is over. */
async function assertRestored(p, label) {
  const r = await p.evaluate(() => {
    const dirty = [...document.querySelectorAll('[data-c], [data-c] *')].filter((e) => {
      const s = e.style; return s.opacity === '0' || (s.clipPath && s.clipPath !== 'none') || (s.filter && s.filter.includes('blur'));
    }).map((e) => e.dataset.c || e.className);
    return {
      overlay: !!document.querySelector('.cx-root'), constructing: document.documentElement.classList.contains('constructing'),
      pre: document.documentElement.classList.contains('pre-construct'), scrollY, dirty,
      flag: sessionStorage.getItem('cipherkey:construction'),
      overflow: getComputedStyle(document.documentElement).overflow, hidden: [...document.querySelectorAll('[data-c]')].filter((e) => getComputedStyle(e).visibility === 'hidden').length,
    };
  });
  ok(!r.overlay, `${label}: overlay removed`);
  ok(!r.constructing && !r.pre, `${label}: no construction/pre-construct class left`);
  ok(r.scrollY === 0, `${label}: back at top`, `scrollY=${r.scrollY}`);
  ok(r.dirty.length === 0, `${label}: no component left hidden`, r.dirty.join(','));
  ok(r.flag === '1', `${label}: played flag stored`);
  ok(r.overflow !== 'hidden', `${label}: scroll unlocked`, r.overflow);
  ok(r.hidden === 0, `${label}: nothing visibility:hidden`);
  return r;
}

// ───────────────────────────────────────────────────────────────────────────
group('1. First visit — full sequence (desktop 1440×900)');
{
  const p = await newPage();
  await p.goto(URL + Q);
  await p.waitForSelector('.cx-root', { timeout: 20000 });
  const early = await p.evaluate(() => ({
    constructing: document.documentElement.classList.contains('constructing'),
    heroTitleClip: getComputedStyle(document.querySelector('[data-c="hero-title"]')).clipPath,
    navClip: getComputedStyle(document.querySelector('[data-c="nav"]')).clipPath,
  }));
  ok(early.constructing, 'site is locked while constructing');
  ok(early.heroTitleClip !== 'none' && early.navClip !== 'none', 'components start hidden', JSON.stringify(early));

  // agents, labels, distinct colours
  await p.waitForFunction(() => document.querySelectorAll('.cx-agent').length === 8, null, { timeout: 10000 });
  const ag = await p.evaluate(() => [...document.querySelectorAll('.cx-agent')].map((a) => ({ l: a.querySelector('.cx-label').textContent, c: a.style.getPropertyValue('--c') })));
  ok(ag.length === 8 && new Set(ag.map((a) => a.c)).size === 8 && new Set(ag.map((a) => a.l)).size === 8, '8 agents with unique label + colour', JSON.stringify(ag.map((a) => a.l)));

  // status messages
  const seen = new Set();
  let alignChecked = 0, alignBad = [];
  const t0 = Date.now();
  while (!(await p.evaluate(() => window.__done)) && Date.now() - t0 < 80000) {
    const s = await p.evaluate(() => document.querySelector('.cx-msg')?.textContent || '');
    if (s) seen.add(s);
    // boundary alignment vs the real element rect
    const res = await p.evaluate(() => [...document.querySelectorAll('.cx-bd')].map((b) => {
      const id = b.querySelector('.cx-bd-tag')?.textContent.trim().split(/\s+/)[0]; const el = id && document.querySelector(`[data-c="${id}"]`); if (!el) return null;
      const r = el.getBoundingClientRect(), x = b.getBoundingClientRect(); const vh = innerHeight, vw = innerWidth;
      if (r.top < 0 || r.left < 0 || r.bottom > vh || r.right > vw) return { id, skip: true };
      const inset = /-bg$/.test(id) ? 10 : 0;
      return { id, dx: Math.abs(x.left - (r.left + inset)), dy: Math.abs(x.top - (r.top + inset)), dw: Math.abs(x.width - (r.width - inset * 2)), dh: Math.abs(x.height - (r.height - inset * 2)) };
    }).filter(Boolean));
    for (const m of res) { if (m.skip) continue; alignChecked++; if (m.dx > 3 || m.dy > 3 || m.dw > 3 || m.dh > 3) alignBad.push(JSON.stringify(m)); }
    await p.waitForTimeout(500);
  }
  await done(p);
  for (const msg of ['INICIANDO ENTORNO DE CONSTRUCCIÓN', 'RED DE AGENTES CONECTADA', 'IDENTIFICANDO COMPONENTES', 'CONSTRUCCIÓN EN PARALELO', 'CONSTRUCCIÓN COMPLETA', 'SISTEMAS OPERATIVOS'])
    ok(seen.has(msg), `status shown: ${msg}`);
  ok(alignChecked > 10 && alignBad.length === 0, `boundaries align with real element rects (${alignChecked} samples)`, alignBad.slice(0, 3).join(' | '));
  await assertRestored(p, 'after intro');
  const log = await p.evaluate(() => window.__cxlog);
  ok(log.some((l) => l[1] === 'zone7 done'), 'all 8 zones completed');
  ok(p.errs.length === 0, 'no console/page errors', p.errs.join(' | '));

  // site works normally afterwards
  await p.evaluate(() => window.scrollTo({ top: 1500, behavior: 'instant' })); await p.waitForTimeout(400);
  ok((await p.evaluate(() => scrollY)) > 1000, 'normal scrolling works after construction');
  await p.evaluate(() => window.scrollTo({ top: 0, behavior: 'instant' }));
  await p.click('.nav-links a[href="#seguridad"]'); await p.waitForTimeout(1800);
  const secTop = await p.evaluate(() => document.querySelector('#seguridad').getBoundingClientRect().top);
  ok(Math.abs(secTop) < 120, 'nav link scrolls to its section', `top=${secTop}`);
  await p.evaluate(() => window.scrollTo({ top: 0, behavior: 'instant' })); await p.waitForTimeout(300);
  ok(await p.evaluate(() => { const b = document.querySelector('.btn.primary'); b.focus(); return document.activeElement === b; }), 'interactive elements focusable');

  // refresh: finished site loads directly
  await p.reload({ waitUntil: 'load' }); await p.waitForTimeout(2500);
  const again = await p.evaluate(() => ({ overlay: !!document.querySelector('.cx-root'), pre: document.documentElement.classList.contains('pre-construct'), vis: getComputedStyle(document.querySelector('[data-c="hero-title"]')).visibility }));
  ok(!again.overlay && !again.pre && again.vis === 'visible', 'refresh in same session loads finished site (no replay, nothing hidden)', JSON.stringify(again));

  // manual replay
  await p.evaluate(() => { window.__done = 0; });
  await p.evaluate(() => document.getElementById('replay').scrollIntoView());
  await p.click('#replay');
  await p.waitForSelector('.cx-root', { timeout: 10000 });
  ok(true, 'replay button starts the sequence');
  await done(p); await assertRestored(p, 'after replay');
  ok(p.errs.length === 0, 'no errors during reload/replay', p.errs.join(' | '));
  await p.context().close();
}

// ───────────────────────────────────────────────────────────────────────────
group('2. Skip button / Escape');
{
  const p = await newPage();
  await p.goto(URL + Q); await p.waitForSelector('.cx-skip'); await p.waitForTimeout(2500);
  const t = Date.now(); await p.click('.cx-skip'); await done(p, 8000);
  ok(Date.now() - t < 3000, `skip finishes quickly (${Date.now() - t} ms)`);
  await assertRestored(p, 'after skip');
  ok(p.errs.length === 0, 'no errors on skip', p.errs.join(' | '));
  await p.context().close();

  const q = await newPage();
  await q.goto(URL + Q); await q.waitForSelector('.cx-root'); await q.waitForTimeout(3500); await q.keyboard.press('Escape'); await done(q, 8000);
  await assertRestored(q, 'after Escape');
  await q.context().close();
}

// ───────────────────────────────────────────────────────────────────────────
group('3. Reduced motion');
{
  const p = await newPage({ reducedMotion: 'reduce' });
  await p.goto(URL + Q); await p.waitForTimeout(2500);
  const r = await p.evaluate(() => ({ overlay: !!document.querySelector('.cx-root'), pre: document.documentElement.classList.contains('pre-construct'), vis: getComputedStyle(document.querySelector('[data-c="hero-title"]')).visibility, clip: getComputedStyle(document.querySelector('[data-c="hero-title"]')).clipPath }));
  ok(!r.overlay && !r.pre && r.vis === 'visible' && r.clip === 'none', 'intro skipped, site fully visible', JSON.stringify(r));
  await p.context().close();
}

// ───────────────────────────────────────────────────────────────────────────
group('4. Mobile (390×844, touch)');
{
  const p = await newPage({ viewport: { width: 390, height: 844 }, hasTouch: true, isMobile: true, deviceScaleFactor: 2 });
  await p.goto(URL + Q); await p.waitForSelector('.cx-root', { timeout: 20000 });
  await p.waitForFunction(() => document.querySelectorAll('.cx-agent').length > 0);
  const n = await p.evaluate(() => document.querySelectorAll('.cx-agent').length);
  ok(n === 3, `reduced agent count on mobile (${n})`);
  await p.waitForTimeout(6000);
  ok(await p.evaluate(() => !document.querySelector('.cx-bd-tag') || [...document.querySelectorAll('.cx-bd')].every((b) => b.classList.contains('simple'))), 'simplified boundaries on mobile');
  await done(p); await assertRestored(p, 'mobile after intro');
  const w = await p.evaluate(() => ({ sw: document.documentElement.scrollWidth, iw: innerWidth }));
  ok(w.sw <= w.iw + 1, 'no horizontal overflow', JSON.stringify(w));
  await p.click('#nav-toggle'); await p.waitForTimeout(500);
  ok(await p.evaluate(() => document.getElementById('nav-links').classList.contains('open') && document.getElementById('nav-toggle').getAttribute('aria-expanded') === 'true'), 'mobile menu opens');
  await p.screenshot({ path: 'test/shots/mobile-menu.png' });
  ok(p.errs.length === 0, 'no errors on mobile', p.errs.join(' | '));
  await p.context().close();
}

// ───────────────────────────────────────────────────────────────────────────
group('5. Tablet (820×1180)');
{
  const p = await newPage({ viewport: { width: 820, height: 1180 } });
  await p.goto(URL + Q); await p.waitForSelector('.cx-root', { timeout: 20000 }); await done(p);
  await assertRestored(p, 'tablet after intro');
  const w = await p.evaluate(() => ({ sw: document.documentElement.scrollWidth, iw: innerWidth }));
  ok(w.sw <= w.iw + 1, 'no horizontal overflow', JSON.stringify(w));
  await p.context().close();
}

// ───────────────────────────────────────────────────────────────────────────
group('6. Web terminada (sin intro): imágenes, generador, captura clara/oscura');
{
  const p = await newPage();
  await p.goto(URL + '?construct=0'); await p.waitForTimeout(2500);
  const imgs = await p.evaluate(() => [...document.images].map((i) => ({ s: i.getAttribute('src'), ok: i.complete && i.naturalWidth > 0 })));
  ok(imgs.length >= 9 && imgs.every((i) => i.ok), `${imgs.length} imágenes cargadas`, JSON.stringify(imgs.filter((i) => !i.ok)));
  const g1 = await p.evaluate(() => document.getElementById('gen-out').textContent);
  ok(g1.length === 20, 'generador: 20 caracteres por defecto', g1);
  await p.evaluate(() => { const r = document.getElementById('gen-len'); r.value = 64; r.dispatchEvent(new Event('input')); });
  const g2 = await p.evaluate(() => ({ t: document.getElementById('gen-out').textContent, m: document.getElementById('meter-text').textContent }));
  ok(g2.t.length === 64 && g2.m === 'Muy fuerte', 'generador: 64 caracteres, Muy fuerte', JSON.stringify(g2).slice(0, 120));
  await p.evaluate(() => { for (const id of ['o-up', 'o-nu', 'o-sy']) { const c = document.getElementById(id); c.checked = false; } document.getElementById('o-lo').dispatchEvent(new Event('change')); });
  const g3 = await p.evaluate(() => document.getElementById('gen-out').textContent);
  ok(/^[a-z]{64}$/.test(g3), 'generador: solo minúsculas respeta las opciones');
  await p.evaluate(() => { for (const id of ['o-up', 'o-nu', 'o-sy']) document.getElementById(id).checked = true; document.getElementById('o-am').checked = true; document.getElementById('o-am').dispatchEvent(new Event('change')); });
  const g4 = await p.evaluate(() => document.getElementById('gen-out').textContent);
  ok(!/[Il1O0o|]/.test(g4), 'generador: evita caracteres ambiguos', g4);
  const uniq = new Set(); for (let k = 0; k < 20; k++) { await p.click('#gen-reroll'); uniq.add(await p.evaluate(() => document.getElementById('gen-out').textContent)); }
  ok(uniq.size === 20, 'generador: 20 contraseñas distintas');
  await p.evaluate(() => document.getElementById('porque').scrollIntoView()); await p.click('[data-theme-btn="light"]'); await p.waitForTimeout(700);
  ok(await p.evaluate(() => document.getElementById('why-img').src.includes('dash-light')), 'captura: cambia a tema claro');
  const cnt = await p.evaluate(() => [...document.querySelectorAll('.num[data-count]')].map((n) => n.textContent));
  await p.evaluate(() => document.getElementById('seguridad').scrollIntoView()); await p.waitForTimeout(2200);
  const cnt2 = await p.evaluate(() => [...document.querySelectorAll('.num[data-count]')].map((n) => n.textContent).join(','));
  ok(cnt2 === '1,0,256', 'contadores terminan en 1, 0, 256', cnt2);
  ok(p.errs.length === 0, 'sin errores de consola', p.errs.join(' | '));
  await p.context().close();
}

await browser.close();
console.log(`\n${fail === 0 ? 'ALL PASSED' : 'FAILURES'}: ${pass} passed, ${fail} failed`);
process.exit(fail ? 1 : 0);
