import { useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { api } from '../api/client.js';
import { useApp, useLoad } from '../lib/AppContext.jsx';
import { fmtDate, money, rateLabel } from '../lib/format.js';
import { ErrorBox, Loading, Notice, PageTitle, StatusBadge } from '../components/ui.jsx';

export function Timeline({ events }) {
  const { t, lang } = useApp();
  return (
    <ol className="border-l-2 border-brand-600 ml-2 space-y-3">
      {events.map((e) => (
        <li key={e.id} className="ml-4">
          <div className="font-semibold">{t('ev.' + e.type)}{e.stage ? ` · ${t('fu.stage.' + e.stage)}` : ''}{e.channel ? ` · ${e.channel}` : ''}</div>
          <div className="text-sm muted">{fmtDate(e.eventAt, lang)} {String(e.eventAt).slice(11, 16)}</div>
          {e.details && <div className="text-sm whitespace-pre-wrap break-words">{e.details}</div>}
        </li>
      ))}
    </ol>
  );
}

export default function InvoiceDetail() {
  const { id } = useParams();
  const nav = useNavigate();
  const { t, lang, today } = useApp();
  const { data: d, error, loading, reload } = useLoad(`/invoices/${id}`);
  const [msg, setMsg] = useState(null);
  const [err, setErr] = useState(null);
  const [promise, setPromise] = useState('');
  const [snooze, setSnooze] = useState('');
  if (loading && !d) return <Loading />;
  if (error) return <ErrorBox error={error} />;
  const s = d.summary, inv = d.invoice;
  const link = `${window.location.origin}/pay/${inv.payToken}`;
  const act = async (fn, ok) => { setErr(null); try { await fn(); setMsg(ok); reload(); } catch (e) { setErr(e); } };
  return (
    <div>
      <PageTitle actions={<>
        <Link className="btn-secondary" to={`/invoices/${id}/print`}>🖨 {t('inv.viewInvoice')}</Link>
        {inv.status !== 'PAID' && <Link className="btn-secondary" to={`/invoices/${id}/edit`}>{t('inv.edit')}</Link>}
        {inv.status === 'DRAFT' && <button className="btn-primary" onClick={() => act(() => api.post(`/invoices/${id}/send`), t('common.saved'))}>{t('inv.sendNow')}</button>}
        {inv.status === 'DRAFT' && <button className="btn-danger" onClick={() => act(async () => { await api.del(`/invoices/${id}`); nav('/invoices'); })}>{t('inv.deleteDraft')}</button>}
      </>}>{d.docTitle} {inv.invoiceNumber || `(${t('status.DRAFT')})`}</PageTitle>
      <ErrorBox error={err} />
      {msg && <Notice tone="ok">{msg}</Notice>}
      {d.eInvoiceRequired && <Notice tone="warn">{t('inv.eInvoiceNote')}</Notice>}
      <div className="grid lg:grid-cols-3 gap-4">
        <div className="lg:col-span-2 space-y-4 min-w-0">
          <section className="card">
            <div className="flex flex-wrap gap-3 items-center mb-2"><StatusBadge status={s.status} />{s.paused && <span className="font-semibold">⏸ {t('inv.paused')}</span>}</div>
            <dl className="grid grid-cols-2 gap-2">
              <dt className="muted">{t('common.customer')}</dt><dd className="font-semibold break-words">{d.customer.name}</dd>
              <dt className="muted">{t('inv.date')}</dt><dd>{fmtDate(inv.invoiceDate, lang)}</dd>
              <dt className="muted">{t('inv.dueDate')}</dt><dd>{fmtDate(inv.dueDate, lang)}{s.daysOverdue > 0 ? ` (${s.daysOverdue} ${t('common.days')})` : ''}</dd>
              <dt className="muted">{t('common.total')}</dt><dd className="font-bold">{money(inv.totalPaise)}</dd>
              <dt className="muted">{t('common.paid')}</dt><dd>{money(s.paidPaise)}</dd>
              <dt className="muted">{t('common.owed')}</dt><dd className="font-bold">{money(s.outstandingPaise)}</dd>
            </dl>
          </section>
          <section className="card overflow-x-auto">
            <h2 className="h2">{t('inv.lines')}</h2>
            <table className="table text-sm">
              <thead><tr><th>{t('newInv.description')}</th><th>{t('newInv.hsn')}</th><th className="text-right">{t('newInv.qty')}</th><th className="text-right">{t('newInv.taxable')}</th><th className="text-right">GST</th><th className="text-right">{t('common.total')}</th></tr></thead>
              <tbody>{d.lines.map((l) => (
                <tr key={l.id}><td>{l.description}</td><td>{l.hsnSac}</td><td className="text-right">{Number(l.quantity)} {l.unit}</td><td className="text-right">{money(l.taxablePaise)}</td><td className="text-right">{rateLabel(l.gstRateBp)}</td><td className="text-right">{money(l.totalPaise)}</td></tr>
              ))}</tbody>
            </table>
          </section>
          {s.outstandingPaise > 0 && inv.status !== 'DRAFT' && (
            <section className="card space-y-3">
              <div>
                <div className="label">{t('inv.payLink')}</div>
                <div className="flex flex-wrap gap-2 items-center"><a className="underline break-all" href={link} target="_blank" rel="noreferrer">{link}</a>
                  <button className="btn-secondary" onClick={() => { navigator.clipboard?.writeText(link); setMsg(t('inv.copied')); }}>{t('inv.copyLink')}</button></div>
              </div>
              <form className="flex flex-wrap gap-2 items-end" onSubmit={(e) => { e.preventDefault(); act(() => api.post(`/invoices/${id}/promise`, { date: promise }), t('common.saved')); }}>
                <div><label className="label" htmlFor="pr">{t('inv.promise')}</label><input id="pr" type="date" min={today} className="input" value={promise} onChange={(e) => setPromise(e.target.value)} required /></div>
                <button className="btn-secondary">{t('inv.setDate')}</button>
              </form>
              <form className="flex flex-wrap gap-2 items-end" onSubmit={(e) => { e.preventDefault(); act(() => api.post(`/invoices/${id}/snooze`, { date: snooze }), t('common.saved')); }}>
                <div><label className="label" htmlFor="sn">{t('inv.snooze')}</label><input id="sn" type="date" min={today} className="input" value={snooze} onChange={(e) => setSnooze(e.target.value)} required /></div>
                <button className="btn-secondary">{t('inv.setDate')}</button>
              </form>
              <div className="flex flex-wrap gap-2">
                <Link className="btn-secondary" to={`/letters/demand?customerId=${inv.customerId}&invoiceIds=${inv.id}`}>{t('inv.demandLetter')}</Link>
                <Link className="btn-secondary" to={`/documents/${inv.id}/evidence`}>{t('inv.evidence')}</Link>
              </div>
            </section>
          )}
        </div>
        <section className="card min-w-0"><h2 className="h2">{t('inv.timeline')}</h2><Timeline events={d.timeline} /></section>
      </div>
    </div>
  );
}
