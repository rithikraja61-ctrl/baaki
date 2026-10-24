import { Link } from 'react-router-dom';
import { useApp, useLoad } from '../lib/AppContext.jsx';
import { money0 } from '../lib/format.js';
import { ErrorBox, Grade, Loading, PageTitle, StatusBadge } from '../components/ui.jsx';

export function ChaseReason({ c }) {
  const { t } = useApp();
  const parts = [t('chase.why', { amt: money0(c.outstandingPaise), d: c.daysOverdue })];
  parts.push(c.avgDaysLate >= 1 ? t('chase.usually', { n: Math.round(c.avgDaysLate) }) : t('chase.onTime'));
  if (c.coversShortfallWeek) parts.push(t('chase.covers', { w: c.coversShortfallWeek }));
  if (c.paused) parts.push(t('chase.paused'));
  return <>{parts.join(', ')}</>;
}

export default function Chase() {
  const { t } = useApp();
  const { data, error, loading } = useLoad('/chase');
  if (loading && !data) return <Loading />;
  if (error) return <ErrorBox error={error} />;
  return (
    <div>
      <PageTitle>{t('chase.title')}</PageTitle>
      <p className="muted mb-4">{t('chase.intro')}</p>
      {data.length === 0 && <p>{t('chase.none')}</p>}
      <ol className="space-y-3">
        {data.map((c, i) => (
          <li key={c.invoiceId} className="card flex flex-wrap gap-3 items-start">
            <div className="text-center w-16">
              <div className="text-3xl font-bold">{c.score}</div>
              <div className="text-xs muted">{t('chase.score')} · #{i + 1}</div>
            </div>
            <div className="flex-1 min-w-[200px]">
              <div className="flex flex-wrap items-center gap-2">
                <Link className="font-semibold underline text-lg" to={`/invoices/${c.invoiceId}`}>{c.customerName}</Link>
                <Grade grade={c.grade} /><StatusBadge status={c.status} />
              </div>
              <div className="text-sm muted">{c.invoiceNumber}</div>
              <p className="mt-1">“<ChaseReason c={c} />”</p>
            </div>
            <div className="flex flex-wrap gap-2">
              <Link className="btn-secondary" to="/followups">{t('nav.followups')}</Link>
              {c.daysOverdue >= 30 && <Link className="btn-secondary" to={`/letters/demand?customerId=${c.customerId}`}>{t('letter.title')}</Link>}
            </div>
          </li>
        ))}
      </ol>
    </div>
  );
}
