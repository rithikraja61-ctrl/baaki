import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import { api } from '../api/client.js';

const AuthCtx = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [token, setToken] = useState(() => localStorage.getItem('baaki.auth.token'));
  const [loading, setLoading] = useState(true);
  const [googleClientId, setGoogleClientId] = useState('');

  // Fetch auth config (like googleClientId)
  useEffect(() => {
    api.get('/auth/config')
      .then((cfg) => {
        if (cfg && cfg.googleClientId) {
          setGoogleClientId(cfg.googleClientId);
        }
      })
      .catch(() => {});
  }, []);

  // Validate existing token
  useEffect(() => {
    const existingToken = localStorage.getItem('baaki.auth.token');
    if (!existingToken) {
      setUser(null);
      setLoading(false);
      return;
    }

    api.get('/auth/me')
      .then((userData) => {
        setUser(userData);
      })
      .catch(() => {
        localStorage.removeItem('baaki.auth.token');
        setUser(null);
        setToken(null);
      })
      .finally(() => {
        setLoading(false);
      });
  }, []);

  // Listen to 401 expiration
  useEffect(() => {
    const onAuthExpired = () => {
      setUser(null);
      setToken(null);
    };
    window.addEventListener('baaki:auth-expired', onAuthExpired);
    return () => window.removeEventListener('baaki:auth-expired', onAuthExpired);
  }, []);

  const loginWithGoogle = useCallback(async (credential) => {
    const res = await api.post('/auth/google', { credential });
    if (res && res.token) {
      localStorage.setItem('baaki.auth.token', res.token);
      setToken(res.token);
      setUser(res.user);
      return res.user;
    }
    throw new Error('Google Sign-In response invalid');
  }, []);

  const loginDemo = useCallback(async () => {
    const res = await api.post('/auth/demo');
    if (res && res.token) {
      localStorage.setItem('baaki.auth.token', res.token);
      setToken(res.token);
      setUser(res.user);
      return res.user;
    }
    throw new Error('Demo login response invalid');
  }, []);

  const logout = useCallback(async () => {
    try {
      await api.post('/auth/logout');
    } catch {
      // ignore network errors on logout
    } finally {
      localStorage.removeItem('baaki.auth.token');
      setUser(null);
      setToken(null);
    }
  }, []);

  const value = useMemo(() => ({
    user,
    token,
    loading,
    isAuthenticated: !!user,
    googleClientId,
    loginWithGoogle,
    loginDemo,
    logout,
  }), [user, token, loading, googleClientId, loginWithGoogle, loginDemo, logout]);

  return <AuthCtx.Provider value={value}>{children}</AuthCtx.Provider>;
}

export const useAuth = () => useContext(AuthCtx);
