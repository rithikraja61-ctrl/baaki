import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useApp, useLoad } from '../lib/AppContext.jsx';
import { money } from '../lib/format.js';
import { ErrorBox, Loading, PageTitle, StatusBadge } from '../components/ui.jsx';
import { Timeline } from './InvoiceDetail.jsx';

function Row({ i }) {
  const { t } = useApp();
  const [open, setOpen] = useState(false);
  const d = useLoad(open ? `/invoices/${i.id}` : null);
  return (
    <li className="card">
      <div className="flex flex-wrap justify-between gap-2 items-center">
        <div><span className="font-semibold">{i.invoiceNumber}</span> · {i.customerName} · {money(i.totalPaise)} <StatusBadge status={i.status} /></div>
        <div className="flex gap-2">
          <button className="btn-secondary" aria-expanded={open} onClick={() => setOpen(!open)}>{t('inv.timeline')}</button>
          <Link className="btn-primary" to={`/documents/${i.id}/evidence`}>{t('docs.evidence')}</Link>
        </div>
      </div>
      {open && (d.data ? <div className="mt-3"><Timeline events={d.data.timeline} /></div> : <Loading />)}
    </li>
  );
}

export default function Documents() {
  const { t } = useApp();
  const { data, error, loading } = useLoad('/invoices');
  const [q, setQ] = useState('');
  if (loading && !data) return <Loading />;
  if (error) return <ErrorBox error={error} />;
  const rows = data.filter((i) => i.invoiceNumber && `${i.invoiceNumber} ${i.customerName}`.toLowerCase().includes(q.toLowerCase()));
  return (
    <div>
      <PageTitle>{t('docs.title')}</PageTitle>
      <p className="muted mb-3">{t('docs.intro')}</p>
      <label className="label" htmlFor="dq">{t('common.search')}</label>
      <input id="dq" className="input mb-4" value={q} onChange={(e) => setQ(e.target.value)} placeholder={t('inv.searchPh')} />
      <ul className="space-y-2">{rows.map((i) => <Row key={i.id} i={i} />)}</ul>
    </div>
  );
}
