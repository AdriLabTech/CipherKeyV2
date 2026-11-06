import gsap from 'gsap';
import './style.css';
import { initSite } from './site/site.js';
import { ConstructionController, shouldPlay } from './construction/ConstructionController.js';

const html = document.documentElement;
// depuración: ?lag=0 desactiva el lag smoothing de GSAP para que las pruebas en render por software mantengan el ritmo real
if (new URLSearchParams(location.search).get('lag') === '0') gsap.ticker.lagSmoothing(0);

async function boot() {
  let site;
  try {
    site = await initSite();                      // Fase 1: la web terminada; nunca depende de la intro
  } catch (err) {
    console.error('Fallo al iniciar la web', err);
    html.classList.remove('pre-construct');       // nunca dejar la página oculta
    return;
  }
  const controller = new ConstructionController();
  window.__site = site; window.__construction = controller;
  document.getElementById('replay')?.addEventListener('click', () => controller.replay());
  if (shouldPlay()) await controller.run();
  else html.classList.remove('pre-construct');
}
boot();
// la restauración desde bfcache nunca debe resucitar una página oculta
addEventListener('pageshow', (e) => { if (e.persisted) html.classList.remove('pre-construct'); });
