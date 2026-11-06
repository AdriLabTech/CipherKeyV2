/**
 * Manifiesto de construcción. La web solo expone ganchos `data-c="<id>"`; quién construye qué,
 * en qué orden y con qué efecto vive aquí, nunca en el código de la web.
 */
export const CONFIG = {
  storageKey: 'cipherkey:construction', // flag de sessionStorage: se reproduce una vez por sesión
  speed: 0.42,                           // escala de tiempo global (>1 = más lento)
  watchdogMs: 60000,                    // parada dura: la web nunca puede quedar oculta
  depTimeoutMs: 6000,                   // una tarea esperando dependencias más de esto se construye igualmente
};

export const AGENTS = [
  { id: 'LAYOUT',   color: '#2f8cff', ink: '#fff' },
  { id: 'CRYPTO',   color: '#8b5cf6', ink: '#fff' },
  { id: 'COPY',     color: '#10b981', ink: '#fff' },
  { id: 'MOTION',   color: '#ff8a1f', ink: '#1a1000' },
  { id: 'ASSETS',   color: '#22d3ee', ink: '#00222a' },
  { id: 'UX',       color: '#e83fd0', ink: '#fff' },
  { id: 'AUDIT',    color: '#facc15', ink: '#241c00' },
  { id: 'RELEASE',  color: '#ff4d6d', ink: '#fff' },
];

/** Paradas de cámara: la página (bloqueada) se desplaza aquí a medida que el trabajo baja. */
export const ZONES = [
  { id: 'inicio',    target: '#hero' },
  { id: 'problema',  target: '#problema' },
  { id: 'publico',   target: '#publico' },
  { id: 'funciones', target: '#funciones' },
  { id: 'demo',      target: '#demo' },
  { id: 'seguridad', target: '#seguridad' },
  { id: 'porque',    target: '#porque' },
  { id: 'descarga',  target: '#descarga', toEnd: true },
];

/**
 * Tareas en orden topológico global (una dependencia siempre se lista antes que sus dependientes).
 * agent: índice en AGENTS. fx: efecto. dur: segundos. kids: revelar los hijos con ese selector.
 */
export const TASKS = [
  // 0 — inicio
  { id: 'nav',          zone: 0, agent: 0, fx: 'drop',   dur: 0.8 },
  { id: 'hero-bg',      zone: 0, agent: 4, fx: 'iris',   dur: 1.1, inset: 10 },
  { id: 'hero-title',   zone: 0, agent: 2, fx: 'mask',   dur: 1.0, deps: ['hero-bg'] },
  { id: 'hero-lede',    zone: 0, agent: 2, fx: 'blur',   dur: 0.8, deps: ['hero-title'] },
  { id: 'hero-cta',     zone: 0, agent: 5, fx: 'fade',   dur: 0.6, deps: ['hero-lede'] },
  { id: 'hero-object',  zone: 0, agent: 3, fx: 'scan',   dur: 1.3, deps: ['hero-bg', 'hero-title'] },
  { id: 'hero-meta',    zone: 0, agent: 7, fx: 'kids',   dur: 0.8, kids: 'span', deps: ['hero-object'] },
  // 1 — problema
  { id: 'prob-bg',      zone: 1, agent: 4, fx: 'fade',   dur: 0.7, inset: 10 },
  { id: 'prob-head',    zone: 1, agent: 2, fx: 'maskUp', dur: 0.9, deps: ['prob-bg'] },
  { id: 'prob-list',    zone: 1, agent: 5, fx: 'kids',   dur: 1.0, kids: 'li', deps: ['prob-head'] },
  { id: 'prob-quote',   zone: 1, agent: 6, fx: 'blur',   dur: 0.8, deps: ['prob-list'] },
  { id: 'prob-image',   zone: 1, agent: 3, fx: 'scan',   dur: 1.0, deps: ['prob-bg'] },
  // 2 — público
  { id: 'pub-bg',       zone: 2, agent: 4, fx: 'fade',   dur: 0.6, inset: 10 },
  { id: 'pub-head',     zone: 2, agent: 2, fx: 'maskUp', dur: 0.9, deps: ['pub-bg'] },
  { id: 'pub-cards',    zone: 2, agent: 5, fx: 'kids',   dur: 1.1, kids: '.card', deps: ['pub-head'] },
  { id: 'pub-note',     zone: 2, agent: 6, fx: 'blur',   dur: 0.7, deps: ['pub-cards'] },
  { id: 'pub-image',    zone: 2, agent: 3, fx: 'iris',   dur: 1.0, deps: ['pub-bg'] },
  // 3 — funciones
  { id: 'fn-bg',        zone: 3, agent: 4, fx: 'fade',   dur: 0.6, inset: 10 },
  { id: 'fn-head',      zone: 3, agent: 2, fx: 'maskUp', dur: 0.9, deps: ['fn-bg'] },
  { id: 'fn-list',      zone: 3, agent: 1, fx: 'kids',   dur: 1.2, kids: '.fn', deps: ['fn-head'] },
  { id: 'fn-image',     zone: 3, agent: 3, fx: 'scan',   dur: 1.1, deps: ['fn-bg'] },
  // 4 — demo interactiva
  { id: 'demo-bg',      zone: 4, agent: 4, fx: 'fade',   dur: 0.6, inset: 10 },
  { id: 'demo-head',    zone: 4, agent: 2, fx: 'maskUp', dur: 0.9, deps: ['demo-bg'] },
  { id: 'demo-gen',     zone: 4, agent: 1, fx: 'iris',   dur: 1.1, deps: ['demo-head'] },
  { id: 'demo-side',    zone: 4, agent: 6, fx: 'kids',   dur: 0.9, kids: '.mini', deps: ['demo-bg'] },
  // 5 — seguridad
  { id: 'sec-bg',       zone: 5, agent: 4, fx: 'fade',   dur: 0.6, inset: 10 },
  { id: 'sec-head',     zone: 5, agent: 2, fx: 'maskUp', dur: 0.9, deps: ['sec-bg'] },
  { id: 'sec-flow',     zone: 5, agent: 1, fx: 'kids',   dur: 1.3, kids: '.node', deps: ['sec-head'] },
  { id: 'sec-stats',    zone: 5, agent: 7, fx: 'kids',   dur: 0.9, kids: '.stat', deps: ['sec-flow'] },
  // 6 — por qué
  { id: 'why-bg',       zone: 6, agent: 4, fx: 'fade',   dur: 0.6, inset: 10 },
  { id: 'why-head',     zone: 6, agent: 2, fx: 'maskUp', dur: 0.9, deps: ['why-bg'] },
  { id: 'why-grid',     zone: 6, agent: 5, fx: 'kids',   dur: 1.1, kids: '.why', deps: ['why-head'] },
  { id: 'why-image',    zone: 6, agent: 3, fx: 'scan',   dur: 1.0, deps: ['why-bg'] },
  // 7 — descarga
  { id: 'dl-bg',        zone: 7, agent: 4, fx: 'fade',   dur: 0.8, inset: 10 },
  { id: 'dl-title',     zone: 7, agent: 2, fx: 'mask',   dur: 1.0, deps: ['dl-bg'] },
  { id: 'dl-cta',       zone: 7, agent: 7, fx: 'fade',   dur: 0.6, deps: ['dl-title'] },
  { id: 'dl-formats',   zone: 7, agent: 7, fx: 'kids',   dur: 0.8, kids: 'li', deps: ['dl-cta'] },
  { id: 'dl-image',     zone: 7, agent: 3, fx: 'scan',   dur: 1.0, deps: ['dl-bg'] },
  { id: 'footer',       zone: 7, agent: 0, fx: 'maskUp', dur: 0.9, deps: ['dl-bg'] },
];
