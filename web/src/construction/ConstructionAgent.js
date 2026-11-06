import gsap from 'gsap';

const clamp = (v, a, b) => Math.min(b, Math.max(a, v));
const wait = (s, track) => new Promise((res) => track(gsap.delayedCall(s, res)));

/** One autonomous-looking worker: a small coloured pointer with a label, a glow trail and a task queue. */
export class ConstructionAgent {
  constructor(def, index, env) {
    this.def = def; this.index = index; this.env = env;   // env: { track, speed, overlay, scheduler, reveal, cfg, mobile }
    this.pos = { x: -60, y: -60 };
    this.amp = 1.2;                    // organic jitter amplitude (px); larger while idle
    this.trailPts = [];
    this.state = 'offstage';
    this.el = document.createElement('div');
    this.el.className = 'cx-agent'; this.el.style.setProperty('--c', def.color); this.el.style.setProperty('--ink', def.ink);
    this.el.innerHTML = `<svg class="cx-ptr" viewBox="0 0 16 18" width="15" height="17"><path d="M1.5 1.2 L1.5 13.6 L4.8 10.6 L7.1 16 L9.5 15 L7.2 9.7 L11.9 9.4 Z" fill="${def.color}" stroke="#fff" stroke-width="1.1" stroke-linejoin="round"/></svg><span class="cx-label">${def.id}</span><i class="cx-pulse"></i>`;
    this.seed = index * 1.7 + 0.3;
    gsap.set(this.el, { opacity: 0 });
  }

  tween(target, vars) { return new Promise((res) => this.env.track(gsap.to(target, { ...vars, onComplete: res }))); }
  wait(s) { return wait(s * this.env.speed, this.env.track); }

  /** Called every frame by the overlay. Position = planned path + a little organic drift. */
  tick(t) {
    const a = this.amp;
    const nx = Math.sin(t * 1.3 + this.seed) * a + Math.sin(t * 2.9 + this.seed * 2) * a * 0.4;
    const ny = Math.cos(t * 1.1 + this.seed) * a + Math.cos(t * 3.3 + this.seed * 3) * a * 0.4;
    this.x = this.pos.x + nx; this.y = this.pos.y + ny;
    this.el.style.transform = `translate3d(${this.x}px,${this.y}px,0)`;
    if (this.state !== 'offstage') {
      this.trailPts.push({ x: this.x, y: this.y, t });
      while (this.trailPts.length > this.env.trailLen || (this.trailPts.length && t - this.trailPts[0].t > 0.45)) this.trailPts.shift();   // time-based: independent of frame rate
    }
  }
  drawTrail(g) {
    const p = this.trailPts, n = p.length; if (n < 3) return;
    g.lineCap = 'round'; g.lineJoin = 'round';
    for (const [w, al] of [[5, 0.10], [1.6, 0.55]]) {
      for (let i = 1; i < n; i++) {
        const k = i / n; g.globalAlpha = al * k * k; g.strokeStyle = this.def.color; g.lineWidth = w * (0.3 + k * 0.7);
        g.beginPath(); g.moveTo(p[i - 1].x, p[i - 1].y); g.lineTo(p[i].x, p[i].y); g.stroke();
      }
    }
    g.globalAlpha = 1;
  }

  /** Smooth curved path (quadratic Bézier, perpendicular offset) with ease-in-out. */
  moveTo(tx, ty, opts = {}) {
    const sx = this.pos.x, sy = this.pos.y, dx = tx - sx, dy = ty - sy, dist = Math.hypot(dx, dy);
    if (dist < 2) return Promise.resolve();
    const dur = (opts.dur ?? clamp(0.32 + dist / 1500, 0.32, 1.05)) * this.env.speed;
    const bend = (this.index % 2 ? 1 : -1) * (opts.curve ?? 0.16) * dist;
    const cx = (sx + tx) / 2 - (dy / dist) * bend, cy = (sy + ty) / 2 + (dx / dist) * bend;
    const o = { t: 0 };
    this.amp = 1.2;
    return this.tween(o, {
      t: 1, duration: dur, ease: opts.ease || 'power3.inOut',
      onUpdate: () => { const t = o.t, u = 1 - t; this.pos.x = u * u * sx + 2 * u * t * cx + t * t * tx; this.pos.y = u * u * sy + 2 * u * t * cy + t * t * ty; },
    });
  }
  appear() { this.state = 'idle'; return this.tween(this.el, { opacity: 1, duration: 0.35 }); }
  setIdle(on) { this.amp = on ? 4.5 : 1.2; this.state = on ? 'idle' : 'busy'; this.el.classList.toggle('idle', on); }
  pulse() {
    const p = this.el.querySelector('.cx-pulse');
    this.env.track(gsap.fromTo(p, { scale: 0.3, opacity: 0.9 }, { scale: 2.6, opacity: 0, duration: 0.7, ease: 'power2.out' }));
  }

  /** Where to stand while working: just outside the top-left corner (inside the viewport). */
  _stand(b) {
    const c = this.env.overlay.boundaries.corner(b, 'tl');
    return { x: clamp(c.x, 8, innerWidth - 90), y: clamp(c.y, 8, innerHeight - 30) };
  }

  async identify(task) {
    const B = this.env.overlay.boundaries;
    const b = B.create(task, this);
    this.setIdle(false);
    const s = this._stand(b); await this.moveTo(s.x, s.y);
    await this.wait(0.06);
    const per = 2 * (b.r.w + b.r.h);
    const dur = clamp(per / 2600, 0.38, 0.8) * this.env.speed;
    await B.trace(b, dur, (p) => { const q = B.pointAt(b, p); this.pos.x = q.x; this.pos.y = q.y; });
    return b;
  }

  async build(task, b) {
    const { scheduler, reveal, overlay, cfg } = this.env, B = overlay.boundaries;
    if (!scheduler.depsMet(task)) { B.setWaiting(b, true); this.setIdle(true); }
    const ok = await scheduler.whenReady(task, cfg.depTimeoutMs);
    if (!ok) console.warn(`[construction] ${task.id}: dependencies timed out, building anyway`);
    B.setWaiting(b, false); this.setIdle(false);
    scheduler.start(task);
    await this.wait(0.1);
    const dur = task.dur * this.env.speed;
    B.scanFill(b, dur);
    const c = B.center(b);
    const move = this.moveTo(clamp(c.x, 20, innerWidth - 100), clamp(c.y, 20, innerHeight - 30), { dur: Math.min(0.7, dur) * 0.9, curve: 0.1 });
    const rev = reveal.play(task, this.env.speed);
    await Promise.all([rev, move]);
    this.pulse(); B.confirm(b);
    scheduler.complete(task, true);
  }

  /** Everything this agent owns inside a zone: identify all regions first, then build as dependencies allow. */
  async runZone(zone) {
    const { scheduler } = this.env;
    const mine = scheduler.forAgent(this.index, zone);
    if (!mine.length) { await this.observe(zone); return; }
    const bs = [];
    for (const t of mine) { scheduler.assign(t, this); bs.push(await this.identify(t)); }
    for (let i = 0; i < mine.length; i++) {
      try { await this.build(mine[i], bs[i]); }
      catch (err) { console.error('[construction] task failed', mine[i].id, err); this.env.reveal.show(mine[i]); this.env.overlay.boundaries.remove(bs[i]); scheduler.complete(mine[i], false); }
    }
    this.setIdle(true);
  }

  /** An agent without work in this zone goes to watch a colleague's region instead of wandering randomly. */
  async observe(zone) {
    const { scheduler } = this.env;
    const pending = scheduler.tasks.filter((t) => t.zone === zone && !t.finished);
    const t = pending[this.index % Math.max(1, pending.length)];
    if (t) {
      const r = t.element.getBoundingClientRect();
      const x = clamp(r.right + 10, 20, innerWidth - 110), y = clamp(r.bottom + 10, 20, innerHeight - 30);
      await this.moveTo(x, y, { curve: 0.25 });
    }
    this.setIdle(true);
    if (!scheduler.zoneDone(zone)) await new Promise((res) => { const off = scheduler.onChange(() => { if (scheduler.zoneDone(zone)) { off(); res(); } }); });
  }

  destroy() { this.el.remove(); }
}
