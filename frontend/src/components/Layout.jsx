import { useState } from 'react';
import { NavLink, Outlet, useLocation } from 'react-router-dom';
import { useApp } from '../lib/AppContext.jsx';
import { fmtDate } from '../lib/format.js';
import { LangPicker } from './ui.jsx';

const LINKS = [
  ['/', 'dashboard', '▦'], ['/followups', 'followups', '☎'], ['/chase', 'chase', '★'], ['/invoices', 'invoices', '▤'],
  ['/invoices/new', 'newInvoice', '+'], ['/customers', 'customers', '☺'], ['/payments', 'payments', '₹'],
  ['/forecast', 'forecast', '↗'], ['/inbox', 'inbox', '✉'], ['/documents', 'documents', '🗎'], ['/settings', 'settings', '⚙'],
];

export default function Layout() {
  const { t, lang, setLang, today, profile } = useApp();
  const [open, setOpen] = useState(false);
  const loc = useLocation();
  const nav = (
    <nav aria-label="Main">
      <ul className="space-y-1">
        {LINKS.map(([to, key, icon]) => (
          <li key={to}>
            <NavLink to={to} end={to === '/' || to === '/invoices'} onClick={() => setOpen(false)}
              className={({ isActive }) => `flex items-center gap-3 min-h-[48px] px-3 rounded-lg font-semibold ${isActive ? 'bg-brand-600 text-white' : 'text-gray-900 hover:bg-brand-50'}`}>
              <span aria-hidden="true" className="w-5 text-center">{icon}</span>{t(`nav.${key}`)}
            </NavLink>
          </li>
        ))}
      </ul>
    </nav>
  );
  return (
    <div className="min-h-screen lg:flex">
      <a href="#main" className="sr-only focus:not-sr-only focus:absolute focus:p-3 focus:bg-white">{t('common.skip')}</a>
      <header className="no-print lg:hidden sticky top-0 z-20 bg-white border-b flex items-center justify-between gap-2 px-2 sm:px-3 h-16">
        <button className="btn-secondary px-3 shrink-0" aria-expanded={open} aria-controls="mobile-nav" onClick={() => setOpen(!open)}>
          <span aria-hidden="true">☰</span><span>{open ? t('nav.close') : t('nav.menu')}</span>
        </button>
        <span className="font-bold text-xl text-brand-800 truncate min-w-0">{t('appName')}</span>
        <LangPicker value={lang} onChange={setLang} label={t('common.switchLang')} className="w-auto max-w-[8.5rem] shrink-0" />
      </header>
      {open && <div id="mobile-nav" className="no-print lg:hidden bg-white border-b p-3">{nav}</div>}
      <aside className="no-print hidden lg:block w-64 shrink-0 bg-white border-r p-4 min-h-screen sticky top-0">
        <div className="mb-4">
          <div className="font-bold text-2xl text-brand-800">{t('appName')}</div>
          <div className="text-sm muted">{t('tagline')}</div>
          {profile && <div className="text-sm mt-1">{profile.name}</div>}
        </div>
        {nav}
        <div className="mt-4"><LangPicker value={lang} onChange={setLang} label={t('common.switchLang')} /></div>
      </aside>
      <main id="main" className="flex-1 min-w-0 p-3 sm:p-6" key={loc.pathname}>
        {today && <p className="no-print text-sm muted mb-2">{t('common.today')}: <strong>{fmtDate(today, lang)}</strong>{profile?.demoDate ? ` (${t('common.demoDate')})` : ''}</p>}
        <Outlet />
      </main>
    </div>
  );
}
