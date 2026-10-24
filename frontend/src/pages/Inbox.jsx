import { useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../api/client.js';
import { useApp, useLoad } from '../lib/AppContext.jsx';
import { fmtDate, money, paiseToInput, toPaise } from '../lib/format.js';
import { ErrorBox, Loading, Notice, PageTitle } from '../components/ui.jsx';

function Item({ v, onDone }) {
  const { t, lang } = useApp();
  const it = v.item, inv = v.invoice;
  const [reply, setReply] = useState('');
  const [cn, setCn] = useState(paiseToInput(Math.round(inv.outstandingPaise / 10)));
  const [err, setErr] = useState(null);
  const go = async (action, extra = {}) => { setErr(null); try { await api.post(`/inbox/${it.id}/resolve`, { action, reply, ...extra }); onDone(); } catch (e) { setErr(e); } };
  return (
    <li className="card">
      <div className="font-semibold text-brand-800">{t('inbox.' + it.kind)}</div>
      <Link className="underline font-bold" to={`/invoices/${inv.id}`}>{inv.customerName} · {inv.invoiceNumber}</Link>
      <div className="muted text-sm">{fmtDate(it.createdAt, lang)} · {t('common.owed')} {money(inv.outstandingPaise)}</div>
      {it.reason && <blockquote className="border-l-4 pl-3 my-2">“{it.reason}”</blockquote>}
      {it.kind === 'PAID_CLAIM' && <p className="my-2">{t('inbox.ref', { r: it.reference, d: fmtDate(it.paidDate, lang) })}</p>}
      <ErrorBox error={err} />
      <label className="label" htmlFor={`r${it.id}`}>{t('common.reply')}</label>
      <textarea id={`r${it.id}`} className="input min-h-[80px] py-2" placeholder={t('inbox.replyPh')} value={reply} onChange={(e) => setReply(e.target.value)} />
      <div className="flex flex-wrap gap-2 mt-3 items-end">
        {it.kind === 'DISPUTE' && (
          <>
            <div><label className="label text-sm" htmlFor={`cn${it.id}`}>₹</label><input id={`cn${it.id}`} inputMode="decimal" className="input w-36" value={cn} onChange={(e) => setCn(e.target.value)} /></div>
            <button className="btn-primary" onClick={() => go('CREDIT_NOTE', { creditNotePaise: toPaise(cn) })}>{t('inbox.creditNote')} {cn && !Number.isNaN(toPaise(cn)) ? money(toPaise(cn)) : ''}</button>
          </>
        )}
        {it.kind === 'PAID_CLAIM' && <button className="btn-primary" onClick={() => go('MARK_PAID')}>{t('inbox.markPaid')}</button>}
        <button className="btn-danger" onClick={() => go('REJECT')}>{t('inbox.reject')}</button>
        <Link className="btn-secondary" to={`/invoices/${inv.id}/edit`}>{t('inbox.editInvoice')}</Link>
        <button className="btn-secondary" onClick={() => go('RESOLVED_EDITED')}>{t('inbox.doneEditing')}</button>
      </div>
    </li>
  );
}

export default function Inbox() {
  const { t } = useApp();
  const { data, error, loading, reload } = useLoad('/inbox');
  if (loading && !data) return <Loading />;
  if (error) return <ErrorBox error={error} />;
  return (
    <div>
      <PageTitle>{t('inbox.title')}</PageTitle>
      <p className="muted mb-4">{t('inbox.intro')}</p>
      {data.length === 0 && <Notice tone="ok">{t('inbox.none')}</Notice>}
      <ul className="space-y-3">{data.map((v) => <Item key={v.item.id} v={v} onDone={reload} />)}</ul>
    </div>
  );
}
