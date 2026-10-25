import { useState } from 'react';
import { NavLink, Outlet, useLocation } from 'react-router-dom';
import { useApp } from '../lib/AppContext.jsx';
import { useAuth } from '../context/AuthContext.jsx';
import { fmtDate } from '../lib/format.js';
import { LangPicker } from './ui.jsx';

const LINKS = [
  ['/', 'dashboard', '▦'], ['/followups', 'followups', '☎'], ['/chase', 'chase', '★'], ['/invoices', 'invoices', '▤'],
  ['/invoices/new', 'newInvoice', '+'], ['/customers', 'customers', '☺'], ['/payments', 'payments', '₹'],
  ['/forecast', 'forecast', '↗'], ['/inbox', 'inbox', '✉'], ['/documents', 'documents', '🗎'], ['/settings', 'settings', '⚙'],
];

export default function Layout() {
  const { t, lang, setLang, today, profile } = useApp();
  const { user, logout } = useAuth();
  const [open, setOpen] = useState(false);
  const loc = useLocation();

  const userInitial = user?.name ? user.name.charAt(0).toUpperCase() : 'U';

  const userPill = user && (
    <div className="p-3 bg-gray-50 border border-gray-200 rounded-xl mb-3 flex items-center justify-between gap-2">
      <div className="flex items-center gap-2.5 min-w-0">
        {user.picture ? (
          <img src={user.picture} alt="" className="w-8 h-8 rounded-full shrink-0 border border-gray-200" />
        ) : (
          <div className="w-8 h-8 rounded-full bg-brand-600 text-white flex items-center justify-center font-bold text-sm shrink-0">
            {userInitial}
          </div>
        )}
        <div className="min-w-0">
          <div className="text-xs font-semibold text-gray-900 truncate">{user.name}</div>
          <div className="text-[11px] text-gray-500 truncate">{user.email}</div>
        </div>
      </div>
      <button
        onClick={logout}
        title="Sign out"
        className="text-xs text-red-600 hover:text-red-800 font-medium px-2 py-1 hover:bg-red-50 rounded transition-colors shrink-0"
      >
        Sign out
      </button>
    </div>
  );

  const nav = (
    <nav aria-label="Main" className="flex-1">
      <ul className="space-y-1">
        {LINKS.map(([to, key, icon]) => (
          <li key={to}>
            <NavLink to={to} end={to === '/' || to === '/invoices'} onClick={() => setOpen(false)}
              className={({ isActive }) => `flex items-center gap-3 min-h-[44px] px-3 rounded-lg font-semibold ${isActive ? 'bg-brand-600 text-white' : 'text-gray-900 hover:bg-brand-50'}`}>
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
      
      {/* Mobile Header */}
      <header className="no-print lg:hidden sticky top-0 z-20 bg-white border-b flex items-center justify-between gap-2 px-2 sm:px-3 h-16">
        <button className="btn-secondary px-3 shrink-0" aria-expanded={open} aria-controls="mobile-nav" onClick={() => setOpen(!open)}>
          <span aria-hidden="true">☰</span><span>{open ? t('nav.close') : t('nav.menu')}</span>
        </button>
        <span className="font-bold text-xl text-brand-800 truncate min-w-0">{t('appName')}</span>
        <LangPicker value={lang} onChange={setLang} label={t('common.switchLang')} className="w-auto max-w-[8.5rem] shrink-0" />
      </header>

      {/* Mobile Drawer */}
      {open && (
        <div id="mobile-nav" className="no-print lg:hidden bg-white border-b p-3 space-y-3">
          {userPill}
          {nav}
        </div>
      )}

      {/* Desktop Sidebar */}
      <aside className="no-print hidden lg:flex flex-col w-64 shrink-0 bg-white border-r p-4 h-screen sticky top-0 overflow-y-auto">
        <div className="mb-4">
          <div className="font-bold text-2xl text-brand-800">{t('appName')}</div>
          <div className="text-sm muted">{t('tagline')}</div>
          {profile && <div className="text-xs font-medium text-gray-600 mt-1">{profile.name}</div>}
        </div>
        
        {nav}

        <div className="mt-4 pt-4 border-t border-gray-100">
          {userPill}
          <LangPicker value={lang} onChange={setLang} label={t('common.switchLang')} />
        </div>
      </aside>

      {/* Main Content Area */}
      <main id="main" className="flex-1 min-w-0 p-3 sm:p-6" key={loc.pathname}>
        {today && <p className="no-print text-sm muted mb-2">{t('common.today')}: <strong>{fmtDate(today, lang)}</strong>{profile?.demoDate ? ` (${t('common.demoDate')})` : ''}</p>}
        <Outlet />
      </main>
    </div>
  );
}
