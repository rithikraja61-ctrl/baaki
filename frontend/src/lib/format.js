const inr = new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', minimumFractionDigits: 2 });
const inr0 = new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 0 });

/** paise (integer) -> "₹1,00,000.00" */
export const money = (paise) => inr.format((paise || 0) / 100);
/** paise -> "₹1,00,000" (whole rupees, for dashboards) */
export const money0 = (paise) => inr0.format(Math.round((paise || 0) / 100));

/** "1234.50" -> 123450 paise without floating point error */
export function toPaise(str) {
  if (str === '' || str == null) return 0;
  const s = String(str).replace(/[,₹\s]/g, '');
  if (!/^\d+(\.\d{0,2})?$/.test(s)) return NaN;
  const [r, p = ''] = s.split('.');
  return parseInt(r, 10) * 100 + parseInt((p + '00').slice(0, 2), 10);
}
export const paiseToInput = (p) => (p == null ? '' : `${Math.floor(p / 100)}.${String(p % 100).padStart(2, '0')}`);

export function fmtDate(iso, lang = 'en') {
  if (!iso) return '';
  const d = new Date(String(iso).slice(0, 10) + 'T00:00:00+05:30');
  return d.toLocaleDateString(({ hi: 'hi-IN', mr: 'mr-IN-u-nu-latn' })[lang] || 'en-IN', { day: 'numeric', month: 'short', year: 'numeric', timeZone: 'Asia/Kolkata' });
}

export const addDays = (iso, n) => {
  const d = new Date(iso + 'T00:00:00Z');
  d.setUTCDate(d.getUTCDate() + n);
  return d.toISOString().slice(0, 10);
};

export const rateLabel = (bp) => `${bp / 100}%`;

export function upiUri({ upiId, name, amountPaise, note }) {
  const am = `${Math.floor(amountPaise / 100)}.${String(amountPaise % 100).padStart(2, '0')}`;
  return `upi://pay?pa=${encodeURIComponent(upiId)}&pn=${encodeURIComponent(name)}&am=${am}&tn=${encodeURIComponent(note)}&cu=INR`;
}

/** Indian number words, for live preview (backend is the source of truth on saved invoices). */
const ONES = ['', 'One', 'Two', 'Three', 'Four', 'Five', 'Six', 'Seven', 'Eight', 'Nine', 'Ten', 'Eleven', 'Twelve', 'Thirteen', 'Fourteen', 'Fifteen', 'Sixteen', 'Seventeen', 'Eighteen', 'Nineteen'];
const TENS = ['', '', 'Twenty', 'Thirty', 'Forty', 'Fifty', 'Sixty', 'Seventy', 'Eighty', 'Ninety'];
const two = (n) => (n < 20 ? ONES[n] : TENS[Math.floor(n / 10)] + (n % 10 ? ' ' + ONES[n % 10] : ''));
const three = (n) => (n >= 100 ? ONES[Math.floor(n / 100)] + ' Hundred' + (n % 100 ? ' ' : '') : '') + (n % 100 ? two(n % 100) : '');
export function numberWords(n) {
  if (n === 0) return 'Zero';
  let out = '';
  const crore = Math.floor(n / 1e7); n %= 1e7;
  if (crore) out += numberWords(crore) + ' Crore ';
  const lakh = Math.floor(n / 1e5); n %= 1e5;
  if (lakh) out += two(lakh) + ' Lakh ';
  const th = Math.floor(n / 1000); n %= 1000;
  if (th) out += two(th) + ' Thousand ';
  if (n) out += three(n);
  return out.trim();
}
export const rupeesWords = (paise) => {
  const r = Math.floor(paise / 100), p = paise % 100;
  return 'Rupees ' + numberWords(r) + (p ? ' and ' + two(p) + ' Paise' : '') + ' Only';
};
