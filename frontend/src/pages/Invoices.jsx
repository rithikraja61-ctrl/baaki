import { useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { useApp, useLoad } from '../lib/AppContext.jsx';
import { addDays, fmtDate, money } from '../lib/format.js';
import { ErrorBox, Loading, PageTitle, StatusBadge } from '../components/ui.jsx';

const STATUSES = ['DRAFT', 'SENT', 'DUE_SOON', 'OVERDUE', 'PART_PAID', 'PAID', 'DISPUTED'];

export default function Invoices() {
  const { t, lang, today } = useApp();
  const { data, error, loading } = useLoad('/invoices');
  const [q, setQ] = useState('');
  const [cust, setCust] = useState('');
  const [status, setStatus] = useState('');
  const [due, setDue] = useState('');
  const [od, setOd] = useState('');
  const list = Array.isArray(data) ? data : [];
  const customers = useMemo(() => [...new Map(list.map((i) => [i.customerId, i.customerName])).entries()].sort((a, b) => a[1].localeCompare(b[1])), [list]);
  const rows = useMemo(() => list.filter((i) => {
    if (q && !(`${i.invoiceNumber || ''} ${i.customerName}`.toLowerCase().includes(q.toLowerCase()))) return false;
    if (cust && String(i.customerId) !== cust) return false;
    if (status && i.status !== status) return false;
    if (due && today) {
      if (due === 'week' && !(i.dueDate >= today && i.dueDate <= addDays(today, 7))) return false;
      if (due === 'month' && !(i.dueDate >= today && i.dueDate <= addDays(today, 30))) return false;
      if (due === 'overdue' && !(i.daysOverdue > 0)) return false;
    }
    if (od === '1' && !(i.daysOverdue >= 1 && i.daysOverdue <= 30)) return false;
    if (od === '31' && !(i.daysOverdue >= 31 && i.daysOverdue <= 60)) return false;
    if (od === '61' && !(i.daysOverdue >= 61)) return false;
    return true;
  }), [data, q, cust, status, due, od, today]);
  if (loading && !data) return <Loading />;
  if (error) return <ErrorBox error={error} />;
  return (
    <div>
      <PageTitle actions={<Link to="/invoices/new" className="btn-primary">+ {t('nav.newInvoice')}</Link>}>{t('inv.title')}</PageTitle>
      <div className="card mb-4 grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-5 gap-3">
        <div className="lg:col-span-2"><label className="label" htmlFor="q">{t('common.search')}</label><input id="q" className="input" value={q} onChange={(e) => setQ(e.target.value)} placeholder={t('inv.searchPh')} /></div>
        <div><label className="label" htmlFor="fc">{t('inv.filterCustomer')}</label><select id="fc" className="input" value={cust} onChange={(e) => setCust(e.target.value)}><option value="">{t('common.all')}</option>{customers.map(([id, n]) => <option key={id} value={id}>{n}</option>)}</select></div>
        <div><label className="label" htmlFor="fs">{t('inv.filterStatus')}</label><select id="fs" className="input" value={status} onChange={(e) => setStatus(e.target.value)}><option value="">{t('common.all')}</option>{STATUSES.map((s) => <option key={s} value={s}>{t('status.' + s)}</option>)}</select></div>
        <div className="grid grid-cols-2 gap-2">
          <div><label className="label" htmlFor="fd">{t('inv.filterDue')}</label><select id="fd" className="input" value={due} onChange={(e) => setDue(e.target.value)}><option value="">{t('inv.dueAny')}</option><option value="week">{t('inv.dueThisWeek')}</option><option value="month">{t('inv.dueThisMonth')}</option><option value="overdue">{t('inv.dueOverdue')}</option></select></div>
          <div><label className="label" htmlFor="fo">{t('inv.filterOverdue')}</label><select id="fo" className="input" value={od} onChange={(e) => setOd(e.target.value)}><option value="">{t('inv.odAny')}</option><option value="1">{t('inv.od1')}</option><option value="31">{t('inv.od31')}</option><option value="61">{t('inv.od61')}</option></select></div>
        </div>
      </div>
      <p className="muted mb-2" aria-live="polite">{t('inv.count', { n: rows.length })}</p>
      <ul className="md:hidden space-y-2">
        {rows.map((i) => (
          <li key={i.id} className="card">
            <Link to={`/invoices/${i.id}`} className="font-semibold underline">{i.invoiceNumber || t('status.DRAFT')} · {i.customerName}</Link>
            <div className="flex flex-wrap justify-between items-center mt-1 gap-2"><StatusBadge status={i.status} /><span className="font-bold">{money(i.outstandingPaise || i.totalPaise)}</span></div>
            <div className="text-sm muted mt-1">{t('inv.dueDate')}: {fmtDate(i.dueDate, lang)}{i.daysOverdue > 0 ? ` · ${i.daysOverdue} ${t('common.days')}` : ''}</div>
          </li>
        ))}
      </ul>
      <div className="hidden md:block card overflow-x-auto">
        <table className="table">
          <thead><tr><th>{t('inv.number')}</th><th>{t('common.customer')}</th><th>{t('inv.date')}</th><th>{t('inv.dueDate')}</th><th>{t('inv.status')}</th><th className="text-right">{t('common.total')}</th><th className="text-right">{t('common.owed')}</th><th className="text-right">{t('inv.daysOverdue')}</th></tr></thead>
          <tbody>
            {rows.map((i) => (
              <tr key={i.id}>
                <td><Link className="underline font-semibold" to={`/invoices/${i.id}`}>{i.invoiceNumber || t('status.DRAFT')}</Link></td>
                <td>{i.customerName}</td><td>{fmtDate(i.invoiceDate, lang)}</td><td>{fmtDate(i.dueDate, lang)}</td>
                <td><StatusBadge status={i.status} />{i.paused && <span className="sr-only">, {t('inv.paused')}</span>}</td>
                <td className="text-right">{money(i.totalPaise)}</td><td className="text-right">{money(i.outstandingPaise)}</td><td className="text-right">{i.daysOverdue || ''}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
