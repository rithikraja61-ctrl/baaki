import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import { api } from '../api/client.js';
import en from '../i18n/en.js';
import hi from '../i18n/hi.js';
import mr from '../i18n/mr.js';

const Ctx = createContext(null);
const DICTS = { en, hi, mr };

function lookup(dict, key) {
  return key.split('.').reduce((o, k) => (o == null ? undefined : o[k]), dict);
}

/** Translate in a given language (falls back to English). Used where the page language differs from the app's, e.g. the customer pay page. */
export function translate(lang, key, vars) {
  let s = lookup(DICTS[lang] || en, key);
  if (s === undefined) s = lookup(en, key);
  if (s === undefined) return key;
  if (vars && typeof s === 'string') for (const [k, v] of Object.entries(vars)) s = s.replaceAll(`{${k}}`, v);
  return s;
}

export function AppProvider({ children }) {
  const [lang, setLangState] = useState(() => { const l = localStorage.getItem('baaki.lang'); return DICTS[l] ? l : 'en'; });
  const [textSize, setTextSizeState] = useState(() => localStorage.getItem('baaki.textSize') || 'normal');
  const [config, setConfig] = useState(null);
  const [profile, setProfile] = useState(null);
  const [version, setVersion] = useState(0); // bump to make pages reload data

  const refresh = useCallback(async () => {
    const [c, p] = await Promise.all([api.get('/config'), api.get('/settings/profile')]);
    setConfig(c);
    setProfile(p);
    if (p.language && !localStorage.getItem('baaki.lang')) setLangState(p.language);
    setVersion((v) => v + 1);
  }, []);

  useEffect(() => { refresh().catch(() => {}); }, [refresh]);

  useEffect(() => {
    document.documentElement.lang = lang;
    document.documentElement.classList.remove('text-large', 'text-xlarge');
    if (textSize === 'large') document.documentElement.classList.add('text-large');
    if (textSize === 'xlarge') document.documentElement.classList.add('text-xlarge');
  }, [lang, textSize]);

  const setLang = (l) => { localStorage.setItem('baaki.lang', l); setLangState(l); api.put('/settings/prefs', { language: l }).catch(() => {}); };
  const setTextSize = (s) => { localStorage.setItem('baaki.textSize', s); setTextSizeState(s); api.put('/settings/prefs', { textSize: s }).catch(() => {}); };

  const t = useCallback((key, vars) => translate(lang, key, vars), [lang]);

  const value = useMemo(() => ({ lang, setLang, textSize, setTextSize, t, config, profile, refresh, version, today: config?.today }),
    [lang, textSize, t, config, profile, refresh, version]);
  return <Ctx.Provider value={value}>{children}</Ctx.Provider>;
}

export const useApp = () => useContext(Ctx);

/** Loads data from the API and reloads when the app version changes. */
export function useLoad(path, deps = []) {
  const { version } = useApp();
  const [state, setState] = useState({ data: null, error: null, loading: true });
  const load = useCallback(() => {
    if (!path) return;
    setState((s) => ({ ...s, loading: true }));
    api.get(path).then((data) => setState({ data, error: null, loading: false }))
      .catch((error) => setState({ data: null, error, loading: false }));
  }, [path]);
  // eslint-disable-next-line react-hooks/exhaustive-deps
  useEffect(load, [load, version, ...deps]);
  return { ...state, reload: load };
}
