import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../api/client.js';
import { useApp, useLoad } from '../lib/AppContext.jsx';
import { fmtDate, money, money0 } from '../lib/format.js';
import { ErrorBox, Grade, Loading, PageTitle } from '../components/ui.jsx';

export function CustomerForm({ initial, onSaved, onCancel }) {
  const { t, config } = useApp();
  const [f, setF] = useState(initial || { name: '', contactPerson: '', phone: '', email: '', address: '', city: '', stateCode: '27', gstin: '', creditDays: 30 });
  const [gst, setGst] = useState(null);
  const [err, setErr] = useState(null);
  const set = (k) => (e) => setF({ ...f, [k]: e.target.value });
  useEffect(() => {
    const g = (f.gstin || '').trim();
    if (!g) { setGst(null); return; }
    if (g.length < 15) { setGst({ valid: false }); return; }
    api.get(`/gstin/validate?gstin=${encodeURIComponent(g)}`).then((r) => { setGst(r); if (r.valid) setF((x) => ({ ...x, stateCode: r.stateCode })); }).catch(() => setGst(null));
  }, [f.gstin]);
  const submit = async (e) => {
    e.preventDefault(); setErr(null);
    try { const r = initial?.id ? await api.put(`/customers/${initial.id}`, f) : await api.post('/customers', f); onSaved(r); } catch (x) { setErr(x); }
  };
  const fld = (k, label, props = {}) => (
    <div><label className="label" htmlFor={`cf-${k}`}>{label}</label><input id={`cf-${k}`} className="input" value={f[k] ?? ''} onChange={set(k)} {...props} /></div>
  );
  return (
    <form onSubmit={submit} className="space-y-3">
      <ErrorBox error={err} />
      <div className="grid sm:grid-cols-2 gap-3">
        {fld('name', t('cust.name'), { required: true })}
        <div>
          <label className="label" htmlFor="cf-gstin">{t('cust.gstin')}</label>
          <input id="cf-gstin" className="input uppercase" maxLength={15} value={f.gstin || ''} onChange={(e) => setF({ ...f, gstin: e.target.value.toUpperCase() })} aria-describedby="cf-gstin-help" aria-invalid={gst && !gst.valid} />
          <p id="cf-gstin-help" className={`text-sm mt-1 ${gst ? (gst.valid ? 'text-green-800' : 'text-red-800') : 'muted'}`} aria-live="polite">
            {gst ? (gst.valid ? '✓ ' + t('cust.gstinValid', { state: `${gst.stateName} (${gst.stateCode})` }) : '✕ ' + t('cust.gstinInvalid')) : t('cust.gstinHelp')}
          </p>
        </div>
        {fld('contactPerson', t('cust.contact'))}
        {fld('phone', t('cust.phone'), { type: 'tel' })}
        {fld('email', t('cust.email'), { type: 'email' })}
        {fld('address', t('cust.address'))}
        {fld('city', t('cust.city'))}
        <div>
          <label className="label" htmlFor="cf-state">{t('cust.state')}</label>
          <select id="cf-state" className="input" value={f.stateCode} onChange={set('stateCode')} disabled={gst?.valid}>
            {Object.entries(config?.states || {}).map(([c, n]) => <option key={c} value={c}>{n} ({c})</option>)}
          </select>
        </div>
        {fld('creditDays', t('cust.creditDays'), { type: 'number', min: 0, max: 365 })}
      </div>
      <div className="flex gap-2"><button className="btn-primary" disabled={gst && !gst.valid}>{t('common.save')}</button>{onCancel && <button type="button" className="btn-secondary" onClick={onCancel}>{t('common.cancel')}</button>}</div>
    </form>
  );
}

export function reliabilityText(t, r) {
  return t(`cust.reason.${r.reasonCode}`, { p: r.onTimePercent, d: r.avgDaysLate });
}

function PaymentRecord({ id }) {
  const { t, lang } = useApp();
  const { data, error } = useLoad(`/customers/${id}/payments`);
  if (error) return <ErrorBox error={error} />;
  if (!data) return <Loading />;
  if (data.length === 0) return <p className="muted mt-2">{t('cust.histEmpty')}</p>;
  const timing = (r) => (r.unpaid ? t('cust.histUnpaid', { n: r.daysLate }) : r.daysLate > 0 ? t('cust.late', { n: r.daysLate }) : r.daysLate < 0 ? t('cust.early', { n: -r.daysLate }) : t('cust.onTimeExact'));
  return (
    <ul className="divide-y mt-2 text-sm">
      {data.map((r) => (
        <li key={r.reference + r.dueDate} className="py-2 flex flex-wrap justify-between gap-x-3">
          <span><span className="font-semibold">{r.reference}</span> · {money(r.amountPaise)}</span>
          <span className="muted">{t('cust.histDue')} {fmtDate(r.dueDate, lang)}{r.paidDate ? ` · ${t('cust.histPaid')} ${fmtDate(r.paidDate, lang)}` : ''}</span>
          <span className={`font-semibold ${r.unpaid || r.daysLate > 15 ? 'text-red-800' : r.daysLate > 0 ? 'text-amber-800' : 'text-green-800'}`}>{timing(r)}</span>
        </li>
      ))}
    </ul>
  );
}

export default function Customers() {
  const { t } = useApp();
  const { data, error, loading, reload } = useLoad('/customers');
  const [editing, setEditing] = useState(null);
  const [hist, setHist] = useState(null);
  if (loading && !data) return <Loading />;
  if (error) return <ErrorBox error={error} />;
  return (
    <div>
      <PageTitle actions={<button className="btn-primary" onClick={() => setEditing({})}>+ {t('cust.add')}</button>}>{t('cust.title')}</PageTitle>
      {editing && <section className="card mb-4"><CustomerForm initial={editing.id ? { ...editing, gstin: editing.gstin || '' } : null} onSaved={() => { setEditing(null); reload(); }} onCancel={() => setEditing(null)} /></section>}
      <ul className="grid md:grid-cols-2 gap-3">
        {data.map((c) => (
          <li key={c.id} className="card">
            <div className="flex items-start gap-3">
              <Grade grade={c.reliability.grade} />
              <div className="flex-1 min-w-0">
                <h2 className="font-bold text-lg">{c.name}</h2>
                <div className="text-sm muted">{c.contactPerson} · <a className="underline" href={`tel:${c.phone}`}>{c.phone}</a> · <a className="underline break-all" href={`mailto:${c.email}`}>{c.email}</a></div>
                <div className="text-sm">{c.gstin ? `GSTIN ${c.gstin}` : t('cust.unregistered')} · {c.stateName}{c.stateCode !== '27' ? ` (${t('cust.otherState')})` : ''}</div>
              </div>
            </div>
            <dl className="grid grid-cols-2 sm:grid-cols-4 gap-2 mt-3 text-sm">
              <div><dt className="muted">{t('cust.owed')}</dt><dd className="font-bold">{money0(c.owedPaise)}</dd></div>
              <div><dt className="muted">{t('cust.overdue')}</dt><dd className="font-bold">{money0(c.overduePaise)}</dd></div>
              <div><dt className="muted">{t('cust.avgLate')}</dt><dd className="font-bold">{c.reliability.avgDaysLate}</dd></div>
              <div><dt className="muted">{t('cust.onTime')}</dt><dd className="font-bold">{c.reliability.onTimePercent}%</dd></div>
            </dl>
            <p className="mt-2"><span className="font-semibold">{t('cust.grade')} {c.reliability.grade}:</span> {reliabilityText(t, c.reliability)}</p>
            <p className="text-sm muted">{t('cust.creditDays')}: {c.creditDays}</p>
            <div className="flex flex-wrap gap-2 mt-3">
              <Link className="btn-secondary" to={`/invoices/new?customerId=${c.id}`}>{t('cust.newInvoice')}</Link>
              <button className="btn-secondary" onClick={() => { setEditing(c); window.scrollTo(0, 0); }}>{t('inv.edit')}</button>
              <button className="btn-secondary" aria-expanded={hist === c.id} onClick={() => setHist(hist === c.id ? null : c.id)}>{hist === c.id ? t('cust.hideHistory') : t('cust.history')}</button>
            </div>
            {hist === c.id && <PaymentRecord id={c.id} />}
          </li>
        ))}
      </ul>
    </div>
  );
}
