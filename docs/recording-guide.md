# Guía de grabación de vídeo en alta calidad

Esta guía documenta el sistema que usamos para grabar los anuncios de StreetAsk
([`ads/`](../ads/)) a 1080p 60fps perfectamente suaves, en una pantalla portátil
de 14" con escalado 150%. Sirve como referencia para grabar **cualquier**
contenido web animado (incluida una demo de TFG) sin depender del monitor físico.

---

## El problema con la grabación "fácil"

La primera idea para grabar una demo es:

1. Pantalla completa en el navegador (F11) + OBS / Xbox Game Bar / etc.
2. O usar `playwright codegen` con captura de pantalla del SO (`gdigrab` en Windows).

Esto **NO funciona bien** en muchos casos:

- **Pantallas pequeñas**: en un Yoga de 14" con resolución 1920×1080, no hay sitio
  para una ventana de navegador a tamaño nativo más la barra de tareas.
- **Escalado DPI ≠ 100%**: Windows por defecto pone 125% o 150% en portátiles.
  La captura de pantalla agarra píxeles físicos pero el navegador renderiza en
  píxeles lógicos. El resultado: vídeo recortado mal o desproporcionado.
- **Animaciones lentas en headless**: `requestAnimationFrame` se ralentiza
  cuando el navegador no está en foco, así que cualquier captura headless con
  Playwright `recordVideo` da ~15-25 fps reales en vez de 60.
- **Tienes que dejar el ordenador quieto**: no puedes mover el ratón ni cambiar
  de ventana durante toda la grabación.

---

## La estrategia: captura determinística frame-a-frame

En lugar de capturar **tiempo real**, controlamos el **reloj virtual** del
navegador y vamos avanzando un fotograma a la vez. Para cada tick:

1. Avanzamos el reloj virtual 1/60 de segundo.
2. Disparamos manualmente todos los `requestAnimationFrame`, `setTimeout` y
   `setInterval` que tocaban en ese intervalo.
3. Hacemos un screenshot JPEG de la página.
4. Pasamos ese JPEG a `ffmpeg` por stdin.

Después de 60s × 60fps = **3600 fotogramas**, `ffmpeg` cierra y produce un MP4
H.264 a 60fps. Como cada fotograma se renderiza con el navegador "congelado",
da igual lo lento que rinda el GPU — el vídeo siempre sale suave.

Es exactamente la técnica que usan herramientas como **Remotion** (vídeo
programático en React) o los renderizadores de Lottie.

**Trade-off:** la grabación tarda más que el vídeo final. Para 60s de vídeo, el
proceso tarda ~3-5 minutos. Pero puedes seguir usando el ordenador mientras se
graba (es todo headless).

---

## Componentes del sistema

```
ads/
├── scripts/
│   └── record.mjs              ← orquestador Playwright + ffmpeg
├── src/
│   └── components/
│       └── useTimeline.js      ← hook de línea temporal (anclado a 1er rAF)
└── recordings/                 ← MP4s generados
```

### 1. Override del reloj de la página

Antes de que se ejecute cualquier script de la página, inyectamos un
`addInitScript` que reemplaza las APIs de tiempo:

```js
// Activo sólo cuando la URL contiene ?record
if (!new URLSearchParams(location.search).has('record')) return;

let vt = 0;  // virtual time en ms

performance.now = () => vt;
Date.now = () => Math.floor(vt) + 1700000000000;

const rafQueue = new Map();
let nextRafId = 1;
window.requestAnimationFrame = (cb) => {
  const id = nextRafId++;
  rafQueue.set(id, cb);
  return id;
};
window.cancelAnimationFrame = (id) => rafQueue.delete(id);

const timers = new Map();
let nextTimerId = 1;
window.setTimeout = (cb, delay = 0, ...args) => {
  const id = nextTimerId++;
  timers.set(id, { fireAt: vt + Number(delay), cb, args, repeat: false });
  return id;
};
window.clearTimeout = (id) => timers.delete(id);
// setInterval análogo con repeat: true

// API que llama el recorder
window.__tick = (deltaMs) => {
  vt += deltaMs;
  // Fire timers cuyo fireAt <= vt
  // Fire todos los rAF encolados
};
```

Como el `?record` se añade en la URL desde el script, **la página normal del
dev server sigue funcionando normal** sin estos overrides.

### 2. El gotcha de Framer Motion (WAAPI)

Framer Motion v11 detecta si el navegador tiene `Element.prototype.animate`
(Web Animations API). Si la tiene, usa esa API para animar `opacity` y
`transform` porque está acelerada en el compositor de Chromium.

**El problema**: WAAPI se tickea desde el compositor en **tiempo real**,
ignorando completamente nuestros overrides de `requestAnimationFrame` y
`performance.now`. Resultado: todas las animaciones aparecen al instante en el
primer fotograma.

**La detección de FM es**:
```js
const supportsWaapi = memo(() => Object.hasOwnProperty.call(Element.prototype, "animate"));
```

Comprueba si **existe la propiedad**, no si funciona. Así que no basta con
sobrescribir `.animate` con una función que lance — hay que **borrar** la
propiedad del prototipo para que `hasOwnProperty` devuelva `false`:

```js
delete Element.prototype.animate;
delete HTMLElement.prototype.animate;
```

Con WAAPI deshabilitada, Framer Motion cae a su animador JS, que sí usa
`requestAnimationFrame` (el que hemos sobrescrito). Animaciones controladas.

### 3. El timeline determinístico

[`useTimeline.js`](../ads/src/components/useTimeline.js) maneja la progresión
entre escenas. Para que funcione con el reloj virtual, hay que **anclar el t=0
al primer tick de rAF**, no al `useEffect`:

```js
const loopFn = (t) => {
  if (startRef.current === null) startRef.current = t;
  setTick(t);
  rafRef.current = requestAnimationFrame(loopFn);
};
```

Esto es importante porque bajo reloj virtual `performance.now()` puede valer 0
al montar (que es falsy), lo que rompe el `startRef.current ? ... : 0` original.

### 4. Pipe a ffmpeg

Cada screenshot JPEG se escribe directamente a stdin de un proceso `ffmpeg`
que lo lee como `image2pipe` y encodea a H.264:

```js
const ffmpeg = spawn(FFMPEG_BIN, [
  '-y',
  '-f', 'image2pipe',
  '-vcodec', 'mjpeg',
  '-framerate', '60',
  '-i', '-',
  '-vcodec', 'libx264',
  '-pix_fmt', 'yuv420p',
  '-crf', '14',          // visualmente sin pérdida
  '-preset', 'slow',     // mejor compresión
  '-tune', 'animation',  // optimizado para gradientes / áreas planas
  '-r', '60',
  '-movflags', '+faststart',
  outFile,
]);

// En el loop:
const buf = await page.screenshot({ type: 'jpeg', quality: 92 });
ffmpeg.stdin.write(buf);
```

---

## Cómo usarlo (StreetAsk ads)

### Requisitos previos (una sola vez)

```powershell
cd ads
npm install
npx playwright install chromium     # descarga el binario (~150 MB)
winget install ffmpeg                # o "choco install ffmpeg"
# reinicia la terminal para que ffmpeg esté en PATH
```

### Grabar

```powershell
# Terminal 1 — dev server
cd ads
npm run dev

# Terminal 2 — esperar ~3s a que arranque dev, luego:
cd ads
npm run record:investors            # o record:clients

# Si Vite escoge otro puerto (5180 ocupado → 5181, etc.):
$env:PORT=5181; npm run record:investors
```

Salida en consola:
```
▶  Deterministic capture: 3660 frames @ 60fps  (1920×1080)
▶  Starting ffmpeg (libx264 CRF 14, preset slow, tune animation)
▶  Rendering frames …
   60s rendered  ·  264.6s elapsed  ·  13.8 capture-fps
✓  Saved recordings/investors-2026-05-11T16-00-56.mp4  (17.4 MB)
```

### Flags útiles

| Variable de entorno | Efecto |
|---|---|
| `PORT=5181` | Apuntar a otro puerto del dev server |
| `FPS=30` | Capturar a 30fps en vez de 60 (la mitad de tiempo de render) |
| `DEBUG_FFMPEG=1` | Mostrar la salida de stderr de ffmpeg (debug) |

---

## Cómo adaptarlo a otra app (demo del TFG)

Para grabar **otra** SPA (React/Vue/Svelte/lo que sea) con este sistema, hacen
falta tres cosas:

### 1. Copiar el script de grabación

Copia [`ads/scripts/record.mjs`](../ads/scripts/record.mjs) a tu proyecto y
ajusta:

```js
const DURATIONS = { demo: 90 };           // segundos de tu demo
const url = `http://localhost:5173/?record#/demo`;  // tu dev URL
```

Asegúrate de:
- Tu dev server expone la URL del demo
- La query `?record` se preserva (con hash routes no hay problema; con react-router
  o similar, comprueba que `?record` no se pierde al navegar)

### 2. Hacer la demo "determinista"

Tu demo necesita un **timeline conocido**. Hay dos formas:

**A) Si tu demo se reproduce sola** (igual que los anuncios):
- Define las escenas/pasos como un array `[{ id, duration }]`
- Usa un hook tipo `useTimeline` (puedes copiar el nuestro) que dispare
  `setState` desde `requestAnimationFrame` para que sea determinístico

**B) Si tu demo requiere interacción** (clicks, scrolls, formularios):
- Mantén la URL `?record` para activar el reloj virtual
- En lugar de avanzar tiempo automáticamente, el `record.mjs` también puede
  ejecutar acciones de Playwright entre ticks:

```js
for (let i = 0; i < totalFrames; i++) {
  await page.evaluate((dt) => window.__tick(dt), FRAME_MS);

  // Acciones programadas en momentos específicos:
  if (i === 60) await page.click('[data-test="primary-button"]');
  if (i === 180) await page.fill('#email', 'demo@example.com');
  if (i === 300) await page.click('[data-test="submit"]');

  const buf = await page.screenshot({ type: 'jpeg', quality: 92 });
  ffmpeg.stdin.write(buf);
}
```

### 3. Verificar el tipo de animaciones que usa tu demo

| Tipo | ¿Funciona con virtual time? | Notas |
|---|---|---|
| Framer Motion | ✅ Sí, **si deshabilitas WAAPI** (`delete Element.prototype.animate`) | Ya está en el script |
| CSS transitions | ❌ No — usan el compositor en tiempo real | Tendrías que sustituirlas por JS |
| CSS keyframes (`@keyframes`) | ❌ No — igual que las transitions | Sustituir por JS |
| GSAP | ✅ Sí — usa `requestAnimationFrame` por defecto | Sin cambios |
| Lottie web | ⚠️ Depende del player — `lottie-web` con `setSpeed(0)` y `setSegment` controlable | Programable |
| `<video>` HTML | ❌ El elemento `<video>` usa su propio reloj | No usar para demos |
| Tippy.js / popovers | ✅ Sí (usan rAF) | OK |
| GIFs/PNGs | ✅ Sí | Estáticos |

**Si tu demo usa CSS transitions/keyframes**, antes de grabar deshabilítalas
inyectando este CSS desde el `addInitScript`:

```js
const style = document.createElement('style');
style.textContent = `
  * {
    transition: none !important;
    animation: none !important;
  }
`;
document.head.appendChild(style);
```

Y reimplementa las animaciones con Framer Motion / GSAP, o aceptar que se vean
"saltadas" en la grabación.

---

## Troubleshooting (errores reales que tuvimos)

### `✗ Could not reach the dev server at http://localhost:5180/...`

Vite cogió otro puerto porque el 5180 ya estaba ocupado. Comprueba la salida de
`npm run dev` — verás algo como `Local: http://localhost:5181/`. Luego:

```powershell
$env:PORT=5181; npm run record:investors
```

### El vídeo sale a 0.6 MB y queda en blanco

Significa que las animaciones no se ejecutan en absoluto. Causas posibles:

- **WAAPI no está realmente deshabilitada**: comprueba que el `delete
  Element.prototype.animate` corre dentro del `addInitScript` (no fuera).
- **Tu URL no tiene `?record`**: el override sólo se activa con ese flag.
  Verifica con: `await page.evaluate(() => typeof window.__tick)` → debe
  devolver `'function'`.

### Todos los títulos/elementos aparecen del tirón en el fotograma 1

WAAPI sigue activa. Confirma el `delete Element.prototype.animate` y prueba:

```js
await page.evaluate(() => 'animate' in Element.prototype);
// debe devolver false
```

Si devuelve `true`, el script no se está inyectando antes que Framer Motion.
El `context.addInitScript()` debe ir **antes** de `context.newPage()`.

### Los timers (setInterval del `Typewriter`) no disparan

Mira si la captura registra menos `tick` calls que los necesarios. Por ejemplo,
para un `setInterval(cb, 35)`, hacen falta al menos `35 / FRAME_MS ≈ 2` ticks
para que dispare. Si `FRAME_MS = 16.67`, dos ticks lo cubren. Si lo bajas a
30fps (`FRAME_MS = 33`), va justo.

### El screenshot tarda demasiado por fotograma

A 1920×1080 JPEG, suele rondar los 50-80 ms. Si va a >150 ms:
- Cierra apps que consuman GPU (Chrome con muchas pestañas, Discord, etc.)
- Usa `FPS=30` para reducir frames a la mitad
- Baja la calidad: `quality: 80` en vez de 92 (apenas se nota)

### El audio

`record.mjs` no captura audio (no estaba en el alcance de los anuncios). Para
una demo con voz en off:
- Graba el vídeo sin audio con este sistema
- Graba el audio aparte (Audacity, Garage Band, etc.)
- Mezcla con ffmpeg:
  ```bash
  ffmpeg -i demo.mp4 -i voz.wav -c:v copy -c:a aac -shortest demo-final.mp4
  ```

---

## Resumen rápido

```
1. ffmpeg + Playwright Chromium instalados.
2. Dev server corriendo en localhost:PUERTO.
3. node scripts/record.mjs <nombre-escena>
4. Esperar ~3-5 min mientras se grababa frame a frame.
5. Reproducir el MP4 en recordings/. Debe verse perfectamente fluido.
```

Tres claves técnicas que NO son obvias:

1. **Override de rAF/timers con `addInitScript`** (no funcionará si está fuera
   o si la URL no lleva `?record`).
2. **Borrar `Element.prototype.animate`** para deshabilitar WAAPI y forzar el
   animador JS de Framer Motion.
3. **Anclar el timeline al primer rAF** (no a `useEffect`) para que t=0 sea el
   primer fotograma capturado.
