import { useEffect, useRef, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext.jsx';
import { useApp } from '../lib/AppContext.jsx';
import { LangPicker } from '../components/ui.jsx';

export default function Login() {
  const { user, isAuthenticated, loading, googleClientId, loginWithGoogle, loginDemo } = useAuth();
  const { t, lang, setLang } = useApp();
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const googleBtnRef = useRef(null);
  const navigate = useNavigate();
  const location = useLocation();

  const from = location.state?.from?.pathname || '/';

  // Redirect if already authenticated
  useEffect(() => {
    if (isAuthenticated && !loading) {
      navigate(from, { replace: true });
    }
  }, [isAuthenticated, loading, navigate, from]);

  // Initialize Google Identity Services button
  useEffect(() => {
    if (loading || isAuthenticated) return;

    if (!googleClientId) {
      return;
    }

    const checkGsi = () => {
      if (window.google?.accounts?.id && googleBtnRef.current) {
        try {
          window.google.accounts.id.initialize({
            client_id: googleClientId,
            callback: async (response) => {
              try {
                setSubmitting(true);
                setError('');
                await loginWithGoogle(response.credential);
              } catch (err) {
                setError(err.message || 'Failed to authenticate with Google');
              } finally {
                setSubmitting(false);
              }
            },
          });

          window.google.accounts.id.renderButton(googleBtnRef.current, {
            type: 'standard',
            theme: 'outline',
            size: 'large',
            text: 'signin_with',
            shape: 'rectangular',
            logo_alignment: 'left',
            width: 320,
          });
        } catch (e) {
          console.error('Google button init error:', e);
        }
      } else {
        setTimeout(checkGsi, 200);
      }
    };

    checkGsi();
  }, [googleClientId, loading, isAuthenticated, loginWithGoogle]);

  const handleDemoLogin = async () => {
    try {
      setSubmitting(true);
      setError('');
      await loginDemo();
    } catch (err) {
      setError(err.message || 'Demo login failed');
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gray-50">
        <div className="text-center p-6">
          <div className="w-10 h-10 border-4 border-brand-600 border-t-transparent rounded-full animate-spin mx-auto mb-4"></div>
          <p className="text-gray-600 font-medium">Checking authentication...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen flex flex-col justify-center items-center bg-gray-50 px-4 py-8">
      {/* Top language selector */}
      <div className="absolute top-4 right-4">
        <LangPicker value={lang} onChange={setLang} label={t('common.switchLang')} />
      </div>

      <div className="w-full max-w-md">
        {/* Brand Header */}
        <div className="text-center mb-8">
          <div className="inline-flex items-center justify-center w-16 h-16 rounded-2xl bg-brand-600 text-white font-bold text-3xl shadow-md mb-4">
            ₹
          </div>
          <h1 className="text-3xl font-bold text-brand-800 tracking-tight">{t('appName')}</h1>
          <p className="text-sm text-gray-600 mt-1">{t('tagline')}</p>
        </div>

        {/* Card */}
        <div className="card shadow-lg p-6 sm:p-8 bg-white border border-gray-200">
          <div className="mb-6">
            <h2 className="text-xl font-bold text-gray-900">Sign in to your account</h2>
            <p className="text-sm text-gray-500 mt-1">Manage invoices, GST, and automated payment follow-ups.</p>
          </div>

          {error && (
            <div className="mb-4 p-3 bg-red-50 border border-red-200 rounded-lg text-sm text-red-700 flex items-start gap-2">
              <span className="font-bold">!</span>
              <span>{error}</span>
            </div>
          )}

          {/* Google Sign In Area */}
          <div className="space-y-4">
            {googleClientId ? (
              <div className="flex flex-col items-center">
                <div ref={googleBtnRef} className="min-h-[44px] flex justify-center w-full"></div>
                <p className="text-xs text-gray-500 mt-2">Sign in using your authorized Google account</p>
              </div>
            ) : (
              <div className="p-3 bg-blue-50 border border-blue-200 rounded-lg text-xs text-blue-800">
                <div className="font-semibold mb-1">Google OAuth Configuration</div>
                <div>To enable Google Sign-In, add your <code>GOOGLE_CLIENT_ID</code> to <code>.env</code>. You can sign in immediately using Demo Mode below.</div>
              </div>
            )}

            <div className="relative my-6">
              <div className="absolute inset-0 flex items-center">
                <div className="w-full border-t border-gray-200"></div>
              </div>
              <div className="relative flex justify-center text-xs uppercase">
                <span className="bg-white px-3 text-gray-400 font-medium">Or quick access</span>
              </div>
            </div>

            {/* Demo Login Button */}
            <button
              type="button"
              onClick={handleDemoLogin}
              disabled={submitting}
              className="w-full group relative flex items-center justify-between p-3.5 rounded-xl border-2 border-brand-600 bg-brand-50 hover:bg-brand-100/70 transition-all text-left shadow-sm hover:shadow"
            >
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 rounded-full bg-brand-600 text-white flex items-center justify-center font-bold text-lg shrink-0">
                  RS
                </div>
                <div>
                  <div className="font-bold text-gray-900 group-hover:text-brand-800">
                    Continue as Rakesh Sharma
                  </div>
                  <div className="text-xs text-gray-600">
                    Owner • Sharma Packaging Industries (MSME)
                  </div>
                </div>
              </div>
              <span className="text-xs font-semibold px-2 py-1 rounded bg-brand-600 text-white shrink-0">
                {submitting ? 'Signing in...' : 'Demo Login →'}
              </span>
            </button>
          </div>

          <div className="mt-8 pt-6 border-t border-gray-100 text-center text-xs text-gray-500">
            Protected under Section 15 & 16 of the MSME Development Act, 2006.
          </div>
        </div>
      </div>
    </div>
  );
}
