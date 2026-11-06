import gsap from 'gsap';
import { ScrollTrigger } from 'gsap/ScrollTrigger';

gsap.registerPlugin(ScrollTrigger);
const $ = (s, r = document) => r.querySelector(s);
const $$ = (s, r = document) => [...r.querySelectorAll(s)];
const reduce = matchMedia('(prefers-reduced-motion: reduce)').matches;

/* ── generador de contraseñas (crypto.getRandomValues; nada sale del navegador) ── */
function initGenerator() {
  const SETS = { up: 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', lo: 'abcdefghijklmnopqrstuvwxyz', nu: '0123456789', sy: '!@#$%^&*()-_=+[]{};:,.<>?/|~' };
  const AMBIG = /[Il1O0o|]/g;
  const out = $('#gen-out'), len = $('#gen-len'), lenV = $('#gen-len-v'), fill = $('#meter-fill'), mt = $('#meter-text'), mb = $('#meter-bits'), toast = $('#gen-toast');
  const boxes = { up: $('#o-up'), lo: $('#o-lo'), nu: $('#o-nu'), sy: $('#o-sy'), am: $('#o-am') };
  const rnd = (n) => { const lim = Math.floor(0x100000000 / n) * n; const a = new Uint32Array(1); do crypto.getRandomValues(a); while (a[0] >= lim); return a[0] % n; };

  function generate() {
    const L = +len.value;
    let groups = Object.keys(SETS).filter((k) => boxes[k].checked).map((k) => (boxes.am.checked ? SETS[k].replace(AMBIG, '') : SETS[k]));
    if (!groups.length) { boxes.lo.checked = true; groups = [boxes.am.checked ? SETS.lo.replace(AMBIG, '') : SETS.lo]; }
    const pool = groups.join('');
    const chars = groups.map((g) => g[rnd(g.length)]);          // al menos un carácter de cada grupo
    while (chars.length < L) chars.push(pool[rnd(pool.length)]);
    for (let i = chars.length - 1; i > 0; i--) { const j = rnd(i + 1); [chars[i], chars[j]] = [chars[j], chars[i]]; }
    const pw = chars.slice(0, L).join('');
    const bits = Math.round(L * Math.log2(pool.length));
    out.textContent = pw; lenV.textContent = L;
    const [txt, col, pct] = bits < 40 ? ['Muy débil', '#d4483b', 15] : bits < 60 ? ['Débil', '#e0852b', 38] : bits < 80 ? ['Aceptable', '#d6b020', 60] : bits < 110 ? ['Fuerte', '#4aa564', 82] : ['Muy fuerte', '#1f9d6b', 100];
    fill.style.width = pct + '%'; fill.style.background = col; mt.textContent = txt; mb.textContent = `~${bits} bits`;
  }
  Object.values(boxes).forEach((b) => b.addEventListener('change', generate));
  len.addEventListener('input', generate);
  $('#gen-reroll').addEventListener('click', generate);
  $('#gen-copy').addEventListener('click', async () => {
    try { await navigator.clipboard.writeText(out.textContent); toast.textContent = 'Copiada. (En la app, el portapapeles se vacía solo.)'; }
    catch { toast.textContent = 'Selecciona y copia el texto manualmente.'; }
    setTimeout(() => { toast.textContent = ''; }, 2600);
  });
  generate();
}

/* ── navegación ── */
function initNav() {
  const toggle = $('#nav-toggle'), links = $('#nav-links');
  toggle.addEventListener('click', () => { const o = toggle.getAttribute('aria-expanded') !== 'true'; toggle.setAttribute('aria-expanded', o); links.classList.toggle('open', o); });
  links.addEventListener('click', (e) => { if (e.target.closest('a')) { toggle.setAttribute('aria-expanded', 'false'); links.classList.remove('open'); } });
  const secs = $$('.nav-links a').map((a) => [a, $(a.getAttribute('href'))]).filter(([, s]) => s);
  secs.forEach(([a, s]) => ScrollTrigger.create({ trigger: s, start: 'top 55%', end: 'bottom 55%', onToggle: (t) => a.classList.toggle('on', t.isActive) }));
  gsap.set('#progress', { transformOrigin: '0 50%' });
  ScrollTrigger.create({ start: 0, end: 'max', onUpdate: (t) => gsap.set('#progress', { scaleX: t.progress }) });
}

/* ── captura del hero: inclinación 3D con el ratón ── */
function initTilt() {
  const el = $('#tilt'); if (!el || reduce || !matchMedia('(pointer: fine)').matches) return;
  const host = el.parentElement;
  host.addEventListener('pointermove', (e) => {
    const r = host.getBoundingClientRect(), x = (e.clientX - r.left) / r.width - .5, y = (e.clientY - r.top) / r.height - .5;
    el.style.transform = `rotateY(${x * 8}deg) rotateX(${-y * 6}deg)`;
  });
  host.addEventListener('pointerleave', () => { el.style.transform = ''; });
}

/* ── contadores y parallax (solo sobre nodos que la intro no toca) ── */
function initScrollFx() {
  $$('.num[data-count]').forEach((el) => {
    const end = +el.dataset.count, o = { v: end };
    el.textContent = end;
    if (reduce || end === 0) return;
    ScrollTrigger.create({ trigger: el, start: 'top 85%', once: true, onEnter: () => { o.v = 0; gsap.to(o, { v: end, duration: 1.4, ease: 'power2.out', onUpdate: () => { el.textContent = Math.round(o.v); }, onComplete: () => { el.textContent = end; } }); } });
  });
  if (reduce) return;
  $$('img[data-parallax]').forEach((img) => gsap.fromTo(img, { y: 16 }, { y: -16, ease: 'none', scrollTrigger: { trigger: img, start: 'top bottom', end: 'bottom top', scrub: true } }));
}

/* ── captura clara / oscura ── */
function initThemeShot() {
  const img = $('#why-img'), btns = $$('[data-theme-btn]');
  const alt = { dark: ['/img/dash-dark.webp', 'Panel principal de CipherKey en tema oscuro'], light: ['/img/dash-light.webp', 'Panel principal de CipherKey en tema claro'] };
  btns.forEach((b) => b.addEventListener('click', () => {
    const k = b.dataset.themeBtn; btns.forEach((x) => x.classList.toggle('on', x === b));
    img.style.opacity = 0; setTimeout(() => { img.src = alt[k][0]; img.alt = alt[k][1]; img.onload = () => { img.style.opacity = 1; }; }, 180);
  }));
}

/** Prepara toda la web y espera a fuentes + imágenes visibles antes de que la intro mida nada. */
export async function initSite() {
  initNav(); initTilt(); initGenerator(); initThemeShot(); initScrollFx();
  await Promise.all([document.fonts?.ready, ...$$('img').filter((i) => i.loading !== 'lazy').map((i) => i.decode().catch(() => {}))]);
  $$('img[loading="lazy"]').forEach((i) => { i.loading = 'eager'; });   // dimensiones fijas por atributos width/height → los rects son estables
  await Promise.all($$('img').map((i) => i.decode().catch(() => {})));
  ScrollTrigger.refresh();
  return { ScrollTrigger };
}
