import { useEffect, useMemo, useRef, useState } from 'react';
import { useNavigate, useParams, useSearchParams } from 'react-router-dom';
import { api } from '../api/client.js';
import { useApp, useLoad } from '../lib/AppContext.jsx';
import { addDays, money, paiseToInput, rateLabel, rupeesWords, toPaise } from '../lib/format.js';
import { ErrorBox, Loading, Notice, PageTitle } from '../components/ui.jsx';
import InvoiceDocument from '../components/InvoiceDocument.jsx';
import { CustomerForm } from './Customers.jsx';
import cfg from '../data/config.js';
import { ScanReview, ScanUpload } from '../components/ScanInvoice.jsx';

const emptyLine = () => ({ description: '', hsnSac: '', quantity: '1', unit: 'pcs', rate: '', discount: '', gstRateBp: 1800 });

export default function NewInvoice() {
  const { id } = useParams();
  const [sp] = useSearchParams();
  const nav = useNavigate();
  const { t, config, profile, today } = useApp();
  const custs = useLoad('/customers');
  const existing = useLoad(id ? `/invoices/${id}` : null);
  const [customerId, setCustomerId] = useState(sp.get('customerId') || '');
  const [invoiceDate, setInvoiceDate] = useState('');
  const [placeOfSupply, setPlace] = useState('');
  const [reverseCharge, setRC] = useState(false);
  const [deliveryAddress, setDelivery] = useState('');
  const [notes, setNotes] = useState('');
  const [lines, setLines] = useState([emptyLine()]);
  const [calc, setCalc] = useState(null);
  const [err, setErr] = useState(null);
  const [adding, setAdding] = useState(false);
  const [preview, setPreview] = useState(false);
  const [busy, setBusy] = useState(false);
  // Reading an existing paper/PDF invoice: values are only filled in, never saved until the owner reviews them.
  const [scan, setScan] = useState(null);
  const [unsure, setUnsure] = useState(new Set());
  const [existingNumber, setExistingNumber] = useState('');
  const [printedDue, setPrintedDue] = useState('');
  const [reviewed, setReviewed] = useState(false);
  const scanPlace = useRef(false);
  const hl = (f) => (unsure.has(f) ? ' bg-amber-50 ring-2 ring-amber-600' : '');
  const touch = (f) => setUnsure((u) => { if (!u.has(f)) return u; const n = new Set(u); n.delete(f); return n; });
  const applyScan = (r) => {
    // Map AI field names and check results onto the form fields the owner can see and correct.
    const toField = (f) => {
      if (/^items\.\d+\.(description|hsnSac|quantity|unit|rate|discount|gstRate)$/.test(f)) return f;
      if (/^(totals\.|items$|taxableTotal|cgst|sgst|igst|roundOff|total)/.test(f)) return 'items';
      if (f === 'customerName' || f === 'customer') return 'customer';
      if (f === 'customerState' || f === 'placeOfSupply') return 'placeOfSupply';
      if (f === 'sellerGstin' || f === 'customerGstin') return 'customerGstin';
      if (['invoiceNumber', 'invoiceDate'].includes(f)) return f;
      if (f === 'dueDate') return r.printedDueDate ? 'dueDate' : null;
      return null;
    };
    const u = new Set();
    for (const f of [...(r.uncertain || []), ...(r.flags || []).map((x) => x.field)]) { const m = toField(f); if (m) u.add(m); }
    if (!r.matchedCustomerId) u.add('customer');
    if (r.matchedCustomerId) setCustomerId(String(r.matchedCustomerId)); else setCustomerId('');
    if (r.invoiceDate) setInvoiceDate(r.invoiceDate);
    if (r.placeOfSupply) { scanPlace.current = true; setPlace(r.placeOfSupply); }
    setExistingNumber(r.invoiceNumber || '');
    setPrintedDue(r.printedDueDate || '');
    setLines((r.lines || []).length ? r.lines.map((l, i) => {
      const unit = cfg.units.includes((l.unit || '').toLowerCase()) ? l.unit.toLowerCase() : 'pcs';
      if (l.unit && unit !== l.unit.toLowerCase()) u.add(`items.${i}.unit`);
      return { description: l.description || '', hsnSac: l.hsnSac || '', quantity: String(Number(l.quantity || 1)), unit, rate: l.ratePaise == null ? '' : paiseToInput(l.ratePaise), discount: l.discountPaise ? paiseToInput(l.discountPaise) : '', gstRateBp: l.gstRateBp };
    }) : [emptyLine()]);
    setUnsure(u); setScan(r); setReviewed(false);
    if (!r.matchedCustomerId && r.customerName) setAdding(true);
  };
  const discardScan = () => { setScan(null); setUnsure(new Set()); setExistingNumber(''); setPrintedDue(''); setReviewed(false); setAdding(false); setLines([emptyLine()]); setCustomerId(''); };

  useEffect(() => { if (today && !invoiceDate && !id) setInvoiceDate(today); }, [today, invoiceDate, id]);
  useEffect(() => {
    const d = existing.data; if (!d) return;
    const i = d.invoice;
    setCustomerId(String(i.customerId)); setInvoiceDate(i.invoiceDate); setPlace(i.placeOfSupply); setRC(i.reverseCharge);
    setDelivery(i.deliveryAddress || ''); setNotes(i.notes || '');
    setLines(d.lines.map((l) => ({ description: l.description, hsnSac: l.hsnSac, quantity: String(Number(l.quantity)), unit: l.unit, rate: paiseToInput(l.ratePaise), discount: l.discountPaise ? paiseToInput(l.discountPaise) : '', gstRateBp: l.gstRateBp })));
  }, [existing.data]);

  const customer = (custs.data || []).find((c) => String(c.id) === String(customerId));
  useEffect(() => { if (scanPlace.current) { scanPlace.current = false; return; } if (customer && !id) setPlace(customer.stateCode); }, [customerId]); // eslint-disable-line
  const creditDue = customer && invoiceDate ? addDays(invoiceDate, customer.creditDays) : '';
  const dueDate = scan && printedDue ? printedDue : creditDue;
  const rates = cfg.gstRatesBp; // GST slabs come from src/data/config.js (the backend checks the same list)

  const lineErrors = lines.map((l) => ({ rate: Number.isNaN(toPaise(l.rate)), discount: Number.isNaN(toPaise(l.discount)), qty: !(Number(l.quantity) > 0) }));
  const payload = useMemo(() => ({
    customerId: customerId ? Number(customerId) : null, invoiceDate, placeOfSupply, reverseCharge, deliveryAddress, notes,
    existingNumber: scan ? existingNumber.trim() || null : null, dueDate: scan && printedDue ? printedDue : null,
    lines: lines.map((l) => ({ description: l.description, hsnSac: l.hsnSac, quantity: l.quantity, unit: l.unit, ratePaise: toPaise(l.rate) || 0, discountPaise: toPaise(l.discount) || 0, gstRateBp: Number(l.gstRateBp) })),
  }), [customerId, invoiceDate, placeOfSupply, reverseCharge, deliveryAddress, notes, lines, scan, existingNumber, printedDue]);

  // Live totals from the backend so the maths is identical to saving.
  useEffect(() => {
    if (!customerId || lineErrors.some((e) => e.rate || e.discount || e.qty)) { setCalc(null); return; }
    const h = setTimeout(() => api.post('/invoices/preview', payload).then((r) => { setCalc(r); setErr(null); }).catch((e) => { setCalc(null); setErr(e); }), 250);
    return () => clearTimeout(h);
  }, [payload]); // eslint-disable-line

  const setLine = (i, k, v) => { touch(`items.${i}.${k === 'gstRateBp' ? 'gstRate' : k}`); setLines((ls) => ls.map((l, j) => (j === i ? { ...l, [k]: v } : l))); };
  const removeLine = (i) => {
    setLines((ls) => ls.filter((_, j) => j !== i));
    setUnsure((u) => new Set([...u].flatMap((f) => {
      const m = f.match(/^items\.(\d+)\.(.+)$/);
      if (!m) return [f];
      const k = Number(m[1]);
      return k === i ? [] : [k > i ? `items.${k - 1}.${m[2]}` : f];
    })));
  };
  const blocked = scan && !reviewed;
  const save = async (send) => {
    if (blocked) { setErr(new Error(t('scan.mustReview'))); return; }
    setBusy(true); setErr(null);
    try {
      const body = { ...payload, send };
      const r = id ? await api.put(`/invoices/${id}`, body) : await api.post('/invoices', body);
      if (id && send && r.invoice.status === 'DRAFT') await api.post(`/invoices/${id}/send`);
      nav(`/invoices/${r.invoice.id}`);
    } catch (e) { setErr(e); } finally { setBusy(false); }
  };

  if (custs.loading && !custs.data) return <Loading />;
  const reg = profile?.registrationType;
  const modeText = reg === 'COMPOSITION' ? t('newInv.noGstComposition') : reg === 'UNREGISTERED' ? t('newInv.noGstUnreg')
    : calc ? (calc.intraState ? t('newInv.sameState') : t('newInv.otherState')) : '';
  const docType = reg === 'COMPOSITION' ? 'BILL_OF_SUPPLY' : reg === 'UNREGISTERED' ? 'INVOICE' : 'TAX_INVOICE';
  const previewDoc = calc && customer && profile ? {
    business: profile, customer: { ...customer }, intraState: calc.intraState, docTitle: { TAX_INVOICE: 'Tax Invoice', BILL_OF_SUPPLY: 'Bill of Supply', INVOICE: 'Invoice' }[docType],
    amountInWords: rupeesWords(calc.totalPaise), eInvoiceRequired: profile.turnoverPaise > (config?.eInvoiceThresholdPaise || 5e9),
    placeOfSupplyName: config?.states?.[placeOfSupply],
    invoice: { invoiceNumber: existing.data?.invoice.invoiceNumber, invoiceDate, dueDate, docType, placeOfSupply, reverseCharge, deliveryAddress, notes,
      taxablePaise: calc.taxablePaise, cgstPaise: calc.cgstPaise, sgstPaise: calc.sgstPaise, igstPaise: calc.igstPaise, roundOffPaise: calc.roundOffPaise, totalPaise: calc.totalPaise },
    lines: payload.lines.map((l, i) => ({ ...l, ...calc.lines[i] })),
  } : null;

  if (preview && previewDoc) return (
    <div>
      <div className="no-print flex flex-wrap gap-2 mb-4">
        <button className="btn-secondary" onClick={() => setPreview(false)}>← {t('common.back')}</button>
        <button className="btn-secondary" onClick={() => window.print()}>🖨 {t('common.print')}</button>
        <button className="btn-secondary" disabled={busy || blocked} onClick={() => save(false)}>{t('newInv.saveDraft')}</button>
        <button className="btn-primary" disabled={busy || blocked} onClick={() => save(true)}>{t('newInv.send')}</button>
      </div>
      <InvoiceDocument d={previewDoc} states={config?.states} />
    </div>
  );

  return (
    <div>
      <PageTitle>{id ? t('newInv.editTitle') : t('newInv.title')}</PageTitle>
      <ErrorBox error={err} />
      {!id && !scan && <ScanUpload onResult={applyScan} />}
      {scan && <ScanReview scan={scan} calc={calc} pending={unsure.size} onDiscard={discardScan} />}
      <section className="card mb-4">
        <div className="grid sm:grid-cols-2 gap-3">
          <div>
            <label className="label" htmlFor="cust">{t('newInv.customer')}</label>
            <select id="cust" className={`input${hl('customer')}`} value={customerId} onChange={(e) => { touch('customer'); setCustomerId(e.target.value); }} required>
              <option value="">{t('newInv.pick')}</option>
              {(custs.data || []).map((c) => <option key={c.id} value={c.id}>{c.name}{c.gstin ? '' : ` (${t('cust.unregistered')})`}</option>)}
            </select>
            <button type="button" className="underline mt-2 min-h-[48px]" onClick={() => setAdding(!adding)}>+ {t('newInv.addCustomer')}</button>
          </div>
          <div className="grid grid-cols-2 gap-2">
            <div><label className="label" htmlFor="idate">{t('newInv.invoiceDate')}</label><input id="idate" type="date" className={`input${hl('invoiceDate')}`} value={invoiceDate} onChange={(e) => { touch('invoiceDate'); setInvoiceDate(e.target.value); }} /></div>
            {scan && printedDue
              ? <div><label className="label" htmlFor="ddate">{t('scan.printedDue')}</label><input id="ddate" type="date" className={`input${hl('dueDate')}`} value={printedDue} onChange={(e) => { touch('dueDate'); setPrintedDue(e.target.value); }} /></div>
              : <div><label className="label" htmlFor="ddate">{t('newInv.dueDate')}</label><input id="ddate" className="input bg-gray-100" value={dueDate} readOnly /></div>}
          </div>
        </div>
        {scan && (
          <div className="grid sm:grid-cols-2 gap-3 mt-3">
            <div>
              <label className="label" htmlFor="exno">{t('scan.existingNumber')}</label>
              <input id="exno" className={`input${hl('invoiceNumber')}`} value={existingNumber} maxLength={16} onChange={(e) => { touch('invoiceNumber'); setExistingNumber(e.target.value); }} />
              <p className="text-sm muted mt-1">{t('scan.existingNumberHelp')}</p>
            </div>
            <div className={`rounded-lg p-2 break-words${hl('customerGstin')}`}>
              <p className="label">{t('scan.readCustomer')}</p>
              <p>{scan.customerName || '—'}</p>
              <p className="text-sm">GSTIN {scan.customerGstin || '—'}{scan.customerGstin ? (scan.customerGstinValid ? ` · ${t('scan.gstinOk')}` : ` · ${t('scan.gstinBad')}`) : ''}</p>
              {scan.sellerGstin && <p className="text-sm">{t('scan.sellerGstin')}: {scan.sellerGstin}</p>}
              {unsure.has('customerGstin') && <button type="button" className="underline min-h-[48px]" onClick={() => touch('customerGstin')}>{t('scan.markChecked')}</button>}
            </div>
          </div>
        )}
        {adding && <div className="border rounded-lg p-3 mt-3"><CustomerForm initial={scan && !scan.matchedCustomerId ? { name: scan.customerName || '', contactPerson: '', phone: '', email: '', address: scan.customerAddress || '', city: '', stateCode: scan.placeOfSupply || '27', gstin: scan.customerGstinValid ? scan.customerGstin : '', creditDays: 30 } : undefined} onSaved={(c) => { custs.reload(); touch('customer'); setCustomerId(String(c.id)); setAdding(false); }} onCancel={() => setAdding(false)} /></div>}
        {customer && <p className="mt-2 muted">{customer.gstin ? `GSTIN ${customer.gstin} · ` : `${t('cust.unregistered')} · `}{customer.stateName} ({customer.stateCode}) · {t('cust.creditDays')}: {customer.creditDays}</p>}
        <div className="grid sm:grid-cols-2 gap-3 mt-3">
          <div>
            <label className="label" htmlFor="pos">{t('newInv.placeOfSupply')}</label>
            <select id="pos" className={`input${hl('placeOfSupply')}`} value={placeOfSupply} onChange={(e) => { touch('placeOfSupply'); setPlace(e.target.value); }}>
              {Object.entries(config?.states || {}).map(([code, name]) => <option key={code} value={code}>{name} ({code})</option>)}
            </select>
          </div>
          <label className="flex items-center gap-3 min-h-[48px] mt-6"><input type="checkbox" className="w-6 h-6" checked={reverseCharge} onChange={(e) => setRC(e.target.checked)} />{t('newInv.reverseCharge')}</label>
          <div><label className="label" htmlFor="del">{t('newInv.deliveryAddress')}</label><input id="del" className="input" value={deliveryAddress} onChange={(e) => setDelivery(e.target.value)} /></div>
          <div><label className="label" htmlFor="notes">{t('newInv.notes')}</label><input id="notes" className="input" value={notes} onChange={(e) => setNotes(e.target.value)} /></div>
        </div>
      </section>

      <section className="card mb-4">
        <h2 className="h2">{t('newInv.items')}</h2>
        <p className="text-sm muted mb-2">{t('newInv.hsnHelp')}</p>
        <div className="space-y-4">
          {lines.map((l, i) => (
            <fieldset key={i} className="border rounded-lg p-3">
              <legend className="font-semibold px-1">#{i + 1}</legend>
              <div className="grid grid-cols-2 md:grid-cols-12 gap-2">
                <div className="col-span-2 md:col-span-4"><label className="label" htmlFor={`d${i}`}>{t('newInv.description')}</label><input id={`d${i}`} className={`input${hl(`items.${i}.description`)}`} value={l.description} onChange={(e) => setLine(i, 'description', e.target.value)} /></div>
                <div className="md:col-span-2"><label className="label" htmlFor={`h${i}`}>{t('newInv.hsn')}</label><input id={`h${i}`} inputMode="numeric" className={`input${hl(`items.${i}.hsnSac`)}`} value={l.hsnSac} onChange={(e) => setLine(i, 'hsnSac', e.target.value.replace(/\D/g, ''))} maxLength={8} /></div>
                <div className="md:col-span-1"><label className="label" htmlFor={`q${i}`}>{t('newInv.qty')}</label><input id={`q${i}`} inputMode="decimal" className={`input ${lineErrors[i].qty ? 'border-red-700' : ''}${hl(`items.${i}.quantity`)}`} value={l.quantity} onChange={(e) => setLine(i, 'quantity', e.target.value)} aria-invalid={lineErrors[i].qty} /></div>
                <div className="md:col-span-1"><label className="label" htmlFor={`u${i}`}>{t('newInv.unit')}</label><select id={`u${i}`} className={`input${hl(`items.${i}.unit`)}`} value={l.unit} onChange={(e) => setLine(i, 'unit', e.target.value)}>{cfg.units.map((u) => <option key={u}>{u}</option>)}</select></div>
                <div className="md:col-span-2"><label className="label" htmlFor={`r${i}`}>{t('newInv.rate')}</label><input id={`r${i}`} inputMode="decimal" className={`input ${lineErrors[i].rate ? 'border-red-700' : ''}${hl(`items.${i}.rate`)}`} value={l.rate} onChange={(e) => setLine(i, 'rate', e.target.value)} aria-invalid={lineErrors[i].rate} /></div>
                <div className="md:col-span-1"><label className="label" htmlFor={`x${i}`}>{t('newInv.discount')}</label><input id={`x${i}`} inputMode="decimal" className={`input${hl(`items.${i}.discount`)}`} value={l.discount} onChange={(e) => setLine(i, 'discount', e.target.value)} /></div>
                <div className="md:col-span-1"><label className="label" htmlFor={`g${i}`}>{t('newInv.gst')}</label><select id={`g${i}`} className={`input${hl(`items.${i}.gstRate`)}`} value={l.gstRateBp} onChange={(e) => setLine(i, 'gstRateBp', Number(e.target.value))}>{rates.map((r) => <option key={r} value={r}>{rateLabel(r)}</option>)}</select></div>
              </div>
              <div className="flex flex-wrap justify-between items-center mt-2 gap-2">
                <span className="muted">{calc?.lines[i] ? `${t('newInv.taxable')}: ${money(calc.lines[i].taxablePaise)} · ${t('common.total')}: ${money(calc.lines[i].totalPaise)}` : ''}</span>
                {lines.length > 1 && <button type="button" className="btn-danger" onClick={() => removeLine(i)} aria-label={t('newInv.removeItem', { n: i + 1 })}>✕</button>}
              </div>
            </fieldset>
          ))}
        </div>
        <button type="button" className="btn-secondary mt-3" onClick={() => setLines([...lines, emptyLine()])}>+ {t('newInv.addItem')}</button>
      </section>

      <section className={`card mb-4${unsure.has('items') ? ' ring-2 ring-amber-600' : ''}`} aria-live="polite">
        {scan && unsure.has('items') && <p className="mb-2">{t('scan.totalsDiffer')} <button type="button" className="underline min-h-[48px]" onClick={() => touch('items')}>{t('scan.markChecked')}</button></p>}
        {modeText && <p className="font-semibold mb-2">{modeText}</p>}
        {calc ? (
          <dl className="grid grid-cols-2 gap-1 max-w-md ml-auto">
            <dt>{t('newInv.taxable')}</dt><dd className="text-right">{money(calc.taxablePaise)}</dd>
            {calc.cgstPaise + calc.sgstPaise > 0 && <><dt>{t('newInv.cgst')}</dt><dd className="text-right">{money(calc.cgstPaise)}</dd><dt>{t('newInv.sgst')}</dt><dd className="text-right">{money(calc.sgstPaise)}</dd></>}
            {calc.igstPaise > 0 && <><dt>{t('newInv.igst')}</dt><dd className="text-right">{money(calc.igstPaise)}</dd></>}
            <dt>{t('newInv.roundOff')}</dt><dd className="text-right">{money(calc.roundOffPaise)}</dd>
            <dt className="font-bold text-lg">{t('newInv.grandTotal')}</dt><dd className="text-right font-bold text-lg">{money(calc.totalPaise)}</dd>
            <dt className="col-span-2 text-sm muted">{t('newInv.inWords')}: {rupeesWords(calc.totalPaise)}</dt>
          </dl>
        ) : <p className="muted">{t('newInv.pick')}</p>}
      </section>
      {profile && profile.turnoverPaise > (config?.eInvoiceThresholdPaise || 5e9) && <Notice tone="warn">{t('inv.eInvoiceNote')}</Notice>}
      {scan && (
        <label className="flex items-start gap-3 min-h-[48px] mb-3 font-semibold">
          <input type="checkbox" className="w-6 h-6 mt-1 shrink-0" checked={reviewed} disabled={unsure.size > 0} onChange={(e) => setReviewed(e.target.checked)} />
          <span>{unsure.size > 0 ? t('scan.checkFirst', { n: unsure.size }) : t('scan.confirmReviewed')}</span>
        </label>
      )}
      <div className="flex flex-wrap gap-2">
        <button className="btn-secondary" disabled={!calc} onClick={() => setPreview(true)}>{t('newInv.preview')}</button>
        <button className="btn-secondary" disabled={!calc || busy || blocked} onClick={() => save(false)}>{t('newInv.saveDraft')}</button>
        <button className="btn-primary" disabled={!calc || busy || blocked} onClick={() => save(true)}>{t('newInv.send')}</button>
      </div>
    </div>
  );
}
