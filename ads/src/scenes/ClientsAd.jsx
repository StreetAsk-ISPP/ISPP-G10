import React from 'react';
import { motion } from 'framer-motion';
import Background from '../components/Background.jsx';
import Logo from '../components/Logo.jsx';
import Scene from '../components/Scene.jsx';
import MapMock from '../components/MapMock.jsx';
import PhoneFrame from '../components/PhoneFrame.jsx';
import ProgressBar from '../components/ProgressBar.jsx';
import Counter from '../components/Counter.jsx';
import Person from '../components/Person.jsx';
import SpeechBubble from '../components/SpeechBubble.jsx';
import { Icon, IconChip } from '../components/Icon.jsx';
import { useTimeline } from '../components/useTimeline.js';
import { theme } from '../theme.js';

const SCENES = [
  { id: 'intro',     duration: 8  },
  { id: 'userAsk',   duration: 11 },
  { id: 'userThread',duration: 10 },
  { id: 'rewards',   duration: 7  },
  { id: 'bizIntro',  duration: 7  },
  { id: 'bizDash',   duration: 10 },
  { id: 'outro',     duration: 7  },
];

export default function ClientsAd() {
  const { currentIndex, progress, localTime } = useTimeline(SCENES);
  const scene = SCENES[currentIndex].id;

  return (
    <div style={{ position: 'absolute', inset: 0, color: '#fff' }}>
      <Background variant="clients" />

      <div style={{ position: 'absolute', top: 48, left: 56, zIndex: 30 }}>
        <Logo size={56} />
      </div>

      <Scene active={scene === 'intro'}><Intro /></Scene>
      <Scene active={scene === 'userAsk'}><UserAsk /></Scene>
      <Scene active={scene === 'userThread'}><UserThread /></Scene>
      <Scene active={scene === 'rewards'}><Rewards localTime={localTime} /></Scene>
      <Scene active={scene === 'bizIntro'}><BizIntro /></Scene>
      <Scene active={scene === 'bizDash'}><BizDash localTime={localTime} /></Scene>
      <Scene active={scene === 'outro'}><Outro /></Scene>

      <ProgressBar progress={progress} />
    </div>
  );
}

/* ============================================================
   Scene 1 — INTRO
   Two signage-style people in the lower-corners asking real questions.
   Bubbles are placed in stage-absolute coordinates at the TOP of the
   characters, well clear of their heads, so the text never overlaps.
   ============================================================ */
function Intro() {
  // People sit anchored to the floor; their heads end roughly at:
  // figure size 360 → head top ≈ stage bottom - (360 * 0.95)
  // So we keep bubbles above y=360 to never collide with heads.
  return (
    <div style={{ position: 'absolute', inset: 0 }}>
      {/* Left character: girl with pigtails */}
      <motion.div
        initial={{ opacity: 0, x: -120 }} animate={{ opacity: 1, x: 0 }}
        transition={{ duration: 0.8 }}
        style={{ position: 'absolute', left: 130, bottom: 0, zIndex: 4 }}
      >
        <Person variant="womanPony" color="#ffffff" size={360} />
      </motion.div>
      <div style={{ position: 'absolute', left: 130, top: 540, zIndex: 6 }}>
        <SpeechBubble tailSide="left" delay={0.6} color={theme.red}>
          ¿Hay cola en ese bar?
        </SpeechBubble>
      </div>

      {/* Right character: man with cap */}
      <motion.div
        initial={{ opacity: 0, x: 120 }} animate={{ opacity: 1, x: 0 }}
        transition={{ duration: 0.8, delay: 0.2 }}
        style={{ position: 'absolute', right: 130, bottom: 0, zIndex: 4 }}
      >
        <Person variant="manCap" color="#ffffff" size={360} flip />
      </motion.div>
      <div style={{ position: 'absolute', right: 130, top: 540, zIndex: 6 }}>
        <SpeechBubble tailSide="right" delay={1.4} color={theme.purple}>
          ¿Cómo está la playa hoy?
        </SpeechBubble>
      </div>

      {/* Centered tagline (between the two characters, above floor line) */}
      <div style={{
        position: 'absolute', left: 0, right: 0, top: 120,
        display: 'flex', flexDirection: 'column',
        alignItems: 'center', zIndex: 5, pointerEvents: 'none',
      }}>
        <motion.div
          initial={{ opacity: 0 }} animate={{ opacity: 1 }}
          transition={{ delay: 2.6, duration: 0.6 }}
          style={{
            fontSize: 26, color: theme.textDim, fontWeight: 600,
            letterSpacing: 4, textTransform: 'uppercase',
          }}>
          Lo que pasa en tu calle, ahora.
        </motion.div>
        <motion.div
          initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 3.0, duration: 0.7 }}
          style={{
            marginTop: 24,
            fontFamily: 'Space Grotesk',
            fontSize: 168, fontWeight: 800, letterSpacing: -4, lineHeight: 1,
            background: `linear-gradient(135deg, ${theme.red}, ${theme.purple})`,
            WebkitBackgroundClip: 'text', WebkitTextFillColor: 'transparent',
          }}>
          Pregúntalo.
        </motion.div>
        <motion.div
          initial={{ opacity: 0 }} animate={{ opacity: 1 }}
          transition={{ delay: 3.8, duration: 0.6 }}
          style={{ marginTop: 16, fontSize: 36, color: '#fff', fontWeight: 500 }}>
          En StreetAsk, te responde el <strong>barrio</strong>.
        </motion.div>
      </div>
    </div>
  );
}

/* ============================================================
   Scene 2 — USER ASK
   Left column: title + map mock.
   Right column: phone composer.
   The character + speech bubble live in the top-right corner so that
   nothing overlaps the phone or the title.
   ============================================================ */
function UserAsk() {
  return (
    <div style={{ position: 'absolute', inset: 0 }}>
      {/* Title + map */}
      <div style={{ position: 'absolute', top: 160, left: 100, maxWidth: 760 }}>
        <SectionTitle eyebrow="USUARIO" title="Pregunta a 500 m a la redonda." align="left" />
        <div style={{ marginTop: 24, fontSize: 28, color: theme.textDim, maxWidth: 640 }}>
          Sólo la gente que está cerca te responde. Como preguntar al vecino, pero a escala.
        </div>
        <motion.div
          initial={{ opacity: 0, scale: 0.94 }}
          animate={{ opacity: 1, scale: 1 }}
          transition={{ delay: 0.4 }}
          style={{ marginTop: 36 }}
        >
          <MapMock width={680} height={420} />
        </motion.div>
      </div>

      {/* Phone — moved a bit left to make room for the character */}
      <motion.div
        initial={{ opacity: 0, x: 80 }} animate={{ opacity: 1, x: 0 }}
        transition={{ delay: 0.4, duration: 0.7 }}
        style={{ position: 'absolute', right: 320, top: 200 }}
      >
        <PhoneFrame width={340}>
          <ComposerScreen />
        </PhoneFrame>
      </motion.div>

      {/* Speech bubble floats above the phone (the "asker's thought").
          High enough that the phone screen reads cleanly. */}
      <div style={{ position: 'absolute', right: 220, top: 90, zIndex: 6 }}>
        <SpeechBubble tailSide="right" delay={1.2} color={theme.red}>
          ¿Hay cola en La Bodega?
        </SpeechBubble>
      </div>

      {/* Character: smaller, anchored bottom-right corner — fully visible */}
      <motion.div
        initial={{ opacity: 0, x: 80 }} animate={{ opacity: 1, x: 0 }}
        transition={{ delay: 0.7 }}
        style={{ position: 'absolute', right: 100, bottom: 0 }}
      >
        <Person variant="womanPony" color="#ffffff" size={220} flip />
      </motion.div>
    </div>
  );
}

function ComposerScreen() {
  const text = '¿Hay cola en La Bodega ahora mismo?';
  return (
    <div style={{ width: '100%', height: '100%', padding: '70px 22px 22px',
                  display: 'flex', flexDirection: 'column' }}>
      <div style={{ fontSize: 14, color: theme.textDim }}>Nueva pregunta</div>
      <div style={{ fontSize: 22, fontWeight: 700, marginTop: 4 }}>Pregunta al barrio</div>

      <div style={{
        marginTop: 18, padding: 16, borderRadius: 16,
        background: 'rgba(255,255,255,0.06)', border: `1px solid ${theme.border}`,
        minHeight: 120, fontSize: 20, lineHeight: 1.4,
      }}>
        <Typewriter text={text} />
      </div>

      <div style={{ marginTop: 16 }}>
        <div style={{ fontSize: 13, color: theme.textDim, display: 'flex', alignItems: 'center', gap: 6 }}>
          <Icon name="pin" size={14} /> Radio de pregunta
        </div>
        <div style={{ marginTop: 10, height: 6, borderRadius: 999,
                      background: 'rgba(255,255,255,0.1)', position: 'relative' }}>
          <motion.div
            initial={{ width: '20%' }} animate={{ width: '50%' }}
            transition={{ duration: 1.4, delay: 1.6 }}
            style={{ height: '100%', borderRadius: 999,
                     background: `linear-gradient(90deg, ${theme.red}, ${theme.purple})` }}
          />
          <motion.div
            initial={{ left: '20%' }} animate={{ left: '50%' }}
            transition={{ duration: 1.4, delay: 1.6 }}
            style={{ position: 'absolute', top: -5, width: 16, height: 16,
                     borderRadius: '50%', background: '#fff',
                     boxShadow: '0 2px 6px rgba(0,0,0,0.4)' }}
          />
        </div>
        <div style={{ marginTop: 8, fontSize: 13, color: theme.textDim, textAlign: 'right' }}>500 m</div>
      </div>

      <div style={{ marginTop: 16 }}>
        <div style={{ fontSize: 13, color: theme.textDim }}>Tema</div>
        <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap', marginTop: 8 }}>
          {[
            { label: 'Bares', icon: 'beer' },
            { label: 'Eventos', icon: 'music' },
            { label: 'Movilidad', icon: 'bus' },
          ].map((t, i) => (
            <motion.div key={i}
              initial={{ opacity: 0, scale: 0.8 }}
              animate={{ opacity: 1, scale: 1 }}
              transition={{ delay: 2 + i * 0.2 }}
              style={{
                display: 'flex', alignItems: 'center', gap: 6,
                padding: '8px 14px', borderRadius: 999,
                fontSize: 14, fontWeight: 600,
                background: i === 0 ? `${theme.red}33` : 'rgba(255,255,255,0.08)',
                border: `1px solid ${i === 0 ? theme.red : theme.border}`,
                color: i === 0 ? '#fff' : theme.textDim,
              }}>
              <Icon name={t.icon} size={14} />
              {t.label}
            </motion.div>
          ))}
        </div>
      </div>

      <motion.div
        initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 3.2 }}
        style={{
          marginTop: 'auto', padding: '16px 0', borderRadius: 16,
          background: `linear-gradient(135deg, ${theme.red}, ${theme.purple})`,
          textAlign: 'center', fontWeight: 700, fontSize: 18,
          boxShadow: `0 10px 30px ${theme.red}66`,
        }}>
        Publicar pregunta
      </motion.div>
    </div>
  );
}

function Typewriter({ text }) {
  const [shown, setShown] = React.useState('');
  React.useEffect(() => {
    let i = 0;
    const id = setInterval(() => {
      i++;
      setShown(text.slice(0, i));
      if (i >= text.length) clearInterval(id);
    }, 35);
    return () => clearInterval(id);
  }, [text]);
  return (
    <span>{shown}
      <motion.span animate={{ opacity: [1, 0] }}
        transition={{ duration: 0.5, repeat: Infinity }}>|</motion.span>
    </span>
  );
}

/* ============================================================
   Scene 3 — USER THREAD
   ============================================================ */
function UserThread() {
  const answers = [
    { user: 'Marta · 230 m', time: 'hace 1 min', text: 'Sin cola, acaba de abrir. Está vacío.', likes: 12, top: true, person: 'womanPony' },
    { user: 'Carlos · 410 m', time: 'hace 2 min', text: 'Yo paso por allí en 5, te confirmo.', likes: 4, person: 'manCap' },
    { user: 'La Bodega', time: 'hace 3 min', text: 'Hola! Mesas libres en terraza. Reservas en bio.', likes: 28, verified: true, person: 'businessman' },
  ];
  return (
    <div style={{ position: 'absolute', inset: 0, display: 'grid',
                  gridTemplateColumns: '1fr 1.4fr', alignItems: 'center',
                  padding: '0 120px', gap: 60 }}>
      <div>
        <SectionTitle eyebrow="MINI-FORO EN VIVO" title="Respuestas reales en segundos." align="left" />
        <div style={{ marginTop: 24, fontSize: 24, color: theme.textDim, maxWidth: 520 }}>
          Vota lo útil. Lo que más sube, gana. Y los negocios verificados aparecen marcados.
        </div>
      </div>
      <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
        <motion.div
          initial={{ opacity: 0, y: -20 }} animate={{ opacity: 1, y: 0 }}
          style={{
            padding: 24, borderRadius: 20,
            background: theme.panelStrong, border: `1px solid ${theme.border}`,
          }}>
          <div style={{ display: 'flex', justifyContent: 'space-between',
                        color: theme.textDim, fontSize: 16, alignItems: 'center' }}>
            <span style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
              <Icon name="pin" size={16} /> Triana · 500 m
            </span>
            <span style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
              <Icon name="clock" size={16} /> caduca en 5h 42m
            </span>
          </div>
          <div style={{ fontSize: 32, fontWeight: 700, marginTop: 10 }}>
            ¿Hay cola en La Bodega ahora mismo?
          </div>
        </motion.div>

        {answers.map((a, i) => (
          <motion.div key={i}
            initial={{ opacity: 0, x: 40 }}
            animate={{ opacity: 1, x: 0 }}
            transition={{ delay: 0.4 + i * 0.6, duration: 0.5 }}
            style={{
              padding: 20, borderRadius: 18,
              background: a.top ? `${theme.green}15` : theme.panel,
              border: `1px solid ${a.top ? theme.green : theme.border}`,
              display: 'grid', gridTemplateColumns: 'auto 1fr auto',
              gap: 16, alignItems: 'center',
            }}>
            <div style={{
              width: 56, height: 56, borderRadius: '50%',
              background: 'rgba(255,255,255,0.08)', overflow: 'hidden',
              display: 'grid', placeItems: 'end center',
            }}>
              <Person variant={a.person} color="#fff" size={70} bob={false} />
            </div>
            <div>
              <div style={{ display: 'flex', gap: 10, alignItems: 'center', marginBottom: 6 }}>
                <div style={{ fontWeight: 700, fontSize: 18 }}>{a.user}</div>
                {a.verified && (
                  <span style={{
                    display: 'inline-flex', alignItems: 'center', gap: 4,
                    fontSize: 12, padding: '3px 10px', borderRadius: 999,
                    background: `${theme.gold}33`, color: theme.gold,
                    fontWeight: 700, border: `1px solid ${theme.gold}66`,
                  }}>
                    <Icon name="shield" size={12} /> NEGOCIO VERIFICADO
                  </span>
                )}
                {a.top && (
                  <span style={{
                    display: 'inline-flex', alignItems: 'center', gap: 4,
                    fontSize: 12, padding: '3px 10px', borderRadius: 999,
                    background: `${theme.green}33`, color: theme.green, fontWeight: 700,
                  }}>
                    <Icon name="trending" size={12} /> TOP
                  </span>
                )}
                <div style={{ marginLeft: 'auto', color: theme.textFaint, fontSize: 14 }}>
                  {a.time}
                </div>
              </div>
              <div style={{ fontSize: 22, lineHeight: 1.4 }}>{a.text}</div>
            </div>
            <motion.div
              animate={{ y: [0, -6, 0] }} transition={{ duration: 1.4, repeat: Infinity }}
              style={{ display: 'flex', flexDirection: 'column',
                       alignItems: 'center', gap: 4, color: a.top ? theme.green : '#fff' }}>
              <Icon name="thumbUp" size={26} />
              <div style={{ fontWeight: 700 }}>
                <Counter to={a.likes} duration={1.4} />
              </div>
            </motion.div>
          </motion.div>
        ))}
      </div>
    </div>
  );
}

/* ============================================================
   Scene 4 — REWARDS
   ============================================================ */
function Rewards({ localTime }) {
  return (
    <div style={center()}>
      <SectionTitle eyebrow="GAMIFICACIÓN" title="Responde. Gana StreetCoins." />

      <motion.div
        initial={{ opacity: 0, scale: 0.6, rotate: -20 }}
        animate={{ opacity: 1, scale: 1, rotate: 0 }}
        transition={{ duration: 0.8, type: 'spring' }}
        style={{
          marginTop: 32, width: 220, height: 220, borderRadius: '50%',
          background: `radial-gradient(circle at 30% 30%, #ffd54a, ${theme.gold} 70%)`,
          display: 'grid', placeItems: 'center',
          boxShadow: `0 30px 80px ${theme.gold}66, inset 0 0 0 8px rgba(255,255,255,0.2)`,
          color: '#3a2a00',
        }}>
        <Icon name="coin" size={120} strokeWidth={2.4} />
      </motion.div>

      <div style={{ marginTop: 36, display: 'flex', gap: 60 }}>
        {[
          { v: '+1', t: 'por respuesta útil', icon: 'chat' },
          { v: '+1', t: 'extra si más likes que dislikes', icon: 'thumbUp' },
          { v: 'Canjea', t: 'premium · badges · perks', icon: 'gift' },
        ].map((it, i) => (
          <motion.div key={i}
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.6 + i * 0.2 }}
            style={{ textAlign: 'center', display: 'flex',
                     flexDirection: 'column', alignItems: 'center', gap: 12 }}>
            <IconChip name={it.icon} color={theme.gold} size={64} />
            <div style={{ fontFamily: 'Space Grotesk', fontSize: 56,
                          fontWeight: 800, color: theme.gold }}>{it.v}</div>
            <div style={{ fontSize: 20, color: theme.textDim }}>{it.t}</div>
          </motion.div>
        ))}
      </div>
    </div>
  );
}

/* ============================================================
   Scene 5 — BIZ INTRO
   ============================================================ */
function BizIntro() {
  return (
    <div style={{ position: 'absolute', inset: 0 }}>
      {/* Title block on the left */}
      <div style={{
        position: 'absolute', left: 100, top: 200, maxWidth: 1100, zIndex: 5,
      }}>
        <motion.div
          initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }}
          style={{ fontSize: 24, color: theme.gold, fontWeight: 700,
                   letterSpacing: 4, textTransform: 'uppercase' }}>
          Para negocios
        </motion.div>
        <motion.h1
          initial={{ opacity: 0, y: 30 }} animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.3 }}
          style={{
            fontFamily: 'Space Grotesk', fontSize: 124, fontWeight: 800,
            lineHeight: 1.02, letterSpacing: -3, margin: '24px 0',
          }}>
          Tu evento, en el<br/>
          <span style={{
            background: `linear-gradient(135deg, ${theme.gold}, ${theme.red})`,
            WebkitBackgroundClip: 'text', WebkitTextFillColor: 'transparent',
          }}>radar del barrio.</span>
        </motion.h1>
        <motion.div
          initial={{ opacity: 0 }} animate={{ opacity: 1 }} transition={{ delay: 1.2 }}
          style={{ marginTop: 12, fontSize: 30, color: theme.textDim, maxWidth: 900 }}>
          Publica eventos, responde como negocio verificado, mide el impacto en directo.
        </motion.div>
      </div>

      {/* Businessman bottom-right, smaller, with bubble high above his head */}
      <motion.div
        initial={{ opacity: 0, x: 100 }} animate={{ opacity: 1, x: 0 }}
        transition={{ duration: 0.8 }}
        style={{ position: 'absolute', right: 160, bottom: 40, zIndex: 4 }}
      >
        <Person variant="businessman" color="#ffffff" size={320} flip />
      </motion.div>
      <div style={{ position: 'absolute', right: 180, bottom: 380, zIndex: 6 }}>
        <SpeechBubble tailSide="right" delay={1} color={theme.gold}>
          Quiero llenar mi local hoy.
        </SpeechBubble>
      </div>
    </div>
  );
}

/* ============================================================
   Scene 6 — BIZ DASH
   ============================================================ */
function BizDash({ localTime }) {
  const start = localTime > 0.3;
  return (
    <div style={{ position: 'absolute', inset: 0, display: 'grid',
                  gridTemplateColumns: '1fr 1fr', alignItems: 'center',
                  padding: '0 100px', gap: 40 }}>
      <div>
        <SectionTitle eyebrow="DASHBOARD BUSINESS" title="Mide qué funciona, en directo." align="left" />
        <div style={{ marginTop: 28, display: 'grid',
                      gridTemplateColumns: 'repeat(2, 1fr)', gap: 20 }}>
          <Stat label="Asistentes confirmados" icon="users" value={284} start={start} accent={theme.green} />
          <Stat label="Vistas en mapa" icon="globe" value={6420} start={start} accent={theme.purple} />
          <Stat label="Preguntas respondidas" icon="chat" value={92} start={start} accent={theme.red} />
          <Stat label="Boost ROAS" icon="trending" value={4.6} suffix="x" decimals={1} start={start} accent={theme.gold} />
        </div>
        <motion.div
          initial={{ opacity: 0 }} animate={{ opacity: 1 }} transition={{ delay: 1.4 }}
          style={{ marginTop: 24, fontSize: 22, color: theme.textDim,
                   display: 'flex', alignItems: 'center', gap: 14, flexWrap: 'wrap' }}>
          <span style={{ display: 'inline-flex', alignItems: 'center', gap: 8 }}>
            <Icon name="store" size={20} /> Cuenta Business
          </span>
          <strong style={{ color: '#fff' }}>19,99 €/mes</strong>
          <span style={{ color: theme.textFaint }}>·</span>
          <span style={{ display: 'inline-flex', alignItems: 'center', gap: 8 }}>
            <Icon name="bolt" size={20} /> Boost por evento
          </span>
          <strong style={{ color: theme.gold }}>14,99 €</strong>
        </motion.div>
      </div>
      <motion.div
        initial={{ opacity: 0, scale: 0.9 }}
        animate={{ opacity: 1, scale: 1 }}
        transition={{ delay: 0.4 }}
        style={{ display: 'grid', placeItems: 'center' }}>
        <PhoneFrame width={360}>
          <BizPhone />
        </PhoneFrame>
      </motion.div>
    </div>
  );
}

function BizPhone() {
  return (
    <div style={{ width: '100%', height: '100%', padding: '70px 18px 18px',
                  display: 'flex', flexDirection: 'column', gap: 12 }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <div>
          <div style={{ fontSize: 13, color: theme.textDim }}>Mi evento</div>
          <div style={{ fontSize: 18, fontWeight: 700 }}>Noche flamenca · Triana</div>
        </div>
        <div style={{
          display: 'inline-flex', alignItems: 'center', gap: 4,
          padding: '4px 10px', borderRadius: 999, fontSize: 11, fontWeight: 700,
          background: `${theme.gold}33`, color: theme.gold, border: `1px solid ${theme.gold}55`,
        }}>
          <Icon name="shield" size={11} /> VERIFICADO
        </div>
      </div>
      <div style={{
        height: 130, borderRadius: 14, marginTop: 6,
        background: `linear-gradient(135deg, ${theme.purpleDeep}, ${theme.redDeep})`,
        display: 'grid', placeItems: 'center', position: 'relative', overflow: 'hidden',
        color: theme.gold,
      }}>
        <motion.div
          animate={{ scale: [1, 1.4], opacity: [0.6, 0] }}
          transition={{ duration: 1.6, repeat: Infinity }}
          style={{ position: 'absolute', width: 80, height: 80, borderRadius: '50%',
                   border: `3px solid ${theme.gold}` }}
        />
        <Icon name="bolt" size={42} />
        <div style={{ position: 'absolute', bottom: 8, left: 12, fontSize: 12,
                      color: '#fff', fontWeight: 600 }}>BOOST ACTIVO</div>
      </div>
      <div style={{ marginTop: 4, display: 'grid',
                    gridTemplateColumns: '1fr 1fr', gap: 10 }}>
        {[
          { l: 'Asistentes', v: '284', a: theme.green, icon: 'users' },
          { l: 'Vistas', v: '6.4k', a: theme.purple, icon: 'globe' },
          { l: 'Preguntas', v: '92', a: theme.red, icon: 'chat' },
          { l: 'CTR', v: '8.1%', a: theme.gold, icon: 'trending' },
        ].map((it, i) => (
          <div key={i} style={{
            padding: 10, borderRadius: 12,
            background: 'rgba(255,255,255,0.06)', border: `1px solid ${theme.border}`,
          }}>
            <div style={{ fontSize: 11, color: theme.textDim,
                          display: 'flex', alignItems: 'center', gap: 4 }}>
              <Icon name={it.icon} size={11} /> {it.l}
            </div>
            <div style={{ fontFamily: 'Space Grotesk', fontSize: 20,
                          fontWeight: 700, color: it.a }}>{it.v}</div>
          </div>
        ))}
      </div>
      <div style={{
        marginTop: 'auto', padding: 12, borderRadius: 12,
        background: `linear-gradient(135deg, ${theme.gold}, #d97706)`,
        textAlign: 'center', fontWeight: 700, fontSize: 14, color: '#1a1100',
        display: 'flex', justifyContent: 'center', alignItems: 'center', gap: 6,
      }}>
        <Icon name="bolt" size={14} /> Promocionar (14,99 €)
      </div>
    </div>
  );
}

function Stat({ label, value, suffix = '', decimals = 0, start, accent, icon }) {
  return (
    <div style={{ ...card(), padding: 22 }}>
      <div style={{ fontSize: 14, color: theme.textDim,
                    letterSpacing: 1, display: 'flex', alignItems: 'center', gap: 8 }}>
        <Icon name={icon} size={16} /> {label}
      </div>
      <div style={{ fontFamily: 'Space Grotesk', fontSize: 56,
                    fontWeight: 800, color: accent, marginTop: 4 }}>
        <Counter to={value} suffix={suffix} decimals={decimals} duration={1.6} start={start} />
      </div>
    </div>
  );
}

/* ============================================================
   Scene 7 — OUTRO
   ============================================================ */
function Outro() {
  return (
    <div style={center()}>
      <motion.div
        initial={{ opacity: 0, scale: 0.7 }} animate={{ opacity: 1, scale: 1 }}
        transition={{ duration: 0.8 }}>
        <Logo size={140} />
      </motion.div>
      <motion.h1
        initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 0.5, duration: 0.7 }}
        style={{
          fontFamily: 'Space Grotesk', fontSize: 110, fontWeight: 800,
          margin: '40px 0 8px', textAlign: 'center', letterSpacing: -2,
        }}>
        Pregunta. <span style={{
          background: `linear-gradient(135deg, ${theme.red}, ${theme.purple})`,
          WebkitBackgroundClip: 'text', WebkitTextFillColor: 'transparent',
        }}>Responde. Conecta.</span>
      </motion.h1>
      <motion.div
        initial={{ opacity: 0 }} animate={{ opacity: 1 }} transition={{ delay: 1.4 }}
        style={{ marginTop: 24, fontSize: 28, color: theme.textDim,
                 display: 'flex', alignItems: 'center', gap: 10 }}>
        <Icon name="globe" size={24} /> streetask.app · Descárgalo gratis
      </motion.div>
      <motion.div
        initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 1.8 }}
        style={{ marginTop: 36, display: 'flex', gap: 16 }}>
        {['App Store', 'Google Play'].map((s, i) => (
          <div key={i} style={{
            padding: '16px 32px', borderRadius: 999,
            background: 'rgba(255,255,255,0.08)', border: `1px solid ${theme.border}`,
            fontSize: 22, fontWeight: 700,
          }}>
            {s}
          </div>
        ))}
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
        style={{ fontSize: 22, color: theme.red, fontWeight: 700,
                 letterSpacing: 4, textTransform: 'uppercase' }}>
        {eyebrow}
      </motion.div>
      <motion.h2
        initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: 0.15 }}
        style={{
          fontFamily: 'Space Grotesk', fontSize: 88, fontWeight: 800,
          lineHeight: 1.05, margin: '12px 0 0', letterSpacing: -2,
        }}>
        {title}
      </motion.h2>
    </div>
  );
}

function center() {
  return {
    position: 'absolute', inset: 0,
    display: 'flex', flexDirection: 'column',
    alignItems: 'center', justifyContent: 'center', padding: '0 80px',
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
