import { useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { api } from '../api/client.js';
import { useApp, useLoad } from '../lib/AppContext.jsx';
import { fmtDate, money } from '../lib/format.js';
import { ErrorBox, Loading, Notice, QR } from '../components/ui.jsx';
import { NextSteps } from './FollowUps.jsx';

/**
 * "Payment Demand Letter": a plain request for payment from the business. No emblem, seal, or court wording.
 * Interface chrome follows the app language; the letter body follows the chosen letter language.
 */
export default function DemandLetter() {
  const { t, lang } = useApp();
  const [sp, setSp] = useSearchParams();
  const customerId = sp.get('customerId') || '';
  const invoiceIds = sp.get('invoiceIds') || '';
  const [interest, setInterest] = useState(false);
  const [msg, setMsg] = useState(null);
  const [err, setErr] = useState(null);
  const custs = useLoad('/customers');
  const q = customerId ? `/letters/demand?customerId=${customerId}${invoiceIds ? `&invoiceIds=${invoiceIds}` : ''}&includeInterest=${interest}` : null;
  const { data: L, error, loading } = useLoad(q, [interest]);
  const maxDays = L ? Math.max(...L.lines.map((l) => l.daysOverdue)) : 0;
  return (
    <div className="p-3 sm:p-6">
      <div className="no-print max-w-3xl mx-auto mb-4 space-y-3">
        <div className="flex flex-wrap gap-2"><Link className="btn-secondary" to="/followups">← {t('common.back')}</Link>
          {L && <button className="btn-primary" onClick={() => window.print()}>🖨 {t('common.print')}</button>}
          {L && <button className="btn-secondary" onClick={async () => { setErr(null); try { await api.post('/letters/demand/log', { customerId: Number(customerId), invoiceIds: L.lines.map((l) => l.invoiceId), channel: 'PRINT' }); setMsg(t('letter.logged')); } catch (e) { setErr(e); } }}>{t('letter.log')}</button>}
        </div>
        <div className="card grid sm:grid-cols-2 gap-3">
          <div><label className="label" htmlFor="lc">{t('letter.customer')}</label>
            <select id="lc" className="input" value={customerId} onChange={(e) => setSp({ customerId: e.target.value })}>
              <option value="">{t('newInv.pick')}</option>
              {(custs.data || []).filter((c) => c.owedPaise > 0).map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
            </select></div>
          {L?.msmeEligible && <label className="flex items-center gap-3 min-h-[48px] sm:mt-6"><input type="checkbox" className="w-6 h-6" checked={interest} onChange={(e) => setInterest(e.target.checked)} />{t('letter.includeInterest')}</label>}
        </div>
        <ErrorBox error={err || error} />
        {msg && <Notice tone="ok">{msg}</Notice>}
        {L && maxDays >= 45 && <NextSteps />}
      </div>
      {loading && q && !L && <Loading />}
      {L && (
        <article className="a4 shadow" lang={lang} aria-label={L.title}>
          <header className="border-b-2 border-black pb-2 flex flex-wrap justify-between gap-4">
            <div>
              <div className="text-xl font-bold">{L.business.name}</div>
              <div>{L.business.address}</div>
              {L.business.gstin && <div>GSTIN: {L.business.gstin}</div>}
              {L.business.udyamRegistered && L.business.udyamNumber && <div>Udyam: {L.business.udyamNumber}</div>}
              <div>{L.business.phone} · {L.business.email}</div>
            </div>
            <div className="text-right">{t('common.date')}: {fmtDate(L.letterDate, lang)}</div>
          </header>
          <h1 className="text-2xl font-bold text-center my-3">{t('letter.title')}</h1>
          <p className="border-2 border-black p-2 font-semibold">{t('letter.disclaimer', { biz: L.business.name })}</p>
          {lang === 'hi' && <p className="text-sm mt-1">{L.disclaimer}</p>}
          <div className="mt-3"><div className="font-bold">{t('letter.to')}</div><div>{L.customer.name}</div><div>{L.customer.address}</div>{L.customer.gstin && <div>GSTIN: {L.customer.gstin}</div>}</div>
          <p className="mt-3 font-semibold">{t('letter.subject')}</p>
          <p className="mt-2">{t('letter.dear', { name: L.customer.contactPerson || L.customer.name })}</p>
          <p className="mt-2">{t('letter.body1', { date: fmtDate(L.payByDate, lang) })}</p>
          <div className="overflow-x-auto"><table className="mt-3 text-sm">
            <thead><tr><th>{t('letter.colInvoice')}</th><th>{t('letter.colDate')}</th><th>{t('letter.colDue')}</th><th>{t('letter.colAmount')}</th><th>{t('letter.colPaid')}</th><th>{t('letter.colOwed')}</th><th>{t('letter.colDays')}</th></tr></thead>
            <tbody>
              {L.lines.map((l) => (
                <tr key={l.invoiceId}><td>{l.invoiceNumber}</td><td>{fmtDate(l.invoiceDate, lang)}</td><td>{fmtDate(l.dueDate, lang)}</td><td className="text-right">{money(l.amountPaise)}</td><td className="text-right">{money(l.paidPaise)}</td><td className="text-right">{money(l.owedPaise)}</td><td className="text-right">{l.daysOverdue}</td></tr>
              ))}
              <tr><td colSpan={5} className="font-bold">{t('letter.totalOwed')}</td><td className="text-right font-bold">{money(L.owedPaise)}</td><td /></tr>
            </tbody>
          </table></div>
          {L.msmeEligible && (
            <div className="mt-3 text-sm">
              <p>{L.msmeLine}</p>
              <p className="mt-1">{t('letter.interest')}: <strong>{money(L.interestPaise)}</strong>{!L.includeInterest && ` ${t('letter.notIncluded')}`}</p>
              <p className="text-xs">{t('letter.interestNote', { rate: L.interestRatePercent, bank: L.bankRatePercent })}</p>
            </div>
          )}
          <p className="mt-3 text-lg">{t('letter.demand')}: <strong>{money(L.demandPaise)}</strong> <span className="text-sm">({L.demandInWords})</span></p>
          <p className="font-semibold">{t('letter.payBy')}: {fmtDate(L.payByDate, lang)}</p>
          <section className="mt-3 flex flex-wrap gap-4 items-start">
            <div className="flex-1">
              <div className="font-bold">{t('letter.howToPay')}</div>
              <div>{t('letter.bank')}: {L.business.bankName}</div>
              <div>{t('letter.accName')}: {L.business.accountName}</div>
              <div>{t('letter.accNo')}: {L.business.accountNumber}</div>
              <div>{t('letter.ifsc')}: {L.business.ifsc}</div>
              <div>{t('letter.upi')}: {L.business.upiId}</div>
            </div>
            <div className="text-center text-sm"><QR text={L.upiUri} size={120} label={t('letter.scan')} /><div>{t('letter.scan')}</div></div>
          </section>
          <p className="mt-3">{t('letter.dispute', { name: L.business.ownerName, phone: L.business.phone, email: L.business.email })}</p>
          <p className="mt-2">{t('letter.closing')}</p>
          <div className="mt-8">{t('letter.regards')}<div className="mt-10 border-t border-black inline-block pt-1 pr-10">{L.business.ownerName}, {L.business.name}<br />{t('letter.sign')}</div></div>
        </article>
      )}
    </div>
  );
}
