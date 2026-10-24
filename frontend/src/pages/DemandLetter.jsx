import { useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { api } from '../api/client.js';
import { translate, useApp, useLoad } from '../lib/AppContext.jsx';
import { fmtDate, money } from '../lib/format.js';
import { Bi, ErrorBox, Loading, Notice, QR } from '../components/ui.jsx';
import { NextSteps } from './FollowUps.jsx';

/**
 * "Payment Demand Letter": a plain request for payment from the business. No emblem, seal, or court wording.
 * Interface chrome follows the app language. The letter itself is in English; if the owner picked a second
 * document language in Settings, it is printed in small text under each heading (and under the disclaimer).
 */
export default function DemandLetter() {
  const { t } = useApp();
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
  const second = L?.business?.docSecondLanguage || null;
  const te = (k, v) => translate('en', k, v);
  const H = (props) => <Bi second={second} {...props} />;
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
        <article className="a4 shadow" lang="en" aria-label={L.title}>
          <header className="border-b-2 border-black pb-2 flex flex-wrap justify-between gap-4">
            <div>
              <div className="text-xl font-bold">{L.business.name}</div>
              <div>{L.business.address}</div>
              {L.business.gstin && <div>GSTIN: {L.business.gstin}</div>}
              {L.business.udyamRegistered && L.business.udyamNumber && <div>Udyam: {L.business.udyamNumber}</div>}
              <div>{L.business.phone} · {L.business.email}</div>
            </div>
            <div className="text-right"><H k="common.date" />: {fmtDate(L.letterDate)}</div>
          </header>
          <h1 className="text-2xl font-bold text-center my-3"><H k="letter.title" /></h1>
          <p className="border-2 border-black p-2 font-semibold"><H k="letter.disclaimer" vars={{ biz: L.business.name }} /></p>
          <div className="mt-3"><div className="font-bold"><H k="letter.to" /></div><div>{L.customer.name}</div><div>{L.customer.address}</div>{L.customer.gstin && <div>GSTIN: {L.customer.gstin}</div>}</div>
          <p className="mt-3 font-semibold"><H k="letter.subject" /></p>
          <p className="mt-2">{te('letter.dear', { name: L.customer.contactPerson || L.customer.name })}</p>
          <p className="mt-2">{te('letter.body1', { date: fmtDate(L.payByDate) })}</p>
          <div className="overflow-x-auto"><table className="mt-3 text-sm">
            <thead><tr><th><H k="letter.colInvoice" /></th><th><H k="letter.colDate" /></th><th><H k="letter.colDue" /></th><th><H k="letter.colAmount" /></th><th><H k="letter.colPaid" /></th><th><H k="letter.colOwed" /></th><th><H k="letter.colDays" /></th></tr></thead>
            <tbody>
              {L.lines.map((l) => (
                <tr key={l.invoiceId}><td>{l.invoiceNumber}</td><td>{fmtDate(l.invoiceDate)}</td><td>{fmtDate(l.dueDate)}</td><td className="text-right">{money(l.amountPaise)}</td><td className="text-right">{money(l.paidPaise)}</td><td className="text-right">{money(l.owedPaise)}</td><td className="text-right">{l.daysOverdue}</td></tr>
              ))}
              <tr><td colSpan={5} className="font-bold"><H k="letter.totalOwed" /></td><td className="text-right font-bold">{money(L.owedPaise)}</td><td /></tr>
            </tbody>
          </table></div>
          {L.msmeEligible && (
            <div className="mt-3 text-sm">
              <p>{te('letter.msmeLine')}</p>
              <p className="mt-1"><H k="letter.interest" />: <strong>{money(L.interestPaise)}</strong>{!L.includeInterest && ` ${te('letter.notIncluded')}`}</p>
              <p className="text-xs">{te('letter.interestNote', { rate: L.interestRatePercent, bank: L.bankRatePercent })}</p>
            </div>
          )}
          <p className="mt-3 text-lg"><H k="letter.demand" />: <strong>{money(L.demandPaise)}</strong> <span className="text-sm">({L.demandInWords})</span></p>
          <p className="font-semibold"><H k="letter.payBy" />: {fmtDate(L.payByDate)}</p>
          <section className="mt-3 flex flex-wrap gap-4 items-start">
            <div className="flex-1">
              <div className="font-bold"><H k="letter.howToPay" /></div>
              <div><H k="letter.bank" />: {L.business.bankName}</div>
              <div><H k="letter.accName" />: {L.business.accountName}</div>
              <div><H k="letter.accNo" />: {L.business.accountNumber}</div>
              <div>IFSC: {L.business.ifsc}</div>
              <div>UPI ID: {L.business.upiId}</div>
            </div>
            <div className="text-center text-sm"><QR text={L.upiUri} size={120} label={te('letter.scan')} /><div><H k="letter.scan" /></div></div>
          </section>
          <p className="mt-3">{te('letter.dispute', { name: L.business.ownerName, phone: L.business.phone, email: L.business.email })}</p>
          <p className="mt-2">{te('letter.closing')}</p>
          <div className="mt-8">{te('letter.regards')}<div className="mt-10 border-t border-black inline-block pt-1 pr-10">{L.business.ownerName}, {L.business.name}<br /><H k="letter.sign" /></div></div>
        </article>
      )}
    </div>
  );
}
