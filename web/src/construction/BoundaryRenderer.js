import gsap from 'gsap';

const NS = 'http://www.w3.org/2000/svg';

/**
 * Draws technical selection boxes around the *live* bounding rect of each target element.
 * Rects are re-read every frame in one batched read phase (getBoundingClientRect), then written, so
 * scroll, resize, sticky layouts and DPR changes are all followed without hardcoded coordinates.
 */
export class BoundaryRenderer {
  constructor(layer, { simple = false, track }) {
    this.layer = layer; this.simple = simple; this.track = track;
    this.items = new Set();
  }

  create(task, agent) {
    const el = document.createElement('div');
    el.className = 'cx-bd' + (this.simple ? ' simple' : '');
    el.style.setProperty('--c', agent.def.color);
    const svg = document.createElementNS(NS, 'svg');
    const rect = document.createElementNS(NS, 'rect');
    rect.setAttribute('pathLength', '1'); rect.setAttribute('rx', '3');
    svg.append(rect);
    const fill = document.createElement('i'); fill.className = 'cx-bd-fill';
    const scan = document.createElement('i'); scan.className = 'cx-bd-scan';
    const corners = ['tl', 'tr', 'bl', 'br'].map((k) => { const c = document.createElement('i'); c.className = `cx-bd-c ${k}`; return c; });
    const tag = document.createElement('span'); tag.className = 'cx-bd-tag';
    el.append(fill, scan, svg, ...corners, tag);
    this.layer.append(el);
    const b = { task, agent, el, svg, rect, fill, scan, corners, tag, trace: 0, r: { x: 0, y: 0, w: 0, h: 0 }, clipped: false, dead: false };
    gsap.set([rect], { strokeDashoffset: 1 });
    gsap.set(corners, { opacity: 0, scale: 0.4 });
    gsap.set(tag, { opacity: 0 });
    this.items.add(b);
    this._measure(b); this._write(b, true);
    return b;
  }

  _measure(b) {
    const r = b.task.element.getBoundingClientRect();
    const vw = innerWidth, vh = innerHeight, ins = b.task.inset || 0;
    let x = r.left + ins, y = r.top + ins, w = r.width - ins * 2, h = r.height - ins * 2;
    b.clipped = false;
    // elements taller/wider than the viewport are shown clipped to it (still aligned to their real edges)
    if (y < 0) { h += y; y = 0; b.clipped = true; } if (x < 0) { w += x; x = 0; b.clipped = true; }
    if (y + h > vh) { h = vh - y; b.clipped = true; } if (x + w > vw) { w = vw - x; b.clipped = true; }
    b.next = { x, y, w: Math.max(0, w), h: Math.max(0, h), raw: r };
  }
  _write(b, force) {
    const n = b.next, o = b.r;
    if (!force && Math.abs(n.x - o.x) < 0.2 && Math.abs(n.y - o.y) < 0.2 && Math.abs(n.w - o.w) < 0.2 && Math.abs(n.h - o.h) < 0.2) return;
    b.r = { x: n.x, y: n.y, w: n.w, h: n.h };
    b.el.style.transform = `translate3d(${n.x}px,${n.y}px,0)`;
    b.el.style.width = n.w + 'px'; b.el.style.height = n.h + 'px';
    b.rect.setAttribute('width', n.w); b.rect.setAttribute('height', n.h);
    if (!this.simple) { b.tag.style.bottom = n.y < 26 ? 'auto' : ''; b.tag.style.top = n.y < 26 ? '6px' : ''; b.tag.style.left = n.y < 26 ? '8px' : ''; }
    if (!this.simple) b.tag.textContent = `${b.task.id}  x${Math.round(n.raw.left)} y${Math.round(n.raw.top + scrollY)}  ${Math.round(n.raw.width)}×${Math.round(n.raw.height)}${b.clipped ? '  ⤓' : ''}`;
  }
  /** Called once per frame by the overlay. */
  update() {
    const live = [...this.items];
    for (const b of live) this._measure(b);          // read phase
    for (const b of live) this._write(b, false);     // write phase
  }

  /** Perimeter point at progress p (0..1, clockwise from top-left), in viewport px. */
  pointAt(b, p) {
    const { x, y, w, h } = b.r, per = 2 * (w + h), d = ((p % 1) + 1) % 1 * per;
    if (d < w) return { x: x + d, y };
    if (d < w + h) return { x: x + w, y: y + (d - w) };
    if (d < 2 * w + h) return { x: x + w - (d - w - h), y: y + h };
    return { x, y: y + h - (d - 2 * w - h) };
  }
  corner(b, k = 'tl') { const { x, y, w, h } = b.r; return { tl: { x, y }, tr: { x: x + w, y }, bl: { x, y: y + h }, br: { x: x + w, y: y + h } }[k]; }
  center(b) { const { x, y, w, h } = b.r; return { x: x + w / 2, y: y + h / 2 }; }

  /** Animate the outline being traced; `onProgress(p)` lets the owning cursor ride the perimeter. */
  trace(b, dur, onProgress) {
    return new Promise((res) => {
      gsap.set(b.corners, { opacity: 0 });
      if (!this.simple) gsap.to(b.tag, { opacity: 1, duration: 0.3, delay: 0.1 });
      const o = { p: 0 };
      this.track(gsap.to(o, {
        p: 1, duration: dur, ease: 'power2.inOut',
        onUpdate: () => { b.rect.style.strokeDashoffset = 1 - o.p; onProgress?.(o.p); },
        onComplete: () => {
          this.track(gsap.to(b.corners, { opacity: 1, scale: 1, duration: 0.35, stagger: 0.04, ease: 'back.out(2)' }));
          res();
        },
      }));
    });
  }
  setWaiting(b, on) { b.el.classList.toggle('waiting', on); if (!this.simple) b.tag.dataset.wait = on ? '1' : ''; }

  scanFill(b, dur) {
    this.track(gsap.fromTo(b.fill, { opacity: 0 }, { opacity: 1, duration: dur * 0.35, ease: 'power1.out' }));
    if (!this.simple) this.track(gsap.fromTo(b.scan, { yPercent: -100, opacity: 1 }, { yPercent: 100, duration: dur, ease: 'power1.inOut' }));
  }
  /** Outline becomes a quiet confirmation, then disappears. */
  confirm(b) {
    b.el.classList.add('done');
    this.track(gsap.to(b.fill, { opacity: 0, duration: 0.5, ease: 'power1.in' }));
    this.track(gsap.to(b.el, { opacity: 0, duration: 0.55, delay: 0.25, ease: 'power1.in', onComplete: () => this.remove(b) }));
  }
  remove(b) { if (b.dead) return; b.dead = true; this.items.delete(b); b.el.remove(); }
  clear() { [...this.items].forEach((b) => this.remove(b)); }
}
