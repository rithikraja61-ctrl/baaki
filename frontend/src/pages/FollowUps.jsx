import { useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../api/client.js';
import { useApp, useLoad } from '../lib/AppContext.jsx';
import { addDays, fmtDate, money } from '../lib/format.js';
import { fillTemplate } from '../data/templates.js';
import { ErrorBox, Loading, Notice, PageTitle } from '../components/ui.jsx';

export function NextSteps() {
  const { t, profile } = useApp();
  const msme = profile?.udyamRegistered && ['MICRO', 'SMALL'].includes(profile?.msmeCategory);
  return (
    <div className="border-l-4 border-amber-600 bg-amber-50 p-3 rounded">
      <p className="font-semibold">{t('next.title')}</p>
      <ul className="list-disc ml-6">
        <li>{t('next.CALL_CUSTOMER')}</li><li>{t('next.CONSULT_LAWYER')}</li>{msme && <li>{t('next.MSME_SAMADHAAN')}</li>}
      </ul>
      <p className="text-sm mt-1">{t('next.noAuto')}</p>
    </div>
  );
}

function FollowUpCard({ f, onDone }) {
  const { t, lang, profile, today } = useApp();
  const [msgLang, setMsgLang] = useState(lang);
  const [err, setErr] = useState(null);
  const [date, setDate] = useState(addDays(today, 3));
  const [done, setDone] = useState(false);
  const origin = window.location.origin;
  const message = fillTemplate(msgLang, f.stage, {
    contact: f.contactPerson || f.customerName, customer: f.customerName, business: profile?.name,
    invoices: f.invoices.map((i) => i.invoiceNumber).join(', '), amount: money(f.totalPaise),
    days: Math.max(0, f.maxDaysOverdue), dueDate: fmtDate(f.invoices[0].dueDate, msgLang),
    link: f.invoices.map((i) => `${origin}/pay/${i.payToken}`).join(' '), owner: profile?.ownerName, phone: profile?.phone,
  });
  const phone = (f.phone || '').replace(/\D/g, '');
  const enc = encodeURIComponent(message);
  const ids = f.invoices.map((i) => i.invoiceId);
  const call = async (fn) => { setErr(null); try { await fn(); setDone(true); onDone(); } catch (e) { setErr(e); } };
  return (
    <li className="card">
      <div className="flex flex-wrap justify-between gap-2">
        <div>
          <h2 className="font-bold text-lg">{f.customerName}</h2>
          <div className="font-semibold text-brand-800">{t('fu.stage.' + f.stage)}</div>
          <div className="text-sm muted">{f.invoices.map((i) => i.invoiceNumber).join(', ')} · {money(f.totalPaise)} · {f.maxDaysOverdue > 0 ? `${f.maxDaysOverdue} ${t('common.days')}` : fmtDate(f.invoices[0].dueDate, lang)}</div>
        </div>
        <div>
          <label className="label text-sm" htmlFor={`ml-${f.customerId}-${f.stage}`}>{t('fu.language')}</label>
          <select id={`ml-${f.customerId}-${f.stage}`} className="input" value={msgLang} onChange={(e) => setMsgLang(e.target.value)}><option value="en">English</option><option value="hi">हिंदी</option></select>
        </div>
      </div>
      <ErrorBox error={err} />
      {f.stage === 'FINAL' && <Notice tone="warn">☎ {t('fu.callPrompt')} <a className="underline" href={`tel:${f.phone}`}>{f.phone}</a></Notice>}
      {f.stage === 'NEXT_STEPS' && <div className="my-2"><NextSteps /></div>}
      <label className="label mt-3" htmlFor={`msg-${f.customerId}-${f.stage}`}>Message</label>
      <textarea id={`msg-${f.customerId}-${f.stage}`} readOnly className="input min-h-[120px] py-2" value={message} />
      {done ? <Notice tone="ok">✓ {t('fu.sent')}</Notice> : (
        <>
          <div className="flex flex-wrap gap-2 mt-3">
            <a className="btn-secondary" href={`https://wa.me/${phone}?text=${enc}`} target="_blank" rel="noreferrer">{t('fu.whatsapp')}</a>
            <a className="btn-secondary" href={`mailto:${f.email}?subject=${encodeURIComponent(`${profile?.name}: ${f.invoices.map((i) => i.invoiceNumber).join(', ')}`)}&body=${enc}`}>{t('fu.email')}</a>
            <a className="btn-secondary" href={`sms:${f.phone}?body=${enc}`}>{t('fu.sms')}</a>
            {f.stage === 'DEMAND_LETTER' && <Link className="btn-secondary" to={`/letters/demand?customerId=${f.customerId}&invoiceIds=${ids.join(',')}`}>{t('fu.openLetter')}</Link>}
            <button className="btn-primary" onClick={() => call(() => api.post('/followups/mark-sent', { invoiceIds: ids, stage: f.stage, channel: 'MANUAL', message }))}>✓ {t('fu.markSent')}</button>
          </div>
          <div className="flex flex-wrap gap-2 items-end mt-3">
            <div><label className="label text-sm" htmlFor={`d-${f.customerId}-${f.stage}`}>{t('fu.snoozeUntil')} / {t('fu.promise')}</label>
              <input id={`d-${f.customerId}-${f.stage}`} type="date" min={addDays(today, 1)} className="input" value={date} onChange={(e) => setDate(e.target.value)} /></div>
            <button className="btn-secondary" onClick={() => call(() => Promise.all(ids.map((id) => api.post(`/invoices/${id}/snooze`, { date }))))}>{t('fu.snooze')}</button>
            <button className="btn-secondary" onClick={() => call(() => Promise.all(ids.map((id) => api.post(`/invoices/${id}/promise`, { date }))))}>{t('fu.promise')}</button>
          </div>
        </>
      )}
    </li>
  );
}

export default function FollowUps() {
  const { t, lang } = useApp();
  const { data, error, loading, reload } = useLoad('/followups/today');
  if (loading && !data) return <Loading />;
  if (error) return <ErrorBox error={error} />;
  return (
    <div>
      <PageTitle>{t('fu.title')}</PageTitle>
      <p className="muted mb-4">{t('fu.intro')}</p>
      {data.followUps.length === 0 && <Notice tone="ok">{t('fu.none')}</Notice>}
      <ul className="space-y-4">{data.followUps.map((f) => <FollowUpCard key={`${f.customerId}-${f.stage}`} f={f} onDone={() => {}} />)}</ul>
      {data.paused.length > 0 && (
        <section className="card mt-6">
          <h2 className="h2">⏸ {t('fu.paused')}</h2>
          <ul className="divide-y">{data.paused.map((p) => (
            <li key={p.invoiceId} className="py-2"><Link className="underline font-semibold" to={`/invoices/${p.invoiceId}`}>{p.customerName} · {p.invoiceNumber}</Link> — {t('fu.pausedReason.' + p.reason, { d: fmtDate(p.until, lang) })}</li>
          ))}</ul>
        </section>
      )}
      <button className="btn-secondary mt-4" onClick={reload}>↻</button>
    </div>
  );
}
