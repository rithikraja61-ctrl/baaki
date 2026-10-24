import { useState } from 'react';
import { api } from '../api/client.js';
import { useApp, useLoad } from '../lib/AppContext.jsx';
import { fmtDate, money, paiseToInput, toPaise } from '../lib/format.js';
import { ErrorBox, Loading, Notice, PageTitle } from '../components/ui.jsx';
import sampleCsv from '../data/bank-statement.csv?raw';

function Pending({ p, invoices, onDone }) {
  const { t, lang } = useApp();
  const s = p.suggestion;
  const [rows, setRows] = useState(() => s.allocations.map((a) => ({ invoiceId: String(a.invoiceId), amount: paiseToInput(a.amountPaise) })));
  const [err, setErr] = useState(null);
  const total = rows.reduce((x, r) => x + (toPaise(r.amount) || 0), 0);
  const left = p.amountPaise - total;
  const go = async (fn) => { setErr(null); try { await fn(); onDone(); } catch (e) { setErr(e); } };
  const tone = { EXACT: 'border-green-600', MULTIPLE: 'border-green-600', PART: 'border-amber-500', TDS: 'border-amber-500', OVERPAID: 'border-purple-500', UNMATCHED: 'border-red-600' }[s.kind];
  return (
    <li className={`card border-l-8 ${tone}`}>
      <div className="flex flex-wrap justify-between gap-2">
        <div><div className="font-bold text-lg">{money(p.amountPaise)}</div><div className="text-sm muted">{fmtDate(p.date, lang)} · {p.reference}</div><div className="break-words">{p.description}</div></div>
        <div className="text-right"><div className="font-semibold">{t('pmt.kind.' + s.kind)}</div><div className="text-sm">{t('pmt.confidence')}: {t('pmt.conf.' + s.confidence)}</div></div>
      </div>
      <ul className="list-disc ml-6 mt-2 text-sm">{s.reasons.map((r, i) => <li key={i}>{r}</li>)}</ul>
      {s.kind === 'TDS' && <Notice tone="warn">{t('pmt.tdsHint')} ({s.tdsPercent}%)</Notice>}
      <ErrorBox error={err} />
      <div className="mt-3 space-y-2">
        {rows.map((r, i) => (
          <div key={i} className="flex flex-wrap gap-2 items-end">
            <div className="flex-1 min-w-[200px]"><label className="label text-sm" htmlFor={`a${p.id}-${i}`}>{t('pmt.allocate')}</label>
              <select id={`a${p.id}-${i}`} className="input" value={r.invoiceId} onChange={(e) => setRows(rows.map((x, j) => (j === i ? { ...x, invoiceId: e.target.value } : x)))}>
                <option value="">—</option>
                {invoices.map((inv) => <option key={inv.id} value={inv.id}>{inv.invoiceNumber} · {inv.customerName} · {money(inv.outstandingPaise)}</option>)}
              </select></div>
            <div><label className="label text-sm" htmlFor={`m${p.id}-${i}`}>₹</label><input id={`m${p.id}-${i}`} inputMode="decimal" className="input w-36" value={r.amount} onChange={(e) => setRows(rows.map((x, j) => (j === i ? { ...x, amount: e.target.value } : x)))} /></div>
            <button className="btn-danger" aria-label="Remove" onClick={() => setRows(rows.filter((_, j) => j !== i))}>✕</button>
          </div>
        ))}
        <button className="underline min-h-[48px]" onClick={() => setRows([...rows, { invoiceId: '', amount: paiseToInput(Math.max(0, left)) }])}>+ {t('pmt.addLine')}</button>
        {left !== 0 && <p className={left < 0 ? 'text-red-800 font-semibold' : ''}>{t('pmt.unallocated')}: {money(left)}</p>}
      </div>
      <div className="flex flex-wrap gap-2 mt-3">
        <button className="btn-primary" disabled={left < 0} onClick={() => go(() => api.post(`/payments/${p.id}/confirm`, {
          allocations: rows.filter((r) => r.invoiceId).map((r) => ({ invoiceId: Number(r.invoiceId), amountPaise: toPaise(r.amount) })),
          note: s.kind === 'TDS' ? `TDS ${s.tdsPercent}% short: ${money(s.shortfallPaise)} — certificate requested` : null,
        }))}>✓ {t('pmt.confirm')}</button>
        <button className="btn-secondary" onClick={() => go(() => api.post(`/payments/${p.id}/ignore`, { note: 'Not a customer payment' }))}>{t('pmt.ignore')}</button>
      </div>
    </li>
  );
}

export default function Payments() {
  const { t, lang, today } = useApp();
  const pays = useLoad('/payments');
  const invs = useLoad('/invoices');
  const [msg, setMsg] = useState(null);
  const [err, setErr] = useState(null);
  const [m, setM] = useState({ date: '', amount: '', reference: '', description: '' });
  const reload = () => { pays.reload(); invs.reload(); };
  const doImport = async (csv) => { setErr(null); try { const r = await api.postCsv('/payments/import', csv); setMsg(t('pmt.imported', { n: r.imported, d: r.skippedDuplicates, x: r.debitsIgnored })); reload(); } catch (e) { setErr(e); } };
  if (pays.loading && !pays.data) return <Loading />;
  const open = (invs.data || []).filter((i) => i.outstandingPaise > 0 && i.status !== 'DRAFT');
  const pending = (pays.data || []).filter((p) => p.status === 'PENDING');
  const done = (pays.data || []).filter((p) => p.status !== 'PENDING');
  return (
    <div>
      <PageTitle>{t('pmt.title')}</PageTitle>
      <p className="muted mb-4">{t('pmt.intro')}</p>
      <ErrorBox error={err || pays.error} />
      {msg && <Notice tone="ok">{msg}</Notice>}
      <section className="card mb-4 grid md:grid-cols-2 gap-4">
        <div>
          <h2 className="h2">{t('pmt.import')}</h2>
          <button className="btn-primary mb-3" onClick={() => doImport(sampleCsv)}>{t('pmt.importSample')}</button>
          <label className="label" htmlFor="csv">{t('pmt.chooseFile')}</label>
          <input id="csv" type="file" accept=".csv,text/csv" className="block min-h-[48px]" onChange={async (e) => { const f = e.target.files?.[0]; if (f) doImport(await f.text()); e.target.value = ''; }} />
        </div>
        <form onSubmit={async (e) => { e.preventDefault(); setErr(null); try { await api.post('/payments', { date: m.date || today, amountPaise: toPaise(m.amount), reference: m.reference, description: m.description }); setM({ date: '', amount: '', reference: '', description: '' }); reload(); } catch (x) { setErr(x); } }}>
          <h2 className="h2">{t('pmt.addManual')}</h2>
          <div className="grid grid-cols-2 gap-2">
            <div><label className="label" htmlFor="md">{t('common.date')}</label><input id="md" type="date" className="input" value={m.date} onChange={(e) => setM({ ...m, date: e.target.value })} /></div>
            <div><label className="label" htmlFor="ma">{t('common.amount')} (₹)</label><input id="ma" inputMode="decimal" className="input" required value={m.amount} onChange={(e) => setM({ ...m, amount: e.target.value })} /></div>
            <div><label className="label" htmlFor="mr">{t('pmt.reference')}</label><input id="mr" className="input" value={m.reference} onChange={(e) => setM({ ...m, reference: e.target.value })} /></div>
            <div><label className="label" htmlFor="mde">{t('pmt.description')}</label><input id="mde" className="input" value={m.description} onChange={(e) => setM({ ...m, description: e.target.value })} /></div>
          </div>
          <button className="btn-secondary mt-2">{t('common.save')}</button>
        </form>
      </section>
      <h2 className="h2">{t('pmt.pending')} ({pending.length})</h2>
      <ul className="space-y-3 mb-6">{pending.map((p) => <Pending key={p.id} p={p} invoices={open} onDone={reload} />)}</ul>
      {done.length > 0 && (
        <section className="card overflow-x-auto"><table className="table text-sm">
          <thead><tr><th>{t('common.date')}</th><th>{t('pmt.reference')}</th><th>{t('pmt.description')}</th><th className="text-right">{t('common.amount')}</th><th>{t('inv.status')}</th></tr></thead>
          <tbody>{done.map((p) => <tr key={p.id}><td>{fmtDate(p.date, lang)}</td><td>{p.reference}</td><td>{p.description}{p.note ? <div className="muted">{p.note}</div> : null}</td><td className="text-right">{money(p.amountPaise)}</td><td>{p.status === 'CONFIRMED' ? t('pmt.confirmed') : t('pmt.ignored')}</td></tr>)}</tbody>
        </table></section>
      )}
    </div>
  );
}
