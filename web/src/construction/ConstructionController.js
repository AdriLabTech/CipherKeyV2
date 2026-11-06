import gsap from 'gsap';
import { ScrollTrigger } from 'gsap/ScrollTrigger';
import './construction.css';
import { CONFIG, TASKS, ZONES } from './config.js';
import { TaskScheduler } from './TaskScheduler.js';
import { ComponentRevealManager } from './ComponentRevealManager.js';
import { ConstructionOverlay } from './ConstructionOverlay.js';
import { AgentManager } from './AgentManager.js';

const html = document.documentElement;
const $ = (s) => document.querySelector(s);

export function shouldPlay({ force = false } = {}) {
  const q = new URLSearchParams(location.search).get('construct');
  if (q === '0') return false;
  if (matchMedia('(prefers-reduced-motion: reduce)').matches && q !== '1') return false;
  if (force || q === '1') return true;
  try { return sessionStorage.getItem(CONFIG.storageKey) !== '1'; } catch { return true; }
}

/**
 * Orchestrates the whole intro. It is a visual layer on top of the finished site: it registers
 * existing elements, hides them temporarily, lets agents "build" them, then restores everything.
 */
export class ConstructionController {
  constructor() {
    this.running = false; this.tweens = new Set();
    this.mobile = matchMedia('(max-width: 760px)').matches || (matchMedia('(pointer: coarse)').matches && innerWidth < 1024);
  }

  track = (tw) => { this.tweens.add(tw); return tw; };
  _log(label) { (window.__cxlog ||= []).push([Math.round(performance.now() - (this._t0 ||= performance.now())), label]); }

  async run({ force = false } = {}) {
    if (this.running) return;
    this.running = true; this.aborted = false;
    const cfg = { ...CONFIG, speed: CONFIG.speed * (this.mobile ? 0.8 : 1) };
    const track = this.track;

    this.scheduler = new TaskScheduler(TASKS, (id) => $(`[data-c="${id}"]`));
    this.reveal = new ComponentRevealManager({ track, custom: {} });
    this.overlay = new ConstructionOverlay({ simple: this.mobile, track, onSkip: () => this.skip() });
    const env = { track, speed: cfg.speed, overlay: this.overlay, scheduler: this.scheduler, reveal: this.reveal, cfg, mobile: this.mobile, trailLen: this.mobile ? 10 : 22 };
    this.agents = new AgentManager(env, this.mobile ? 3 : 8);
    this.cfg = cfg;

    this.watchdog = setTimeout(() => this.skip('watchdog'), cfg.watchdogMs);
    this._onKey = (e) => { if (e.key === 'Escape') this.skip(); };
    addEventListener('keydown', this._onKey);
    this._onResize = () => this._syncCamera();
    addEventListener('resize', this._onResize);

    try {
      // ── Stage 1: empty canvas ───────────────────────────────────────────────
      html.classList.add('constructing');
      window.scrollTo(0, 0);
      this.reveal.hideAll(this.scheduler.tasks);
      html.classList.remove('pre-construct');           // same tick as hideAll → no flash of content
      this.overlay.mount();
      this.overlay.setCount(0, this.scheduler.total);
      this.scheduler.onChange((s) => this.overlay.setCount(s.done, s.total));
      this.overlay.setStatus('INICIANDO ENTORNO DE CONSTRUCCIÓN');
      this._log('stage1'); await this._wait(0.8);

      // ── Stage 2: agents arrive ──────────────────────────────────────────────
      await this.agents.arrive(); this._log('arrived');
      if (this.aborted) return;
      this.overlay.setStatus('RED DE AGENTES CONECTADA');
      await this._wait(0.35);

      // ── Stages 3–4: identify and build, zone by zone ────────────────────────
      for (let z = 0; z < ZONES.length; z++) {
        if (this.aborted) return;
        this.zone = z;
        if (z > 0) await this._goto(ZONES[z]);
        this.overlay.setStatus(z === 0 ? 'IDENTIFICANDO COMPONENTES' : `ENSAMBLANDO · ${ZONES[z].id.toUpperCase()}`);
        const unsub = this.scheduler.onChange(() => { if (z === 0 && this.scheduler.tasks.some((t) => t.status === 'in-progress')) { this.overlay.setStatus('CONSTRUCCIÓN EN PARALELO'); unsub(); } });
        this._log('zone' + z + ' start'); await this.agents.runZone(z); this._log('zone' + z + ' done');
      }
      if (this.aborted) return;

      // ── Stage 5: completion ─────────────────────────────────────────────────
      await this.agents.parade(); this._log('parade');
      this.overlay.setStatus('CONSTRUCCIÓN COMPLETA', { done: true });
      await this._wait(0.9);
      this.overlay.setStatus('SISTEMAS OPERATIVOS', { done: true });
      await this._wait(0.9);

      // ── Stage 6: back to the real website ───────────────────────────────────
      await Promise.all([this.agents.fadeOut(0.6), this._scrollTop(1.1), this.overlay.fadeOut(0.9)]);
      this._finish(true);
    } catch (err) {
      console.error('[construction] aborted by error, restoring site', err);
      this._finish(false);
    }
  }

  _wait(s) { return new Promise((res) => this.track(gsap.delayedCall(s * this.cfg.speed, res))); }

  /** Scroll "camera": follows the work down the page. */
  _zoneY(zone) {
    const max = Math.max(0, document.documentElement.scrollHeight - innerHeight);
    if (zone.toEnd) return max;
    const el = $(zone.target);
    return Math.min(max, Math.max(0, el.getBoundingClientRect().top + scrollY + (zone.offset || 0)));
  }
  _goto(zone) {
    const o = { y: scrollY };
    return new Promise((res) => this.track(gsap.to(o, { y: this._zoneY(zone), duration: 1.0 * this.cfg.speed, ease: 'power2.inOut', onUpdate: () => window.scrollTo(0, o.y), onComplete: res })));
  }
  _scrollTop(dur) {
    const o = { y: scrollY };
    return new Promise((res) => this.track(gsap.to(o, { y: 0, duration: dur, ease: 'power2.inOut', onUpdate: () => window.scrollTo(0, o.y), onComplete: res })));
  }
  _syncCamera() { if (this.running && this.zone !== undefined && !this.aborted) window.scrollTo(0, this._zoneY(ZONES[this.zone])); }

  /** Skip / Esc: stop everything and show the finished site immediately. */
  skip(reason = 'user') {
    if (!this.running || this.aborted) return;
    this.aborted = true;
    if (reason === 'watchdog') console.warn('[construction] watchdog fired');
    this.tweens.forEach((t) => t.kill());
    this.overlay.fadeOut(0.3).then(() => {});
    gsap.delayedCall(0.3, () => this._finish(true));
    this.agents?.agents.forEach((a) => gsap.killTweensOf(a.el));
  }

  /** Idempotent teardown: restores every component, removes the overlay, unlocks scrolling. */
  _finish(markPlayed) {
    if (!this.running) return;
    this.running = false; this.aborted = true;
    clearTimeout(this.watchdog);
    removeEventListener('keydown', this._onKey); removeEventListener('resize', this._onResize);
    this.tweens.forEach((t) => t.kill()); this.tweens.clear();
    this.reveal.showAll();
    this.overlay.destroy();
    html.classList.remove('constructing', 'pre-construct');
    window.scrollTo(0, 0);
    ScrollTrigger.refresh();
    ScrollTrigger.update();
    if (markPlayed) { try { sessionStorage.setItem(CONFIG.storageKey, '1'); } catch { /* private mode */ } }
    window.dispatchEvent(new CustomEvent('construction:complete'));
  }

  replay() { if (this.running) return; window.scrollTo(0, 0); return this.run({ force: true }); }
}
