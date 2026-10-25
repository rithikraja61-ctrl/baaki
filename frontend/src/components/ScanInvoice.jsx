import { useEffect, useRef, useState } from 'react';
import { api, API_BASE } from '../api/client.js';
import { useApp } from '../lib/AppContext.jsx';
import { money } from '../lib/format.js';

const MAX = 5 * 1024 * 1024;
const TYPES = ['image/jpeg', 'image/png', 'application/pdf'];

/** Upload card: photo or PDF of an existing invoice -> /api/invoices/scan. The file is never stored. */
export function ScanUpload({ onResult }) {
  const { t } = useApp();
  const [status, setStatus] = useState(null);
  const [busy, setBusy] = useState(false);
  const [err, setErr] = useState('');
  const cam = useRef(null);
  const pick = useRef(null);
  useEffect(() => {
    api.get('/invoices/scan/status')
      .then(setStatus)
      .catch(() => setStatus(null));
  }, []);

  const send = async (file) => {
    setErr('');
    if (!file) return;
    if (!TYPES.includes(file.type) && !/\.(jpe?g|png|pdf)$/i.test(file.name)) { setErr(t('scan.err.UNSUPPORTED_TYPE')); return; }
    if (file.size > MAX) { setErr(t('scan.err.FILE_TOO_BIG')); return; }
    setBusy(true);
    try {
      const fd = new FormData();
      fd.append('file', file);
      const headers = {};
      const token = localStorage.getItem('baaki.auth.token');
      if (token) headers['Authorization'] = `Bearer ${token}`;
      const res = await fetch(`${API_BASE}/invoices/scan`, { method: 'POST', headers, body: fd });
      const text = await res.text();
      const data = text ? JSON.parse(text) : null;
      if (!res.ok) { setErr(t(`scan.err.${data?.code || 'AI_FAILED'}`)); return; }
      onResult(data);
    } catch { setErr(t('scan.err.AI_FAILED')); } finally {
      setBusy(false);
      if (cam.current) cam.current.value = '';
      if (pick.current) pick.current.value = '';
    }
  };

  return (
    <section className="card mb-4 no-print">
      <h2 className="h2">{t('scan.title')}</h2>
      <p className="muted mb-3">{t('scan.intro')}</p>
      {status && !status.configured && <p className="border-l-4 border-amber-600 bg-amber-50 text-amber-950 p-3 rounded mb-3">{t('scan.err.AI_NOT_CONFIGURED')}</p>}
      <div className="flex flex-wrap gap-2">
        <label className={`btn-primary cursor-pointer ${busy ? 'opacity-60 pointer-events-none' : ''}`}>
          📷 {t('scan.takePhoto')}
          <input ref={cam} type="file" accept="image/jpeg,image/png" capture="environment" className="sr-only" onChange={(e) => send(e.target.files[0])} disabled={busy} />
        </label>
        <label className={`btn-secondary cursor-pointer ${busy ? 'opacity-60 pointer-events-none' : ''}`}>
          {t('scan.chooseFile')}
          <input ref={pick} type="file" accept="image/jpeg,image/png,application/pdf,.jpg,.jpeg,.png,.pdf" className="sr-only" onChange={(e) => send(e.target.files[0])} disabled={busy} />
        </label>
      </div>
      <p className="text-sm muted mt-2">{t('scan.limits')}</p>
      <div aria-live="polite">
        {busy && <p className="font-semibold mt-2">{t('scan.reading')}</p>}
        {err && <p role="alert" className="border-l-4 border-red-700 bg-red-50 text-red-900 p-3 rounded mt-2">{err}</p>}
      </div>
    </section>
  );
}

/** Review panel: every check result, plus printed totals next to Baaki's own calculation. */
export function ScanReview({ scan, calc, pending, onDiscard }) {
  const { t } = useApp();
  const msg = (f) => {
    const p = { ...f.params };
    for (const k of ['printed', 'computed']) if (typeof p[k] === 'number' && !f.field.includes('gstRate')) p[k] = money(p[k]);
    if (f.field.includes('gstRate')) { p.printed = `${(f.params.printed || 0) / 100}%`; p.used = `${(f.params.used || 0) / 100}%`; }
    return `${t(`scan.field.${f.field.replace(/^items\.(\d+)\..*$/, 'item').replace('.', '_')}`, { n: Number((f.field.match(/^items\.(\d+)/) || [])[1]) + 1 })}: ${t(`scan.flag.${f.code}`, p)}`;
  };
  const p = scan.printed || {};
  const c = calc || {};
  const row = (label, a, b) => (a == null && !b ? null : (
    <tr key={label} className={a != null && b != null && Math.abs(a - b) > 100 ? 'bg-red-50 font-semibold' : ''}>
      <td className="py-1 pr-2">{label}</td><td className="text-right pr-2">{a == null ? '—' : money(a)}</td><td className="text-right">{b == null ? '—' : money(b)}</td>
    </tr>));
  return (
    <section className="card mb-4 border-2 border-amber-600" aria-live="polite">
      <h2 className="h2">{t('scan.reviewTitle')}</h2>
      <p className="mb-2">{t('scan.reviewIntro')}</p>
      <p className="mb-3"><span className="inline-block px-2 rounded bg-amber-100 ring-2 ring-amber-600 mr-2">{t('scan.legend')}</span>{t('scan.pending', { n: pending })}</p>
      {scan.flags.length > 0 ? (
        <ul className="list-disc pl-5 space-y-1 mb-3">{scan.flags.map((f, i) => <li key={i} className="break-words">{msg(f)}</li>)}</ul>
      ) : <p className="mb-3 text-green-900">{t('scan.allChecksOk')}</p>}
      <div className="overflow-x-auto">
        <table className="text-sm w-full max-w-md">
          <thead><tr><th className="text-left">{' '}</th><th className="text-right pr-2">{t('scan.printed')}</th><th className="text-right">{t('scan.recalculated')}</th></tr></thead>
          <tbody>
            {row(t('newInv.taxable'), p.taxablePaise, c.taxablePaise)}
            {row(t('newInv.cgst'), p.cgstPaise, c.cgstPaise)}
            {row(t('newInv.sgst'), p.sgstPaise, c.sgstPaise)}
            {row(t('newInv.igst'), p.igstPaise, c.igstPaise)}
            {row(t('newInv.roundOff'), p.roundOffPaise, c.roundOffPaise)}
            {row(t('newInv.grandTotal'), p.totalPaise, c.totalPaise)}
          </tbody>
        </table>
      </div>
      <p className="text-sm muted mt-2">{t('scan.notKept')}</p>
      <button type="button" className="btn-secondary mt-3" onClick={onDiscard}>{t('scan.discard')}</button>
    </section>
  );
}
