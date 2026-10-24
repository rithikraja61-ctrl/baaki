import { useState } from 'react';
import { useParams } from 'react-router-dom';
import { api } from '../api/client.js';
import { useApp, useLoad } from '../lib/AppContext.jsx';
import { fmtDate, money, upiUri } from '../lib/format.js';
import { ErrorBox, Loading, Notice, QR } from '../components/ui.jsx';
import InvoiceDocument from '../components/InvoiceDocument.jsx';

/** What the customer sees. No login. */
export default function PayLink() {
  const { token } = useParams();
  const { t, lang, setLang, today } = useApp();
  const { data: d, error, loading, reload } = useLoad(`/public/pay/${token}`);
  const [mode, setMode] = useState(null);
  const [ref, setRef] = useState('');
  const [paidOn, setPaidOn] = useState('');
  const [reason, setReason] = useState('');
  const [done, setDone] = useState(null);
  const [err, setErr] = useState(null);
  if (loading && !d) return <Loading />;
  if (error) return <div className="p-6"><Notice tone="warn">{t('pay.invalid')}</Notice></div>;
  const b = d.business, inv = d.invoice, owed = d.summary.outstandingPaise;
  const uri = upiUri({ upiId: b.upiId, name: b.name, amountPaise: owed, note: inv.invoiceNumber });
  const submit = async (path, body, msg) => { setErr(null); try { await api.post(`/public/pay/${token}/${path}`, body); setDone(msg); setMode(null); reload(); } catch (e) { setErr(e); } };
  return (
    <div className="max-w-xl mx-auto p-3 sm:p-6 space-y-4">
      <div className="flex justify-between items-center no-print">
        <h1 className="text-xl font-bold">{t('pay.title', { biz: b.name })}</h1>
        <button className="btn-secondary" aria-label={t('common.switchLang')} onClick={() => setLang(lang === 'en' ? 'hi' : 'en')}>{lang === 'en' ? 'हिंदी' : 'English'}</button>
      </div>
      <section className="card text-center">
        <div className="muted">{inv.invoiceNumber} · {d.customer.name}</div>
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
