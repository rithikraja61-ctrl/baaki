import { useState } from 'react';
import { useParams } from 'react-router-dom';
import { api } from '../api/client.js';
import { translate, useApp, useLoad } from '../lib/AppContext.jsx';
import { fmtDate, money, upiUri } from '../lib/format.js';
import { ErrorBox, LangPicker, Loading, Notice, QR } from '../components/ui.jsx';
import InvoiceDocument from '../components/InvoiceDocument.jsx';

/**
 * What the customer sees. No login. Shown in the customer's preferred language (set on their customer record);
 * ?lang=xx in the link or the picker overrides it for this visit only, without touching the owner's app language.
 */
export default function PayLink() {
  const { token } = useParams();
  const { today } = useApp();
  const { data: d, error, loading, reload } = useLoad(`/public/pay/${token}`);
  const [chosen, setChosen] = useState(() => new URLSearchParams(window.location.search).get('lang'));
  const lang = ['en', 'hi', 'mr'].includes(chosen) ? chosen : (d?.customer?.preferredLanguage || 'en');
  const t = (k, v) => translate(lang, k, v);
  const [mode, setMode] = useState(null);
  const [ref, setRef] = useState('');
  const [paidOn, setPaidOn] = useState('');
  const [reason, setReason] = useState('');
  const [done, setDone] = useState(null);
  const [err, setErr] = useState(null);
  if (loading && !d) return <Loading />;
  if (error || !d) return <div className="p-6"><Notice tone="warn">{translate(chosen || 'en', 'pay.invalid')}</Notice></div>;
  const b = d.business, inv = d.invoice, owed = d.summary.outstandingPaise;
  const uri = upiUri({ upiId: b.upiId, name: b.name, amountPaise: owed, note: inv.invoiceNumber });
  const submit = async (path, body, msg) => { setErr(null); try { await api.post(`/public/pay/${token}/${path}`, body); setDone(msg); setMode(null); reload(); } catch (e) { setErr(e); } };
  return (
    <div className="max-w-xl mx-auto p-3 sm:p-6 space-y-4" lang={lang}>
      <div className="flex justify-between items-center gap-2 no-print">
        <h1 className="text-xl font-bold min-w-0 break-words">{t('pay.title', { biz: b.name })}</h1>
        <LangPicker value={lang} onChange={setChosen} label={t('pay.language')} className="w-auto max-w-[8.5rem] shrink-0" />
      </div>
      <section className="card text-center">
        <div className="muted break-words">{inv.invoiceNumber} · {d.customer.name}</div>
        <div className="text-sm muted">{t('pay.amountOwed')}</div>
        <div className="text-4xl font-bold my-1">{money(owed)}</div>
        <div>{t('pay.dueOn', { d: fmtDate(inv.dueDate, lang) })}</div>
      </section>
      <ErrorBox error={err} />
      {done && <Notice tone="ok">{done}</Notice>}
      {owed === 0 ? <Notice tone="ok">{t('pay.paidFull')}</Notice> : (
        <>
          <section className="card text-center">
            <div className="flex justify-center"><QR text={uri} size={180} label={t('pay.payUpi')} /></div>
            <a className="btn-primary w-full mt-3" href={uri} onClick={(e) => { e.preventDefault(); submit('upi', {}, t('pay.paidSim')); }}>{t('pay.payUpi')}</a>
            <p className="text-sm muted mt-2">{t('pay.simulated')}</p>
          </section>
          <div className="grid gap-2">
            <button className="btn-secondary" aria-expanded={mode === 'paid'} onClick={() => setMode(mode === 'paid' ? null : 'paid')}>{t('pay.already')}</button>
            {mode === 'paid' && (
              <form className="card space-y-3" onSubmit={(e) => { e.preventDefault(); submit('paid', { reference: ref, paidDate: paidOn }, t('pay.thanks', { biz: b.name })); }}>
                <div><label className="label" htmlFor="ref">{t('pay.reference')}</label><input id="ref" className="input" value={ref} onChange={(e) => setRef(e.target.value)} required /></div>
                <div><label className="label" htmlFor="pd">{t('pay.paidOn')}</label><input id="pd" type="date" max={today} className="input" value={paidOn} onChange={(e) => setPaidOn(e.target.value)} required /></div>
                <button className="btn-primary">{t('pay.submit')}</button>
              </form>
            )}
            <button className="btn-secondary" aria-expanded={mode === 'wrong'} onClick={() => setMode(mode === 'wrong' ? null : 'wrong')}>{t('pay.wrong')}</button>
            {mode === 'wrong' && (
              <form className="card space-y-3" onSubmit={(e) => { e.preventDefault(); submit('dispute', { reason }, t('pay.thanks', { biz: b.name })); }}>
                <div><label className="label" htmlFor="why">{t('pay.wrongReason')}</label><textarea id="why" className="input min-h-[100px] py-2" value={reason} onChange={(e) => setReason(e.target.value)} required /></div>
                <button className="btn-primary">{t('pay.submit')}</button>
              </form>
            )}
          </div>
        </>
      )}
      <div className="overflow-x-auto"><InvoiceDocument d={d} /></div>
    </div>
  );
}
