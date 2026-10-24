import { Link } from 'react-router-dom';
import { useApp, useLoad } from '../lib/AppContext.jsx';
import { money0 } from '../lib/format.js';
import { ErrorBox, Loading, Notice, PageTitle, Term } from '../components/ui.jsx';
import { ChaseReason } from './Chase.jsx';

function Stat({ label, tip, value, sub, tone }) {
  return (
    <div className={`card ${tone === 'bad' ? 'border-red-400' : ''}`}>
      <div className="text-sm font-semibold muted">{tip ? <Term tip={tip}>{label}</Term> : label}</div>
      <div className={`text-2xl font-bold mt-1 ${tone === 'bad' ? 'text-red-800' : ''}`}>{value}</div>
      {sub && <div className="text-sm muted mt-1">{sub}</div>}
    </div>
  );
}

const AGE = [['notDue', 'notDue', '#2f855a'], ['d1to30', 'd1', '#d69e2e'], ['d31to60', 'd31', '#dd6b20'], ['d61to90', 'd61', '#c53030'], ['d90plus', 'd90', '#742a2a']];

export function AgeingBar({ ageing }) {
  const { t } = useApp();
  const total = AGE.reduce((s, [k]) => s + ageing[k], 0) || 1;
  let x = 0;
  return (
    <div>
      <svg viewBox="0 0 100 10" preserveAspectRatio="none" className="w-full h-8 rounded" role="img"
        aria-label={AGE.map(([k, l]) => `${t('dash.' + l)}: ${money0(ageing[k])}`).join(', ')}>
        {AGE.map(([k, , c]) => { const w = (ageing[k] / total) * 100; const r = <rect key={k} x={x} y="0" width={w} height="10" fill={c} />; x += w; return r; })}
      </svg>
      <ul className="grid grid-cols-2 sm:grid-cols-5 gap-2 mt-2">
        {AGE.map(([k, l, c]) => (
          <li key={k} className="text-sm"><span className="inline-block w-3 h-3 mr-1 align-middle" style={{ background: c }} aria-hidden="true" />{t('dash.' + l)}<br /><strong>{money0(ageing[k])}</strong></li>
        ))}
      </ul>
    </div>
  );
}

export default function Dashboard() {
  const { t } = useApp();
  const { data: d, error, loading } = useLoad('/dashboard');
  if (loading && !d) return <Loading />;
  if (error) return <ErrorBox error={error} />;
  return (
    <div>
      <PageTitle actions={<Link to="/invoices/new" className="btn-primary">+ {t('nav.newInvoice')}</Link>}>{t('nav.dashboard')}</PageTitle>
      {d.firstShortfallWeek && <Notice tone="warn"><Link className="underline font-semibold" to="/forecast">{t('dash.shortfall', { w: d.firstShortfallWeek })}</Link></Notice>}
      <div className="flex flex-wrap gap-2 mb-4">
        <Link to="/followups" className="btn-secondary">☎ {t('dash.followupsToday', { n: d.followUpsToday })}</Link>
        {d.inboxOpen > 0 && <Link to="/inbox" className="btn-secondary">✉ {t('dash.inboxOpen', { n: d.inboxOpen })}</Link>}
      </div>
      <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-4 gap-3 mb-6">
        <Stat label={t('dash.toCollect')} tip={t('dash.toCollectTip')} value={money0(d.toCollectPaise)} />
        <Stat label={t('dash.overdue')} tip={t('dash.overdueTip')} value={money0(d.overduePaise)} tone="bad" />
        <Stat label={t('dash.thisWeek')} tip={t('dash.expectedTip')} value={money0(d.expectedThisWeekPaise)} />
        <Stat label={t('dash.thisMonth')} tip={t('dash.expectedTip')} value={money0(d.expectedThisMonthPaise)} />
        <Stat label={t('dash.bills')} tip={t('dash.billsTip')} value={money0(d.billsNext30Paise)} />
        <Stat label={t('dash.cash')} value={money0(d.cashInBankPaise)} />
        <Stat label={t('dash.lowest')} value={money0(d.lowestForecastPaise)} sub={d.lowestWeek ? t('dash.lowestWeek', { w: d.lowestWeek }) : ''}
          tone={d.lowestForecastPaise < d.safetyBufferPaise ? 'bad' : ''} />
      </div>
      <section className="card mb-6" aria-labelledby="age-h">
        <h2 id="age-h" className="h2"><Term tip={t('dash.ageingTip')}>{t('dash.ageing')}</Term></h2>
        <AgeingBar ageing={d.ageing} />
      </section>
      <section className="card" aria-labelledby="top-h">
        <div className="flex justify-between items-center"><h2 id="top-h" className="h2">{t('dash.top5')}</h2><Link className="underline font-semibold" to="/chase">{t('dash.seeAll')}</Link></div>
        <ol className="divide-y">
          {d.chaseTop5.map((c, i) => (
            <li key={c.invoiceId} className="py-3 flex gap-3">
              <span className="font-bold text-lg w-6">{i + 1}.</span>
              <div className="flex-1">
                <Link to={`/invoices/${c.invoiceId}`} className="font-semibold underline">{c.customerName} · {c.invoiceNumber}</Link>
                <div className="muted"><ChaseReason c={c} /></div>
              </div>
            </li>
          ))}
        </ol>
      </section>
    </div>
  );
}
