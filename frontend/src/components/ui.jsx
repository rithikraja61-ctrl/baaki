import { useEffect, useState } from 'react';
import { LANGS } from '../lib/langs.js';
import QRCode from 'qrcode';
import { translate, useApp } from '../lib/AppContext.jsx';

const STATUS_STYLE = {
  DRAFT: 'bg-gray-100 text-gray-800 border-gray-400',
  SENT: 'bg-blue-50 text-blue-900 border-blue-400',
  DUE_SOON: 'bg-amber-50 text-amber-900 border-amber-500',
  OVERDUE: 'bg-red-50 text-red-900 border-red-500',
  PART_PAID: 'bg-purple-50 text-purple-900 border-purple-400',
  PAID: 'bg-green-50 text-green-900 border-green-500',
  DISPUTED: 'bg-orange-50 text-orange-900 border-orange-500',
};
const STATUS_ICON = { DRAFT: '✎', SENT: '➤', DUE_SOON: '⏳', OVERDUE: '!', PART_PAID: '◐', PAID: '✓', DISPUTED: '⚑' };

/** Status always shows a word, never colour alone. */
export function StatusBadge({ status }) {
  const { t } = useApp();
  return (
    <span className={`inline-flex items-center gap-1 px-2 py-0.5 rounded-full border text-sm font-semibold whitespace-nowrap ${STATUS_STYLE[status] || ''}`}>
      <span aria-hidden="true">{STATUS_ICON[status]}</span>{t(`status.${status}`)}
    </span>
  );
}

export function Grade({ grade }) {
  const { t } = useApp();
  const c = grade === 'A' ? 'bg-green-700' : grade === 'B' ? 'bg-amber-600' : 'bg-red-700';
  return <span className={`inline-flex items-center justify-center w-8 h-8 rounded-full text-white font-bold ${c}`} aria-label={t('common.grade', { g: grade })}>{grade}</span>;
}

/** Plain word with the accounting term in a tooltip (title + screen-reader text). */
export function Term({ children, tip }) {
  return (
    <span className="underline decoration-dotted underline-offset-4 cursor-help" title={tip} tabIndex={0}>
      {children}<span className="sr-only"> ({tip})</span>
    </span>
  );
}

export function Loading() { const { t } = useApp(); return <p className="muted p-4" role="status">{t('common.loading')}</p>; }
export function ErrorBox({ error }) {
  const { t } = useApp();
  if (!error) return null;
  return <div role="alert" className="border-l-4 border-red-700 bg-red-50 text-red-900 p-3 rounded my-3">{t('common.error')}: {error.message || String(error)}</div>;
}
export function Notice({ children, tone = 'info' }) {
  const s = tone === 'warn' ? 'border-amber-600 bg-amber-50 text-amber-950' : tone === 'ok' ? 'border-green-700 bg-green-50 text-green-950' : 'border-blue-700 bg-blue-50 text-blue-950';
  return <div role="status" className={`border-l-4 p-3 rounded my-3 ${s}`}>{children}</div>;
}

export function Field({ label, id, help, error, children }) {
  return (
    <div className="mb-3">
      <label htmlFor={id} className="label">{label}</label>
      {children}
      {help && <p id={`${id}-help`} className="text-sm muted mt-1">{help}</p>}
      {error && <p className="text-sm text-red-800 mt-1" role="alert">{error}</p>}
    </div>
  );
}

export function QR({ text, size = 140, label }) {
  const [url, setUrl] = useState('');
  useEffect(() => { QRCode.toDataURL(text, { width: size, margin: 1 }).then(setUrl).catch(() => setUrl('')); }, [text, size]);
  return url ? <img src={url} width={size} height={size} alt={label || 'UPI QR code'} /> : null;
}

export function PageTitle({ children, actions }) {
  return (
    <div className="flex flex-wrap items-center justify-between gap-3 mb-4">
      <h1 className="text-2xl font-bold">{children}</h1>
      {actions && <div className="flex flex-wrap gap-2">{actions}</div>}
    </div>
  );
}

/** Language picker: every language name in its own script. `lang` attribute on each option helps screen readers. */
export function LangPicker({ value, onChange, id, label, className = '' }) {
  return (
    <select id={id} aria-label={id ? undefined : label} className={`input ${className}`} value={value} onChange={(e) => onChange(e.target.value)}>
      {LANGS.map((l) => <option key={l.code} value={l.code} lang={l.code}>{l.name}</option>)}
    </select>
  );
}

/**
 * Bilingual heading for statutory documents: English always, plus the owner's chosen second language
 * in small text underneath. `second` is null for English-only.
 */
export function Bi({ k, vars, second, className = '' }) {
  const en = translate('en', k, vars);
  if (!second) return <span className={className}>{en}</span>;
  return (
    <span className={className}>{en}<span lang={second} className="block text-[0.78em] font-normal leading-snug break-words">{translate(second, k, vars)}</span></span>
  );
}
