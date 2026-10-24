import { Link, useParams } from 'react-router-dom';
import { useApp, useLoad } from '../lib/AppContext.jsx';
import { fmtDate, money } from '../lib/format.js';
import { ErrorBox, Loading } from '../components/ui.jsx';
import InvoiceDocument from '../components/InvoiceDocument.jsx';

/** Print-ready evidence pack: invoice, then every timeline record, payments, credit notes and customer replies. */
export default function EvidencePack() {
  const { id } = useParams();
  const { t, lang } = useApp();
  const { data: d, error, loading } = useLoad(`/invoices/${id}`);
  if (loading && !d) return <Loading />;
  if (error) return <ErrorBox error={error} />;
  return (
    <div className="py-4">
      <div className="no-print flex gap-2 justify-center mb-4">
        <Link className="btn-secondary" to={`/invoices/${id}`}>← {t('common.back')}</Link>
        <button className="btn-primary" onClick={() => window.print()}>🖨 {t('common.print')}</button>
      </div>
      <div className="page-break"><InvoiceDocument d={d} /></div>
      <article className="a4 shadow mt-4">
        <h1 className="text-xl font-bold">{t('inv.evidence')}: {d.invoice.invoiceNumber} · {d.customer.name}</h1>
        <p>{d.business.name} · {t('common.total')} {money(d.invoice.totalPaise)} · {t('common.paid')} {money(d.summary.paidPaise)} · {t('common.owed')} {money(d.summary.outstandingPaise)}</p>
        <h2 className="font-bold mt-4">{t('inv.timeline')}</h2>
        <div className="overflow-x-auto"><table className="text-sm"><thead><tr><th>{t('common.date')}</th><th>{t('common.event')}</th><th>{t('common.details')}</th></tr></thead>
          <tbody>{d.timeline.map((e) => <tr key={e.id}><td>{fmtDate(e.eventAt, lang)} {String(e.eventAt).slice(11, 16)}</td><td>{t('ev.' + e.type)}{e.stage ? ` · ${t('fu.stage.' + e.stage)}` : ''}{e.channel ? ` · ${e.channel}` : ''}</td><td className="whitespace-pre-wrap">{e.details}</td></tr>)}</tbody></table></div>
        {d.payments.length > 0 && <><h2 className="font-bold mt-4">{t('nav.payments')}</h2>
          <table className="text-sm"><thead><tr><th>{t('common.date')}</th><th>{t('pmt.reference')}</th><th>{t('common.amount')}</th></tr></thead><tbody>{d.payments.map((p) => <tr key={p.paymentId}><td>{fmtDate(p.date, lang)}</td><td>{p.reference}</td><td className="text-right">{money(p.amountPaise)}</td></tr>)}</tbody></table></>}
        {d.creditNotes.length > 0 && <><h2 className="font-bold mt-4">{t('ev.CREDIT_NOTE')}</h2>
          <table className="text-sm"><tbody>{d.creditNotes.map((c) => <tr key={c.id}><td>{c.noteNumber}</td><td>{fmtDate(c.noteDate, lang)}</td><td>{c.reason}</td><td className="text-right">{money(c.amountPaise)}</td></tr>)}</tbody></table></>}
        {d.inbox.length > 0 && <><h2 className="font-bold mt-4">{t('inbox.title')}</h2>
          <table className="text-sm"><tbody>{d.inbox.map((x) => <tr key={x.id}><td>{fmtDate(x.createdAt, lang)}</td><td>{t('inbox.' + x.kind)}</td><td>{x.reason || `${x.reference} · ${fmtDate(x.paidDate, lang)}`}</td><td>{x.status}{x.reply ? ` · ${x.reply}` : ''}</td></tr>)}</tbody></table></>}
      </article>
    </div>
  );
}
