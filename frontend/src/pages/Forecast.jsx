import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../api/client.js';
import { useApp, useLoad } from '../lib/AppContext.jsx';
import { fmtDate, money0, toPaise } from '../lib/format.js';
import { ErrorBox, Loading, Notice, PageTitle } from '../components/ui.jsx';

function Chart({ weeks, buffer }) {
  const { t } = useApp();
  const vals = weeks.flatMap((w) => [w.closingPaise, w.openingPaise]).concat([buffer, 0]);
  const max = Math.max(...vals), min = Math.min(...vals);
  const H = 200, W = 640, pad = 30, bw = (W - pad) / weeks.length;
  const y = (v) => H - 20 - ((v - min) / (max - min || 1)) * (H - 40);
  return (
    <svg viewBox={`0 0 ${W} ${H}`} className="w-full h-auto" role="img" aria-label={weeks.map((w) => `${t('common.week')} ${w.week}: ${money0(w.closingPaise)}`).join('; ')}>
      <line x1={pad} x2={W} y1={y(buffer)} y2={y(buffer)} stroke="#b45309" strokeDasharray="6 4" />
      <text x={pad + 2} y={y(buffer) - 4} fontSize="11" fill="#78350f">{t('fc.buffer')}</text>
      <line x1={pad} x2={W} y1={y(0)} y2={y(0)} stroke="#6b7280" />
      {weeks.map((w, i) => (
        <g key={w.week}>
          <rect x={pad + i * bw + 4} width={bw - 8} y={Math.min(y(w.closingPaise), y(0))} height={Math.abs(y(w.closingPaise) - y(0))} fill={w.belowBuffer ? '#b91c1c' : '#0f6b4f'} />
          <text x={pad + i * bw + bw / 2} y={H - 4} fontSize="11" textAnchor="middle">{w.week}</text>
        </g>
      ))}
    </svg>
  );
}

export default function Forecast() {
  const { t, lang, version } = useApp();
  const custs = useLoad('/customers');
  const exps = useLoad('/expenses');
  const [w, setW] = useState({ late: false, lateCustomerId: '', big: false, bigAmt: '500000', bigDate: '', delay: false, delayedExpenseId: '', delayDays: '30' });
  const [fc, setFc] = useState(null);
  const [err, setErr] = useState(null);
  useEffect(() => {
    const body = {
      lateCustomerId: w.late && w.lateCustomerId ? Number(w.lateCustomerId) : null, lateDays: 30,
      bigOrderPaise: w.big ? toPaise(w.bigAmt) || 0 : null, bigOrderDate: w.big && w.bigDate ? w.bigDate : null,
      delayedExpenseId: w.delay && w.delayedExpenseId ? Number(w.delayedExpenseId) : null, expenseDelayDays: Number(w.delayDays) || 0,
    };
    api.post('/forecast', body).then(setFc).catch(setErr);
  }, [w, version]);
  if (!fc && !err) return <Loading />;
  if (err) return <ErrorBox error={err} />;
  const suppliers = (exps.data || []).filter((e) => e.category === 'SUPPLIER');
  return (
    <div>
      <PageTitle>{t('fc.title')}</PageTitle>
      <p className="muted mb-4">{t('fc.intro')}</p>
      {fc.firstShortfallWeek ? <Notice tone="warn">{t('fc.firstBelow', { w: fc.firstShortfallWeek, amt: money0(fc.shortfallPaise) })}</Notice> : <Notice tone="ok">{t('fc.allGood')}</Notice>}
      {fc.disputedExcludedPaise > 0 && <p className="text-sm muted">{t('fc.disputed', { amt: money0(fc.disputedExcludedPaise) })}</p>}
      <section className="card my-4"><Chart weeks={fc.weeks} buffer={fc.safetyBufferPaise} /></section>
      <section className="card mb-4">
        <h2 className="h2">{t('fc.whatIf')}</h2>
        <div className="grid md:grid-cols-3 gap-4">
          <fieldset>
            <label className="flex items-center gap-3 min-h-[48px] font-semibold"><input type="checkbox" className="w-6 h-6" checked={w.late} onChange={(e) => setW({ ...w, late: e.target.checked })} />{t('fc.lateCustomer')}</label>
            <label className="sr-only" htmlFor="wlc">{t('common.customer')}</label>
            <select id="wlc" className="input" disabled={!w.late} value={w.lateCustomerId} onChange={(e) => setW({ ...w, lateCustomerId: e.target.value })}><option value="">—</option>{(custs.data || []).filter((c) => c.owedPaise > 0).map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}</select>
          </fieldset>
          <fieldset>
            <label className="flex items-center gap-3 min-h-[48px] font-semibold"><input type="checkbox" className="w-6 h-6" checked={w.big} onChange={(e) => setW({ ...w, big: e.target.checked })} />{t('fc.bigOrder')}</label>
            <label className="label text-sm" htmlFor="wba">{t('fc.bigOrderAmt')}</label><input id="wba" className="input" inputMode="decimal" disabled={!w.big} value={w.bigAmt} onChange={(e) => setW({ ...w, bigAmt: e.target.value })} />
            <label className="label text-sm mt-1" htmlFor="wbd">{t('fc.bigOrderDate')}</label><input id="wbd" type="date" className="input" disabled={!w.big} value={w.bigDate} onChange={(e) => setW({ ...w, bigDate: e.target.value })} />
          </fieldset>
          <fieldset>
            <label className="flex items-center gap-3 min-h-[48px] font-semibold"><input type="checkbox" className="w-6 h-6" checked={w.delay} onChange={(e) => setW({ ...w, delay: e.target.checked })} />{t('fc.supplierDelay')}</label>
            <label className="sr-only" htmlFor="wse">{t('common.supplier')}</label>
            <select id="wse" className="input" disabled={!w.delay} value={w.delayedExpenseId} onChange={(e) => setW({ ...w, delayedExpenseId: e.target.value })}><option value="">—</option>{suppliers.map((e) => <option key={e.id} value={e.id}>{e.name} · {fmtDate(e.dueDate, lang)} · {money0(e.amountPaise)}</option>)}</select>
            <label className="label text-sm mt-1" htmlFor="wdd">{t('fc.delayDays')}</label><input id="wdd" type="number" className="input" disabled={!w.delay} value={w.delayDays} onChange={(e) => setW({ ...w, delayDays: e.target.value })} />
          </fieldset>
        </div>
      </section>
      {fc.coverGap.length > 0 && (
        <section className="card mb-4">
          <h2 className="h2">{t('fc.cover')}</h2>
          <ul className="divide-y">{fc.coverGap.map((c) => <li key={c.invoiceId} className="py-2"><Link className="underline font-semibold" to={`/invoices/${c.invoiceId}`}>{c.invoiceNumber} · {c.customerName}</Link> — {money0(c.outstandingPaise)} ({fmtDate(c.expectedDate, lang)})</li>)}</ul>
        </section>
      )}
      <section className="card overflow-x-auto">
        <table className="table text-sm">
          <thead><tr><th>{t('common.week')}</th><th className="text-right">{t('fc.opening')}</th><th className="text-right">{t('fc.in')}</th><th className="text-right">{t('fc.out')}</th><th className="text-right">{t('fc.closing')}</th></tr></thead>
          <tbody>
            {fc.weeks.map((wk) => (
              <tr key={wk.week} className={wk.belowBuffer ? 'bg-red-50' : ''}>
                <td><strong>{wk.week}</strong> <span className="muted">{fmtDate(wk.start, lang)}–{fmtDate(wk.end, lang)}</span>{wk.belowBuffer && <span className="text-red-800 font-semibold"> · ! {t('fc.buffer')}</span>}
                  <details className="text-xs"><summary className="cursor-pointer underline">{wk.ins.length + wk.outs.length}</summary>
                    <ul>{wk.ins.map((f, i) => <li key={'i' + i}>+ {f.label} {money0(f.amountPaise)}</li>)}{wk.outs.map((f, i) => <li key={'o' + i}>− {f.label} {money0(f.amountPaise)}</li>)}</ul></details></td>
                <td className="text-right">{money0(wk.openingPaise)}</td><td className="text-right text-green-800">+{money0(wk.inPaise)}</td><td className="text-right text-red-800">−{money0(wk.outPaise)}</td><td className="text-right font-bold">{money0(wk.closingPaise)}</td>
              </tr>
            ))}
            <tr><td className="font-bold">{t('fc.totals')}</td><td className="text-right">{money0(fc.cashInBankPaise)}</td><td className="text-right">+{money0(fc.totalInPaise)}</td><td className="text-right">−{money0(fc.totalOutPaise)}</td><td className="text-right font-bold">{money0(fc.cashInBankPaise + fc.totalInPaise - fc.totalOutPaise)}</td></tr>
          </tbody>
        </table>
      </section>
    </div>
  );
}
