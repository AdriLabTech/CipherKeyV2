import gsap from 'gsap';

const C = { in: 'inset(-8% -3% -8% -3%)' };
/** Reveal effects. Only opacity / clip-path / filter are touched, so layout rects stay exact. */
const FX = {
  fade:   { from: { opacity: 0 }, to: { opacity: 1, ease: 'power1.out' } },
  blur:   { from: { opacity: 0, filter: 'blur(14px)' }, to: { opacity: 1, filter: 'blur(0px)', ease: 'power2.out' } },
  mask:   { from: { clipPath: 'inset(-8% 103% -8% -3%)' }, to: { clipPath: C.in, ease: 'expo.inOut' } },
  maskUp: { from: { clipPath: 'inset(108% -3% -8% -3%)' }, to: { clipPath: C.in, ease: 'expo.inOut' } },
  drop:   { from: { clipPath: 'inset(0% 0% 100% 0%)', opacity: 0.2 }, to: { clipPath: 'inset(0% 0% 0% 0%)', opacity: 1, ease: 'power3.out' } },
  iris:   { from: { clipPath: 'circle(0% at 50% 50%)' }, to: { clipPath: 'circle(78% at 50% 50%)', ease: 'power2.inOut' } },
  scan:   { from: { clipPath: 'inset(50% 0% 50% 0%)', opacity: 0.4 }, to: { clipPath: 'inset(0% 0% 0% 0%)', opacity: 1, ease: 'expo.out' } },
  kids:   { from: { opacity: 0, filter: 'blur(10px)' }, to: { opacity: 1, filter: 'blur(0px)', ease: 'power2.out' }, stagger: 0.12 },
};
const PROPS = 'opacity,filter,clipPath,clip-path';

/**
 * Owns the visual state of every registered component. Components are never destroyed or recreated:
 * we only apply a temporary hidden state and clear it afterwards.
 */
export class ComponentRevealManager {
  constructor({ track, custom = {} }) {
    this.track = track;       // (tween) => tween, so the controller can kill everything on skip
    this.custom = custom;     // { three: { hide(), play(dur, done), show() } }
    this.touched = new Set();
  }
  _targets(task) { return task.kids ? [...task.element.querySelectorAll(task.kids)] : [task.element]; }

  hide(task) {
    this.touched.add(task);
    gsap.set(this._targets(task), FX[task.fx].from);
  }
  hideAll(tasks) { tasks.forEach((t) => this.hide(t)); }

  /** Returns a promise that resolves when the reveal ends. */
  play(task, scale = 1) {
    const dur = task.dur * scale;
    const fx = FX[task.fx], targets = this._targets(task);
    return new Promise((res) => {
      this.track(gsap.to(targets, {
        ...fx.to, duration: dur, stagger: fx.stagger ? fx.stagger * scale : 0,
        onComplete: () => { gsap.set(targets, { clearProps: PROPS }); res(); },
      }));
    });
  }
  /** Restores one task's original appearance immediately. */
  show(task) {
    const t = this._targets(task); gsap.killTweensOf(t); gsap.set(t, { clearProps: PROPS });
  }
  showAll() { this.touched.forEach((t) => this.show(t)); this.touched.clear(); }
}
