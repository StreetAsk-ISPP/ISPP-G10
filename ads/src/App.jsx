import React, { useEffect, useState } from 'react';
import Stage from './components/Stage.jsx';
import InvestorsAd from './scenes/InvestorsAd.jsx';
import ClientsAd from './scenes/ClientsAd.jsx';
import { theme } from './theme.js';

function useHashRoute() {
  const [hash, setHash] = useState(() => window.location.hash.replace('#/', '') || '');
  useEffect(() => {
    const onChange = () => setHash(window.location.hash.replace('#/', '') || '');
    window.addEventListener('hashchange', onChange);
    return () => window.removeEventListener('hashchange', onChange);
  }, []);
  return hash;
}

export default function App() {
  const route = useHashRoute();

  if (route === 'investors') {
    return <Stage><InvestorsAd /></Stage>;
  }
  if (route === 'clients') {
    return <Stage><ClientsAd /></Stage>;
  }
  return <Index />;
}

function Index() {
  const cards = [
    {
      to: '#/investors',
      title: 'Anuncio Inversores',
      desc: 'Por qué invertir en StreetAsk · ~60 s · 1920×1080',
      tag: 'Pitch',
      color: theme.purple,
    },
    {
      to: '#/clients',
      title: 'Anuncio Clientes',
      desc: 'Usuario + Business · ~60 s · 1920×1080',
      tag: 'Marketing',
      color: theme.red,
    },
  ];

  return (
    <div style={{
      minHeight: '100vh', background: `radial-gradient(900px 600px at 30% 0%, ${theme.purpleDeep}, ${theme.bg0})`,
      color: '#fff', padding: '80px 8vw',
    }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: 16 }}>
        <div style={{
          width: 56, height: 56, borderRadius: 16,
          background: `linear-gradient(135deg, ${theme.red}, ${theme.purple})`,
          display: 'grid', placeItems: 'center', fontSize: 30,
        }}>💬</div>
        <div style={{ fontSize: 28, fontWeight: 800, fontFamily: 'Space Grotesk' }}>
          Street<span style={{ color: theme.red }}>Ask</span> · Ads
        </div>
      </div>
      <h1 style={{
        marginTop: 60, fontFamily: 'Space Grotesk',
        fontSize: 80, fontWeight: 800, letterSpacing: -2, lineHeight: 1.05,
      }}>
        Dos anuncios.<br/>Listos para grabar.
      </h1>
      <p style={{ fontSize: 22, color: 'rgba(255,255,255,0.7)', maxWidth: 720, marginTop: 16 }}>
        Cada anuncio dura ~60&nbsp;s y se renderiza a 1920×1080. Para grabarlo, ejecuta el comando
        de Playwright o pulsa F11 y graba con OBS.
      </p>
      <div style={{ marginTop: 60, display: 'grid', gridTemplateColumns: 'repeat(2, 1fr)', gap: 28, maxWidth: 1200 }}>
        {cards.map((c, i) => (
          <a key={i} href={c.to} style={{
            display: 'block', textDecoration: 'none', color: 'inherit',
            padding: 36, borderRadius: 24,
            background: 'rgba(255,255,255,0.05)',
            border: '1px solid rgba(255,255,255,0.1)',
            transition: 'transform 0.2s, background 0.2s',
          }}
          onMouseEnter={e => { e.currentTarget.style.transform = 'translateY(-4px)'; e.currentTarget.style.background = 'rgba(255,255,255,0.08)'; }}
          onMouseLeave={e => { e.currentTarget.style.transform = 'translateY(0)'; e.currentTarget.style.background = 'rgba(255,255,255,0.05)'; }}
          >
            <div style={{
              display: 'inline-block', padding: '6px 14px', borderRadius: 999,
              background: `${c.color}33`, color: '#fff',
              border: `1px solid ${c.color}66`, fontSize: 13, fontWeight: 700, letterSpacing: 2,
            }}>{c.tag.toUpperCase()}</div>
            <div style={{ fontSize: 36, fontWeight: 700, marginTop: 16, fontFamily: 'Space Grotesk' }}>{c.title}</div>
            <div style={{ fontSize: 18, color: 'rgba(255,255,255,0.7)', marginTop: 8 }}>{c.desc}</div>
            <div style={{ marginTop: 24, color: c.color, fontWeight: 700 }}>Reproducir →</div>
          </a>
        ))}
      </div>
      <div style={{ marginTop: 60, fontSize: 14, color: 'rgba(255,255,255,0.5)' }}>
        Comandos: <code>npm run dev</code> · <code>npm run record:investors</code> · <code>npm run record:clients</code>
      </div>
    </div>
  );
}
