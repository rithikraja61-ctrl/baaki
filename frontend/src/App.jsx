import { Route, Routes } from 'react-router-dom';
import Layout from './components/Layout.jsx';
import ProtectedRoute from './components/ProtectedRoute.jsx';
import Login from './pages/Login.jsx';
import Dashboard from './pages/Dashboard.jsx';
import Invoices from './pages/Invoices.jsx';
import InvoiceDetail from './pages/InvoiceDetail.jsx';
import InvoicePrint from './pages/InvoicePrint.jsx';
import NewInvoice from './pages/NewInvoice.jsx';
import Customers from './pages/Customers.jsx';
import FollowUps from './pages/FollowUps.jsx';
import Chase from './pages/Chase.jsx';
import DemandLetter from './pages/DemandLetter.jsx';
import Payments from './pages/Payments.jsx';
import Forecast from './pages/Forecast.jsx';
import Inbox from './pages/Inbox.jsx';
import Documents from './pages/Documents.jsx';
import EvidencePack from './pages/EvidencePack.jsx';
import Settings from './pages/Settings.jsx';
import PayLink from './pages/PayLink.jsx';
import InvoiceView from './pages/InvoiceView.jsx';

export default function App() {
  return (
    <Routes>
      {/* Public customer-facing and auth routes */}
      <Route path="/login" element={<Login />} />
      <Route path="/pay/:token" element={<PayLink />} />
      <Route path="/invoice/:token" element={<InvoiceView />} />
      <Route path="/invoices/:id/print" element={<InvoicePrint />} />
      <Route path="/letters/demand" element={<DemandLetter />} />
      <Route path="/documents/:id/evidence" element={<EvidencePack />} />

      {/* Protected application routes */}
      <Route element={<ProtectedRoute><Layout /></ProtectedRoute>}>
        <Route index element={<Dashboard />} />
        <Route path="invoices" element={<Invoices />} />
        <Route path="invoices/new" element={<NewInvoice />} />
        <Route path="invoices/:id" element={<InvoiceDetail />} />
        <Route path="invoices/:id/edit" element={<NewInvoice />} />
        <Route path="customers" element={<Customers />} />
        <Route path="followups" element={<FollowUps />} />
        <Route path="chase" element={<Chase />} />
        <Route path="payments" element={<Payments />} />
        <Route path="forecast" element={<Forecast />} />
        <Route path="inbox" element={<Inbox />} />
        <Route path="documents" element={<Documents />} />
        <Route path="settings" element={<Settings />} />
        <Route path="*" element={<Dashboard />} />
      </Route>
    </Routes>
  );
}
