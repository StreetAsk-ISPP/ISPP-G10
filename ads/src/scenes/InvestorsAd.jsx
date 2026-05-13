import React from 'react';
import { motion } from 'framer-motion';
import Background from '../components/Background.jsx';
import Logo from '../components/Logo.jsx';
import Scene from '../components/Scene.jsx';
import MapMock from '../components/MapMock.jsx';
import Counter from '../components/Counter.jsx';
import ProgressBar from '../components/ProgressBar.jsx';
import { Icon, IconChip } from '../components/Icon.jsx';
import { useTimeline } from '../components/useTimeline.js';
import { theme } from '../theme.js';

const SCENES = [
  { id: 'hook',     duration: 9  },
  { id: 'problem',  duration: 10 },
  { id: 'solution', duration: 10 },
  { id: 'market',   duration: 11 },
  { id: 'model',    duration: 11 },
  { id: 'cta',      duration: 9  },
];

export default function InvestorsAd() {
  const { currentIndex, progress, localTime } = useTimeline(SCENES);
  const scene = SCENES[currentIndex].id;

  return (
    <div style={{ position: 'absolute', inset: 0, color: theme.text }}>
      <Background variant="investors" />

      {/* Persistent top-left logo */}
      <div style={{ position: 'absolute', top: 48, left: 56, zIndex: 30 }}>
        <Logo size={56} />
      </div>
      <div style={{ position: 'absolute', top: 56, right: 64, zIndex: 30,
                    fontFamily: 'Space Grotesk', fontSize: 22, fontWeight: 700, color: theme.textDim, letterSpacing: 2 }}>
        DECK · INVESTORS · 2026
      </div>

      {/* === Scene 1: Hook === */}
      <Scene active={scene === 'hook'}>
        <Hook />
      </Scene>

      {/* === Scene 2: Problem === */}
      <Scene active={scene === 'problem'}>
        <Problem />
      </Scene>

      {/* === Scene 3: Solution === */}
      <Scene active={scene === 'solution'}>
        <Solution />
      </Scene>

      {/* === Scene 4: Market === */}
      <Scene active={scene === 'market'}>
        <Market localTime={localTime} />
      </Scene>

      {/* === Scene 5: Business model === */}
      <Scene active={scene === 'model'}>
        <Model localTime={localTime} />
      </Scene>

      {/* === Scene 6: CTA === */}
      <Scene active={scene === 'cta'}>
        <CTA />
      </Scene>

      <ProgressBar progress={progress} />
    </div>
  );
}

/* ---------- Scene 1 ---------- */
function Hook() {
  return (
    <div style={center()}>
      <motion.div
        initial={{ opacity: 0, letterSpacing: '0.4em' }}
        animate={{ opacity: 1, letterSpacing: '0.08em' }}
        transition={{ duration: 1.2 }}
        style={{ fontSize: 30, color: theme.textDim, fontWeight: 700, textTransform: 'uppercase' }}
      >
        Hiperlocal · Tiempo real · Comunidad
      </motion.div>

      <motion.h1
        initial={{ opacity: 0, y: 30 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 0.4, duration: 0.8 }}
        style={{
          fontFamily: 'Space Grotesk',
          fontSize: 132, fontWeight: 800, lineHeight: 1.02,
          textAlign: 'center', margin: '32px 0', letterSpacing: -3,
        }}
      >
        La gente ya no le<br/>pregunta a Google.
      </motion.h1>

      <motion.h2
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        transition={{ delay: 1.6, duration: 0.8 }}
        style={{
          fontFamily: 'Space Grotesk', fontSize: 96, fontWeight: 800,
          background: `linear-gradient(135deg, ${theme.red}, ${theme.purple})`,
          WebkitBackgroundClip: 'text', WebkitTextFillColor: 'transparent',
          letterSpacing: -2, margin: 0,
        }}
      >
        Pregunta al barrio.
      </motion.h2>

      <motion.div
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        transition={{ delay: 3, duration: 1 }}
        style={{ marginTop: 48, fontSize: 32, fontWeight: 600, color: theme.textDim, maxWidth: 900, textAlign: 'center' }}
      >
        StreetAsk convierte cada calle en un mini-foro en vivo.
      </motion.div>
    </div>
  );
}

/* ---------- Scene 2 ---------- */
function Problem() {
  const items = [
    { icon: 'clock', color: theme.red,    t: 'Google tarda', s: 'Y la respuesta es genérica, no del lugar.' },
    { icon: 'pin',   color: theme.purple, t: 'Las redes no son hiperlocales', s: 'Twitter / Reddit no responden por barrio.' },
    { icon: 'store', color: theme.gold,   t: 'Los negocios locales no llegan', s: 'No tienen herramientas para activar tráfico real.' },
  ];
  return (
    <div style={{ ...center(), padding: '0 120px' }}>
      <SectionTitle eyebrow="EL PROBLEMA" title="La información local está rota." />
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 32, marginTop: 56, width: '100%' }}>
        {items.map((it, i) => (
          <motion.div
            key={i}
            initial={{ opacity: 0, y: 40 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.4 + i * 0.25, duration: 0.7 }}
            style={card()}
          >
            <IconChip name={it.icon} color={it.color} size={72} />
            <div style={{ fontSize: 36, fontWeight: 800, marginTop: 20 }}>{it.t}</div>
            <div style={{ fontSize: 26, fontWeight: 600, color: theme.textDim, marginTop: 8 }}>{it.s}</div>
          </motion.div>
        ))}
      </div>
    </div>
  );
}

/* ---------- Scene 3 ---------- */
function Solution() {
  return (
    <div style={{ position: 'absolute', inset: 0, display: 'grid', gridTemplateColumns: '1fr 1fr', alignItems: 'center', padding: '0 120px', gap: 60 }}>
      <div>
        <SectionTitle eyebrow="LA SOLUCIÓN" title="Pregunta. Responde. En el momento." align="left" />
        <ul style={{ listStyle: 'none', padding: 0, margin: '40px 0 0', fontSize: 32, fontWeight: 600, lineHeight: 1.7 }}>
          {[
            { ic: 'pin',    c: theme.red,    t: 'Preguntas ancladas a un radio (50m–1km).' },
            { ic: 'chat',   c: theme.purple, t: 'Mini-foros que viven mientras importan (1–24h).' },
            { ic: 'shield', c: theme.gold,   t: 'Respuestas verificadas de negocios.' },
            { ic: 'coin',   c: theme.green,  t: 'Monedas StreetCoin por contribuir.' },
          ].map((row, i) => (
            <motion.li key={i}
              initial={{ opacity: 0, x: -30 }}
              animate={{ opacity: 1, x: 0 }}
              transition={{ delay: 0.4 + i * 0.18 }}
              style={{ display: 'flex', gap: 18, alignItems: 'center', padding: '10px 0' }}>
              <IconChip name={row.ic} color={row.c} size={48} />
              <span>{row.t}</span>
            </motion.li>
          ))}
        </ul>
      </div>
      <motion.div
        initial={{ opacity: 0, scale: 0.9, rotate: -2 }}
        animate={{ opacity: 1, scale: 1, rotate: 0 }}
        transition={{ duration: 0.8 }}
        style={{ display: 'grid', placeItems: 'center' }}
      >
        <MapMock width={780} height={520} showEvents />
      </motion.div>
    </div>
  );
}

/* ---------- Scene 4: Market sizing ---------- */
function Market({ localTime }) {
  const start = localTime > 0.5;
  const stats = [
    { label: 'TAM · Publicidad geolocalizada', value: 12, suffix: ' B €', decimals: 0, hint: 'Interacción hiperlocal y eventos urbanos' },
    { label: 'SAM · España · Portugal · Italia', value: 0.9, suffix: ' B €', decimals: 1, hint: 'Sur de Europa · alta densidad urbana' },
    { label: 'SOM · Medio plazo', value: 5, suffix: ' M €', decimals: 0, hint: '~250k MAU · 1,2 € ARPU' },
  ];
  return (
    <div style={{ ...center(), padding: '0 120px' }}>
      <SectionTitle eyebrow="MERCADO" title="Una oportunidad hiperlocal global." />
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 32, marginTop: 56, width: '100%' }}>
        {stats.map((s, i) => (
          <motion.div key={i} style={{ ...card(), alignItems: 'center', textAlign: 'center' }}
            initial={{ opacity: 0, y: 40 }} animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.2 + i * 0.2, duration: 0.6 }}>
            <div style={{ fontSize: 24, color: theme.textDim, letterSpacing: 2, fontWeight: 700 }}>{s.label}</div>
            <div style={{
              fontFamily: 'Space Grotesk', fontSize: 96, fontWeight: 800, lineHeight: 1,
              background: `linear-gradient(135deg, ${theme.red}, ${theme.purple})`,
              WebkitBackgroundClip: 'text', WebkitTextFillColor: 'transparent',
              margin: '16px 0',
            }}>
              <Counter to={s.value} suffix={s.suffix} decimals={s.decimals} duration={2} start={start} />
            </div>
            <div style={{ fontSize: 24, fontWeight: 600, color: theme.textDim }}>{s.hint}</div>
          </motion.div>
        ))}
      </div>
      <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} transition={{ delay: 1.6 }}
        style={{ marginTop: 56, fontSize: 26, fontWeight: 600, color: theme.textDim }}>
        Fuentes: estimación interna · Statista local search · GMV eventos UE 2025.
      </motion.div>
    </div>
  );
}

/* ---------- Scene 5: Business model ---------- */
function Model({ localTime }) {
  const start = localTime > 0.4;
  const plans = [
    {
      name: 'Free', price: '0', tag: 'Adquisición',
      bullets: ['3 preguntas diarias', 'Visibilidad básica', 'Anuncios intersticiales', 'Radio limitado'],
      color: '#4a4a55',
    },
    {
      name: 'Premium', price: '2,99', tag: 'Conversión 4,2%',
      bullets: ['Ausencia de anuncios', 'Mayor radio de interacción', 'Preguntas ilimitadas'],
      color: theme.red, highlight: true,
    },
    {
      name: 'Business', price: '19,99', tag: 'B2B recurrente',
      bullets: ['Distintivo verificado', 'Creación de eventos', 'Respuestas priorizadas', 'Mayor visibilidad'],
      color: theme.gold,
    },
  ];
  return (
    <div style={{ ...center(), padding: '0 120px' }}>
      <SectionTitle eyebrow="MODELO DE NEGOCIO" title="Tres motores que se refuerzan." />
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 28, marginTop: 48, width: '100%' }}>
        {plans.map((p, i) => (
          <motion.div key={i}
            initial={{ opacity: 0, y: 50 }} animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.2 + i * 0.2, duration: 0.7 }}
            style={{
              ...card(),
              background: p.highlight
                ? `linear-gradient(160deg, ${theme.red}33, ${theme.purple}33)`
                : theme.panel,
              border: `1px solid ${p.highlight ? theme.red : theme.border}`,
              transform: p.highlight ? 'scale(1.04)' : 'none',
            }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <div style={{ fontSize: 36, fontWeight: 800 }}>{p.name}</div>
              <div style={{
                padding: '6px 14px', borderRadius: 999, fontSize: 18, fontWeight: 800,
                background: `${p.color}22`, color: p.color, border: `1px solid ${p.color}55`,
              }}>{p.tag}</div>
            </div>
            <div style={{ fontFamily: 'Space Grotesk', fontSize: 84, fontWeight: 800, marginTop: 8 }}>
              {p.price}<span style={{ fontSize: 32, color: theme.textDim, fontWeight: 700 }}> €/mes</span>
            </div>
            <div style={{ marginTop: 16 }}>
              {p.bullets.map((b, j) => (
                <div key={j} style={{ display: 'flex', gap: 10, padding: '6px 0', fontSize: 24, fontWeight: 600 }}>
                  <span style={{ color: p.color, fontWeight: 800 }}>✓</span>{b}
                </div>
              ))}
            </div>
          </motion.div>
        ))}
      </div>

      <motion.div
        initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 1.4 }}
        style={{ marginTop: 40, display: 'flex', gap: 60, justifyContent: 'center', flexWrap: 'wrap' }}
      >
        <KPI label="Ingreso neto @10k MAU" value={13000} prefix="" suffix=" €" start={start} />
        <KPI label="Recuperación de inversión" value={6} suffix=" meses" decimals={0} start={start} />
        <KPI label="Coste infra @10k MAU" value={500} suffix=" €/mes" start={start} />
      </motion.div>
    </div>
  );
}

/* ---------- Scene 6: CTA ---------- */
function CTA() {
  return (
    <div style={center()}>
      <motion.div
        initial={{ opacity: 0, scale: 0.7 }} animate={{ opacity: 1, scale: 1 }}
        transition={{ duration: 0.8 }}
      >
        <Logo size={140} />
      </motion.div>
      <motion.h1
        initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 0.6, duration: 0.8 }}
        style={{
          fontFamily: 'Space Grotesk', fontSize: 110, fontWeight: 800,
          margin: '40px 0 8px', textAlign: 'center', letterSpacing: -2,
          background: `linear-gradient(135deg, ${theme.red}, ${theme.purple})`,
          WebkitBackgroundClip: 'text', WebkitTextFillColor: 'transparent',
        }}>
        Únete a la ronda.
      </motion.h1>
      <motion.div
        initial={{ opacity: 0 }} animate={{ opacity: 1 }}
        transition={{ delay: 1.2, duration: 0.8 }}
        style={{ fontSize: 38, fontWeight: 600, color: theme.textDim, textAlign: 'center', maxWidth: 1100 }}>
        Inversión total estimada <strong style={{ color: theme.text, fontWeight: 800 }}>84.181 €</strong> para alcanzar
        10k MAU activos y recuperar la inversión en ~6 meses.
      </motion.div>

      <motion.div
        initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 2, duration: 0.8 }}
        style={{ marginTop: 56, display: 'flex', gap: 24, alignItems: 'center' }}>
        <div style={{
          padding: '22px 44px', borderRadius: 999,
          background: `linear-gradient(135deg, ${theme.red}, ${theme.purple})`,
          fontSize: 30, fontWeight: 700, color: '#ffffff',
          boxShadow: `0 20px 60px ${theme.red}55`,
        }}>invest@streetask.app</div>
        <div style={{ fontSize: 24, fontWeight: 700, color: theme.textDim }}>streetask.app</div>
      </motion.div>
    </div>
  );
}

/* ---------- helpers ---------- */
function SectionTitle({ eyebrow, title, align = 'center' }) {
  return (
    <div style={{ textAlign: align, width: '100%' }}>
      <motion.div
        initial={{ opacity: 0, y: -10 }} animate={{ opacity: 1, y: 0 }}
        style={{ fontSize: 28, color: theme.red, fontWeight: 800, letterSpacing: 4, textTransform: 'uppercase' }}>
        {eyebrow}
      </motion.div>
      <motion.h2
        initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 0.15 }}
        style={{
          fontFamily: 'Space Grotesk', fontSize: 92, fontWeight: 800, lineHeight: 1.05,
          margin: '12px 0 0', letterSpacing: -2,
        }}>
        {title}
      </motion.h2>
    </div>
  );
}

function KPI({ label, value, prefix = '', suffix = '', decimals = 0, start }) {
  return (
    <div style={{ textAlign: 'center' }}>
      <div style={{
        fontFamily: 'Space Grotesk', fontSize: 62, fontWeight: 800,
        color: theme.green,
      }}>
        <Counter to={value} prefix={prefix} suffix={suffix} decimals={decimals} duration={1.8} start={start} />
      </div>
      <div style={{ fontSize: 24, fontWeight: 700, color: theme.textDim, marginTop: 4, letterSpacing: 1 }}>{label}</div>
    </div>
  );
}

function center() {
  return {
    position: 'absolute', inset: 0,
    display: 'flex', flexDirection: 'column',
    alignItems: 'center', justifyContent: 'center',
    padding: '0 80px',
  };
}
function card() {
  return {
    background: theme.panel, border: `1px solid ${theme.border}`,
    borderRadius: 24, padding: 32,
    backdropFilter: 'blur(12px)',
    display: 'flex', flexDirection: 'column',
  };
}
