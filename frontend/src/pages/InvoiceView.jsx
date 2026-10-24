import { Link, useParams } from 'react-router-dom';
import { translate, useApp, useLoad } from '../lib/AppContext.jsx';
import { ErrorBox, Loading } from '../components/ui.jsx';
import InvoiceDocument from '../components/InvoiceDocument.jsx';

/** Public read-only invoice, linked from reminder emails (/invoice/:token). */
export default function InvoiceView() {
  const { token } = useParams();
  useApp();
  const { data, error, loading } = useLoad(`/public/pay/${token}`);
  const q = new URLSearchParams(window.location.search).get('lang');
  const lang = ['en', 'hi', 'mr'].includes(q) ? q : (data?.customer?.preferredLanguage || 'en');
  const t = (k, v) => translate(lang, k, v);
  if (loading && !data) return <Loading />;
  if (error) return <ErrorBox error={error} />;
  return (
    <div className="py-4 px-2">
      <div className="no-print flex flex-wrap gap-2 justify-center mb-4">
        <Link className="btn-primary" to={`/pay/${token}?lang=${lang}`}>{t('invView.pay')}</Link>
        <button className="btn-secondary" onClick={() => window.print()}>🖨 {t('common.print')}</button>
      </div>
      <div className="overflow-x-auto"><InvoiceDocument d={data} /></div>
    </div>
  );
}
