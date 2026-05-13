# StreetAsk · Ads

Mini-proyecto **aislado** del frontend principal para producir dos anuncios animados:

- `#/investors` — pitch para inversores (~60 s)
- `#/clients` — anuncio para usuarios + business (~60 s)

Ambos se renderizan a 1920×1080 sobre un escenario fijo (`Stage`) que se escala al viewport, así el resultado grabado es siempre 1080p.

## Setup

```bash
cd ads
npm install
npm run dev
```

Abre http://localhost:5180 — verás el índice con los dos anuncios.

## Grabar a vídeo (Playwright)

Asegúrate de que el dev server está corriendo (`npm run dev`), luego en otra terminal:

```bash
npm run record:investors    # genera recordings/investors-*.webm
npm run record:clients      # genera recordings/clients-*.webm
npm run record:all          # ambos
```

El primer uso descarga el binario de Chromium para Playwright (~150 MB). Si quieres saltarte ese paso porque ya tienes Chrome / OBS:

1. `npm run dev`
2. Abre el anuncio (por ejemplo `http://localhost:5180/#/investors`) a pantalla completa.
3. Graba con OBS / la grabación de pantalla del SO.

## Convertir a MP4

Playwright sólo escribe `.webm` (VP8). Si necesitas MP4 para subir a redes:

```bash
ffmpeg -i recordings/investors-XXX.webm -c:v libx264 -crf 18 -preset slow investors.mp4
```

## Cómo modificar contenido

- Cifras (precios, KPI, ronda) → `src/scenes/InvestorsAd.jsx` y `src/scenes/ClientsAd.jsx`.
- Duración por escena → array `SCENES` en cada anuncio (en segundos). Si tocas duraciones, también ajusta `DURATIONS` en `scripts/record.mjs`.
- Paleta de colores → `src/theme.js`.
