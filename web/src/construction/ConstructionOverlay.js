import gsap from 'gsap';
import { BoundaryRenderer } from './BoundaryRenderer.js';

/**
 * The overlay: one fixed layer holding trails (2D canvas), boundaries, cursors+labels and status.
 * It never touches the website's DOM; it only reads rects from it.
 */
export class ConstructionOverlay {
  constructor({ simple, track, onSkip }) {
    this.simple = simple; this.track = track;
    this.agents = [];
    this.root = document.createElement('div'); this.root.className = 'cx-root';
    this.trail = document.createElement('canvas'); this.trail.className = 'cx-trails'; this.trail.setAttribute('aria-hidden', 'true');
    this.bdLayer = document.createElement('div'); this.bdLayer.className = 'cx-layer'; this.bdLayer.setAttribute('aria-hidden', 'true');
    this.agLayer = document.createElement('div'); this.agLayer.className = 'cx-layer'; this.agLayer.setAttribute('aria-hidden', 'true');
    this.status = document.createElement('div'); this.status.className = 'cx-status'; this.status.setAttribute('role', 'status'); this.status.setAttribute('aria-live', 'polite');
    this.status.innerHTML = '<i class="cx-dot"></i><span class="cx-msg"></span><span class="cx-count"></span>';
    this.msg = this.status.querySelector('.cx-msg'); this.count = this.status.querySelector('.cx-count'); this.dot = this.status.querySelector('.cx-dot');
    this.skip = document.createElement('button'); this.skip.type = 'button'; this.skip.className = 'cx-skip'; this.skip.textContent = 'Saltar intro';
    this.skip.setAttribute('aria-label', 'Saltar la intro de construcción e ir a la web');
    this.skip.addEventListener('click', onSkip);
    this.root.append(this.trail, this.bdLayer, this.agLayer, this.status, this.skip);
    this.boundaries = new BoundaryRenderer(this.bdLayer, { simple, track });
    this.ctx2d = this.trail.getContext('2d');
    this._resize = this._resize.bind(this);
    this._frame = this._frame.bind(this);
    this.ro = new ResizeObserver(this._resize);
  }

  mount() {
    document.body.append(this.root);
    this.ro.observe(document.documentElement); this._resize();
    gsap.ticker.add(this._frame);
    gsap.fromTo(this.status, { opacity: 0, y: 8 }, { opacity: 1, y: 0, duration: 0.6, ease: 'power2.out' });
  }
  _resize() {
    const dpr = Math.min(devicePixelRatio || 1, 2);
    this.trail.width = Math.round(innerWidth * dpr); this.trail.height = Math.round(innerHeight * dpr);
    this.ctx2d.setTransform(dpr, 0, 0, dpr, 0, 0);
  }

  addAgent(agent) { this.agLayer.append(agent.el); this.agents.push(agent); }

  setStatus(text, { done = false } = {}) {
    const m = this.msg;
    gsap.killTweensOf(m);
    gsap.to(m, { opacity: 0, y: -4, duration: 0.16, onComplete: () => { m.textContent = text; gsap.fromTo(m, { opacity: 0, y: 5 }, { opacity: 1, y: 0, duration: 0.3 }); } });
    this.status.classList.toggle('done', done);
  }
  setCount(done, total) { this.count.textContent = `${String(done).padStart(2, '0')}/${String(total).padStart(2, '0')}`; }

  _frame(time) {
    // 1) agents + trails
    const g = this.ctx2d; g.clearRect(0, 0, innerWidth, innerHeight);
    for (const a of this.agents) { a.tick(time); a.drawTrail(g); }
    // 2) boundaries (batched read → write)
    this.boundaries.update();
  }

  fadeOut(dur = 0.7) {
    return new Promise((res) => gsap.to(this.root, { opacity: 0, duration: dur, ease: 'power1.inOut', onComplete: res }));
  }
  destroy() {
    gsap.ticker.remove(this._frame); this.ro.disconnect();
    this.boundaries.clear(); this.agents.forEach((a) => a.destroy?.());
    gsap.killTweensOf([this.root, this.status, this.msg]);
    this.root.remove();
  }
}
