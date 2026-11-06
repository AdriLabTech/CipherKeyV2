import { ConstructionTask, Status } from './ConstructionTask.js';

/** Central scheduler: owns every task, resolves dependencies, tells agents what is runnable. */
export class TaskScheduler {
  constructor(defs, resolve) {
    this.tasks = [];
    this.byId = new Map();
    this.waiters = [];              // [{ids, resolve}]
    this.listeners = new Set();
    for (const def of defs) {
      const el = resolve(def.id);
      if (!el) { console.warn(`[construction] no element for "${def.id}", task skipped`); continue; }
      const t = new ConstructionTask(def, el);
      this.tasks.push(t); this.byId.set(t.id, t);
    }
    // drop dependencies that point to skipped tasks
    for (const t of this.tasks) t.deps = t.deps.filter((d) => this.byId.has(d));
  }
  get total() { return this.tasks.length; }
  get done() { return this.tasks.filter((t) => t.finished).length; }
  get allDone() { return this.tasks.every((t) => t.finished); }
  forAgent(agentIndex, zone) { return this.tasks.filter((t) => t.agentIndex === agentIndex && (zone === undefined || t.zone === zone)); }
  zoneDone(zone) { return this.tasks.filter((t) => t.zone === zone).every((t) => t.finished); }
  depsMet(task) { return task.deps.every((d) => this.byId.get(d).finished); }
  onChange(fn) { this.listeners.add(fn); return () => this.listeners.delete(fn); }

  /** Resolves when every dependency of `task` is finished (or after `timeoutMs`, so nothing can deadlock). */
  whenReady(task, timeoutMs) {
    if (this.depsMet(task)) return Promise.resolve(true);
    return new Promise((res) => {
      const timer = setTimeout(() => { this.waiters = this.waiters.filter((w) => w !== entry); res(false); }, timeoutMs);
      const entry = { task, res: (v) => { clearTimeout(timer); res(v); } };
      this.waiters.push(entry);
    });
  }
  assign(task, agent) { task.agent = agent; task.status = Status.ASSIGNED; }
  start(task) { task.status = Status.IN_PROGRESS; this._emit(); }
  complete(task, ok = true) {
    task.status = ok ? Status.COMPLETED : Status.FAILED;
    this.waiters = this.waiters.filter((w) => { if (this.depsMet(w.task)) { w.res(true); return false; } return true; });
    this._emit();
  }
  _emit() { this.listeners.forEach((fn) => fn(this)); }
}
