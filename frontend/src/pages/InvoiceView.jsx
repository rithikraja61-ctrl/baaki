import { Link, useParams } from 'react-router-dom';
import { useApp, useLoad } from '../lib/AppContext.jsx';
import { ErrorBox, Loading } from '../components/ui.jsx';
import InvoiceDocument from '../components/InvoiceDocument.jsx';

/** Public read-only invoice, linked from reminder emails (/invoice/:token). */
export default function InvoiceView() {
  const { token } = useParams();
  const { t } = useApp();
  const { data, error, loading } = useLoad(`/public/pay/${token}`);
  if (loading && !data) return <Loading />;
  if (error) return <ErrorBox error={error} />;
  return (
    <div className="py-4 px-2">
      <div className="no-print flex flex-wrap gap-2 justify-center mb-4">
        <Link className="btn-primary" to={`/pay/${token}`}>{t('invView.pay')}</Link>
        <button className="btn-secondary" onClick={() => window.print()}>🖨 {t('common.print')}</button>
      </div>
      <div className="overflow-x-auto"><InvoiceDocument d={data} /></div>
    </div>
  );
}
