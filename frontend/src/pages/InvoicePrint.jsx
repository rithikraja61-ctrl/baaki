import { Link, useParams } from 'react-router-dom';
import { useApp, useLoad } from '../lib/AppContext.jsx';
import { ErrorBox, Loading } from '../components/ui.jsx';
import InvoiceDocument from '../components/InvoiceDocument.jsx';

export default function InvoicePrint() {
  const { id } = useParams();
  const { t } = useApp();
  const { data, error, loading } = useLoad(`/invoices/${id}`);
  if (loading && !data) return <Loading />;
  if (error) return <ErrorBox error={error} />;
  return (
    <div className="py-4">
      <div className="no-print flex gap-2 justify-center mb-4">
        <Link className="btn-secondary" to={`/invoices/${id}`}>← {t('common.back')}</Link>
        <button className="btn-primary" onClick={() => window.print()}>🖨 {t('common.print')}</button>
      </div>
      <InvoiceDocument d={data} />
    </div>
  );
}
