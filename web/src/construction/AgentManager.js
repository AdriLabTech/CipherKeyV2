import { AGENTS } from './config.js';
import { ConstructionAgent } from './ConstructionAgent.js';

const rand = (seed) => { const x = Math.sin(seed * 999.1) * 10000; return x - Math.floor(x); };

/** Creates the agents, hands out tasks (via the scheduler) and coordinates parallel execution per zone. */
export class AgentManager {
  constructor(env, count) {
    this.env = env;
    this.agents = AGENTS.slice(0, count).map((d, i) => new ConstructionAgent(d, i, env));
    // fewer agents on small screens: fold the manifest's owners onto the available ones
    env.scheduler.tasks.forEach((t) => { t.agentIndex = t.agentIndex % this.agents.length; });
    this.agents.forEach((a) => env.overlay.addAgent(a));
  }

  /** Stage 2: agents arrive from different directions and scan the environment. */
  async arrive() {
    const n = this.agents.length, vw = innerWidth, vh = innerHeight, cx = vw / 2, cy = vh / 2, R = Math.max(vw, vh) * 0.62;
    await Promise.all(this.agents.map(async (a, i) => {
      const ang = (i / n) * Math.PI * 2 + 0.5;
      a.pos.x = cx + Math.cos(ang) * R; a.pos.y = cy + Math.sin(ang) * R;
      await a.wait(i * 0.07);
      await a.appear();
      const ring = Math.min(vw, vh) * (this.env.mobile ? 0.26 : 0.3);
      const pts = [
        [cx + Math.cos(ang) * ring, cy + Math.sin(ang) * ring * 0.7],
        [vw * (0.2 + rand(i + 1) * 0.6), vh * (0.2 + rand(i + 7) * 0.55)],
        [vw * (0.2 + rand(i + 3) * 0.6), vh * (0.2 + rand(i + 11) * 0.55)],
      ];
      await a.moveTo(pts[0][0], pts[0][1], { dur: 0.95, curve: 0.22 });
      await a.moveTo(pts[1][0], pts[1][1], { dur: 0.6, curve: 0.3 });
      await a.moveTo(pts[2][0], pts[2][1], { dur: 0.55, curve: 0.3 });
    }));
  }

  runZone(zone) { return Promise.all(this.agents.map((a) => a.runZone(zone))); }

  /** Stage 5: every agent reaches its final place — a quiet roster along the bottom edge. */
  async parade() {
    const n = this.agents.length, vw = innerWidth, vh = innerHeight, gap = Math.min(118, (vw - 60) / n);
    const x0 = vw / 2 - (gap * (n - 1)) / 2 - 20;
    await Promise.all(this.agents.map(async (a, i) => {
      a.setIdle(false);
      await a.moveTo(x0 + i * gap, vh - 112, { dur: 0.9, curve: 0.2 });
      a.pulse();
    }));
  }
  fadeOut(dur = 0.6) { return Promise.all(this.agents.map((a) => a.tween(a.el, { opacity: 0, duration: dur }))); }
}
