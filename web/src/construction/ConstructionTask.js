export const Status = Object.freeze({ PENDING: 'pending', ASSIGNED: 'assigned', IN_PROGRESS: 'in-progress', COMPLETED: 'completed', FAILED: 'failed' });

/** One element to construct: target DOM node, owner, dependencies, effect and lifecycle. */
export class ConstructionTask {
  constructor(def, element) {
    this.id = def.id;
    this.zone = def.zone;
    this.agentIndex = def.agent;
    this.fx = def.fx;
    this.dur = def.dur;
    this.deps = def.deps || [];
    this.kids = def.kids || null;
    this.inset = def.inset || 0;
    this.priority = def.priority ?? 0;
    this.element = element;
    this.status = Status.PENDING;
    this.agent = null;
  }
  get finished() { return this.status === Status.COMPLETED || this.status === Status.FAILED; }
}
