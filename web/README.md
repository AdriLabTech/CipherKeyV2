# CipherKey — web de producto con intro de enjambre de agentes

Web en español construida a partir de `CipherKey-Presentacion.pdf` (texto, logo y 7 capturas reales de la app extraídas del PDF)
+ la intro "construcción multiagente": 8 agentes (cursores de color) construyen la web a la vista, zona por zona.
Misma arquitectura que `~/Proyectos/apple-agents-site`: la web va primero y la intro es una capa independiente encima.

```
npm install
npm run dev        # http://127.0.0.1:5173
npm run build && npm run preview
npm test           # e2e Playwright (necesita el dev server en :5173; SITE_URL para otro). Tarda varios minutos.
```

URL: `?construct=1` fuerza la intro · `?construct=0` la omite · `?lag=0` (solo depuración) desactiva el lag smoothing de GSAP.
Salta con **Saltar intro** o `Esc`; botón "Repetir la construcción" en el footer. `prefers-reduced-motion` la omite. Una vez por sesión.

## Estructura
- `index.html` + `src/style.css` + `src/site/site.js` — la web (GSAP/ScrollTrigger, sin Three.js): hero con inclinación 3D,
  problema, público, funciones, **generador de contraseñas funcional** (`crypto.getRandomValues`, nada sale del navegador),
  flujo de seguridad (clave → Argon2id → AES-256-GCM → `.ckey`), por qué usarla (captura clara/oscura), descarga.
- `src/construction/` — capa independiente; solo conoce los elementos por `data-c="<id>"`.
  `config.js` es el manifiesto (agentes, zonas de cámara, 39 tareas con dependencias y efectos).
- `public/img/` — logo (PNG con alfa recompuesto de imagen + smask del PDF) y capturas.

## Contenido: qué viene del PDF y qué se añadió
Del PDF: todos los textos, cifras (256 bits, 1 fichero, 0 conexiones), versión 4.0.0, Java 25, MIT, 9 paquetes, formatos, URL de releases.
Añadido: generador interactivo, diagrama del flujo de cifrado (solo con datos del PDF), anotaciones sobre la captura, conmutador claro/oscuro.
No se inventó ninguna característica, precio ni cifra que no esté en el PDF.

## Verificado
Ver `test/e2e.mjs` (Chromium headless, render por software). No verificado: GPU real, Safari/Firefox, lector de pantalla real,
que el enlace de descarga apunte a una release publicada (se usa la URL del PDF).
