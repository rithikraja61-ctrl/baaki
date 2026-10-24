import { fmtDate, money, rateLabel, upiUri } from '../lib/format.js';
import { Bi, QR } from './ui.jsx';

const STATES = { '24': 'Gujarat', '27': 'Maharashtra' };

/**
 * Print-ready A4 Tax Invoice / Bill of Supply / Invoice. English by default (statutory document).
 * When the owner picks a second language in Settings, it is printed in small text under each heading.
 * Works for saved invoices (d = API detail) and live previews (same shape).
 */
export default function InvoiceDocument({ d, states }) {
  const b = d.business, c = d.customer, inv = d.invoice;
  const second = b.docSecondLanguage || null;
  const H = (props) => <Bi second={second} {...props} />;
  const stateName = (code) => (states && states[code]) || STATES[code] || code;
  const tax = inv.docType === 'TAX_INVOICE';
  const intra = d.intraState;
  const owed = d.summary ? d.summary.outstandingPaise || inv.totalPaise : inv.totalPaise;
  return (
    <article className="a4 shadow" aria-label={d.docTitle} lang="en">
      <header className="flex flex-wrap justify-between items-start gap-4 border-b-2 border-black pb-2">
        <div className="min-w-0">
          {b.logoDataUrl && <img src={b.logoDataUrl} alt="" className="h-12 mb-1" />}
          <div className="text-xl font-bold break-words">{b.name}</div>
          <div className="break-words">{b.address}</div>
          {b.gstin && <div>GSTIN: <strong>{b.gstin}</strong></div>}
          <div><H k="doc.state" />: {stateName(b.stateCode)} ({b.stateCode})</div>
          {b.phone && <div className="break-words"><H k="doc.phone" />: {b.phone} · {b.email}</div>}
        </div>
        <div className="text-right min-w-0">
          <div className="text-2xl font-bold uppercase"><H k={`doc.${inv.docType || 'TAX_INVOICE'}`} /></div>
          {inv.docType === 'BILL_OF_SUPPLY' && <div className="font-semibold"><H k="doc.composition" /></div>}
          <div><H k="doc.number" />: <strong>{inv.invoiceNumber || 'DRAFT'}</strong></div>
          <div><H k="doc.date" />: {fmtDate(inv.invoiceDate)}</div>
          <div><H k="doc.due" />: {fmtDate(inv.dueDate)}</div>
        </div>
      </header>
      {d.eInvoiceRequired && <p className="border p-1 mt-2 text-sm"><H k="doc.eInvoice" /></p>}
      <section className="grid sm:grid-cols-2 gap-4 mt-3">
        <div className="min-w-0">
          <div className="font-bold"><H k="doc.billTo" /></div>
          <div className="font-semibold break-words">{c.name}</div>
          <div className="break-words">{c.address}</div>
          {c.gstin ? <div>GSTIN: {c.gstin}</div> : <div><H k="doc.unregistered" /></div>}
          <div><H k="doc.state" />: {stateName(c.stateCode)} ({c.stateCode})</div>
        </div>
        <div className="min-w-0">
          <div><H k="doc.pos" />: <strong>{d.placeOfSupplyName || stateName(inv.placeOfSupply)} ({inv.placeOfSupply})</strong></div>
          {tax && <div><H k="doc.reverse" />: <strong>{inv.reverseCharge ? 'Yes' : 'No'}</strong></div>}
          {inv.deliveryAddress && <div className="break-words"><span className="font-bold"><H k="doc.delivery" />:</span> {inv.deliveryAddress}</div>}
        </div>
      </section>
      <div className="overflow-x-auto"><table className="mt-3 text-sm">
        <thead>
          <tr>
            <th><H k="doc.sr" /></th><th><H k="doc.desc" /></th><th>HSN/SAC</th><th><H k="doc.qty" /></th><th><H k="doc.rate" /></th><th><H k="doc.discount" /></th><th><H k="doc.taxable" /></th>
            {tax && (intra ? <><th>CGST</th><th>SGST</th></> : <th>IGST</th>)}
            <th><H k="doc.total" /></th>
          </tr>
        </thead>
        <tbody>
          {d.lines.map((l, i) => (
            <tr key={i}>
              <td>{i + 1}</td><td>{l.description}</td><td>{l.hsnSac}</td><td className="text-right">{Number(l.quantity)} {l.unit}</td>
              <td className="text-right">{money(l.ratePaise)}</td><td className="text-right">{l.discountPaise ? money(l.discountPaise) : '-'}</td>
              <td className="text-right">{money(l.taxablePaise)}</td>
              {tax && (intra
                ? <><td className="text-right">{rateLabel(l.gstRateBp / 2)}<br />{money(l.cgstPaise)}</td><td className="text-right">{rateLabel(l.gstRateBp / 2)}<br />{money(l.sgstPaise)}</td></>
                : <td className="text-right">{rateLabel(l.gstRateBp)}<br />{money(l.igstPaise)}</td>)}
              <td className="text-right">{money(l.totalPaise)}</td>
            </tr>
          ))}
        </tbody>
      </table></div>
      <section className="flex flex-wrap justify-between gap-4 mt-3">
        <div className="text-sm min-w-0 flex-1">
          <div className="font-bold"><H k="doc.words" /></div>
          <div>{d.amountInWords}</div>
          <div className="mt-3 font-bold"><H k="doc.payTo" /></div>
          <div className="break-words">{b.bankName} · A/c {b.accountName} · {b.accountNumber} · IFSC {b.ifsc}</div>
          {b.upiId && owed > 0 && (
            <div className="flex items-center gap-2 mt-2">
              <QR text={upiUri({ upiId: b.upiId, name: b.name, amountPaise: owed, note: inv.invoiceNumber || 'DRAFT' })} size={110} label="UPI QR code to pay this invoice" />
              <div className="min-w-0 break-words">UPI: {b.upiId}<br /><H k="doc.scan" /> {money(owed)}</div>
            </div>
          )}
        </div>
        <table className="w-auto text-sm" style={{ width: 'auto', minWidth: '45%' }}>
          <tbody>
            <tr><td><H k="doc.taxable" /></td><td className="text-right">{money(inv.taxablePaise)}</td></tr>
            {tax && intra && <><tr><td>CGST</td><td className="text-right">{money(inv.cgstPaise)}</td></tr><tr><td>SGST</td><td className="text-right">{money(inv.sgstPaise)}</td></tr></>}
            {tax && !intra && <tr><td>IGST</td><td className="text-right">{money(inv.igstPaise)}</td></tr>}
            <tr><td><H k="doc.roundOff" /></td><td className="text-right">{money(inv.roundOffPaise)}</td></tr>
            <tr><td className="font-bold"><H k="doc.total" /></td><td className="text-right font-bold">{money(inv.totalPaise)}</td></tr>
          </tbody>
        </table>
      </section>
      <footer className="flex flex-wrap justify-between items-end gap-4 mt-10 text-sm">
        <div className="min-w-0 break-words">{inv.notes}</div>
        <div className="text-center"><div className="mb-10"><H k="doc.forBiz" vars={{ biz: b.name }} /></div><div className="border-t border-black pt-1 px-6"><H k="doc.sign" /></div></div>
      </footer>
    </article>
  );
}
