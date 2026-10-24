import { useEffect, useState } from 'react';
import { api } from '../api/client.js';
import { useApp, useLoad } from '../lib/AppContext.jsx';
import { paiseToInput, toPaise } from '../lib/format.js';
import { ErrorBox, Notice, PageTitle } from '../components/ui.jsx';

export default function Settings() {
  const { t, lang, setLang, textSize, setTextSize, config, profile, refresh, today } = useApp();
  const ladder = useLoad('/settings/ladder');
  const [p, setP] = useState(null);
  const [lad, setLad] = useState([]);
  const [demo, setDemo] = useState('');
  const [msg, setMsg] = useState(null);
  const [err, setErr] = useState(null);
  useEffect(() => { if (profile) setP({ ...profile, turnover: paiseToInput(profile.turnoverPaise), cash: paiseToInput(profile.cashInBankPaise), buffer: paiseToInput(profile.safetyBufferPaise) }); }, [profile]);
  useEffect(() => { if (ladder.data) setLad(ladder.data); }, [ladder.data]);
  useEffect(() => { if (today) setDemo(today); }, [today]);
  const run = async (fn, ok) => { setErr(null); setMsg(null); try { await fn(); await refresh(); setMsg(ok); window.scrollTo(0, 0); } catch (e) { setErr(e); window.scrollTo(0, 0); } };
  if (!p) return null;
  const set = (k) => (e) => setP({ ...p, [k]: e.target.type === 'checkbox' ? e.target.checked : e.target.value });
  const fld = (k, label, props = {}) => <div><label className="label" htmlFor={`s-${k}`}>{label}</label><input id={`s-${k}`} className="input" value={p[k] ?? ''} onChange={set(k)} {...props} /></div>;
  return (
    <div>
      <PageTitle>{t('set.title')}</PageTitle>
      <ErrorBox error={err} />
      {msg && <Notice tone="ok">{msg}</Notice>}

      <section className="card mb-4">
        <h2 className="h2">{t('set.prefs')}</h2>
        <div className="grid sm:grid-cols-2 gap-3">
          <div><label className="label" htmlFor="lang">{t('set.language')}</label><select id="lang" className="input" value={lang} onChange={(e) => setLang(e.target.value)}><option value="en">English</option><option value="hi">हिंदी</option></select></div>
          <div><label className="label" htmlFor="ts">{t('set.textSize')}</label><select id="ts" className="input" value={textSize} onChange={(e) => setTextSize(e.target.value)}><option value="normal">{t('set.normal')}</option><option value="large">{t('set.large')}</option><option value="xlarge">{t('set.xlarge')}</option></select></div>
        </div>
      </section>

      <section className="card mb-4">
        <h2 className="h2">{t('set.demo')}</h2>
        <p className="muted mb-2">{t('set.demoHelp')}</p>
        <div className="flex flex-wrap gap-2 items-end">
          <div><label className="label" htmlFor="dd">{t('set.demo')}</label><input id="dd" type="date" className="input" value={demo} onChange={(e) => setDemo(e.target.value)} /></div>
          <button className="btn-primary" onClick={() => run(() => api.put('/settings/demo-date', { date: demo }), t('common.saved'))}>{t('set.setDemo')}</button>
          <button className="btn-secondary" onClick={() => run(() => api.put('/settings/demo-date', { date: null }), t('common.saved'))}>{t('set.clearDemo')}</button>
        </div>
        <button className="btn-danger mt-4" onClick={() => { if (window.confirm(t('set.resetConfirm'))) run(() => api.post('/demo/reset'), t('set.resetDone')); }}>↺ {t('set.reset')}</button>
      </section>

      <form className="card mb-4" onSubmit={(e) => { e.preventDefault(); run(() => api.put('/settings/profile', { ...p, turnoverPaise: toPaise(p.turnover), cashInBankPaise: toPaise(p.cash), safetyBufferPaise: toPaise(p.buffer) }), t('common.saved')); }}>
        <h2 className="h2">{t('set.profile')}</h2>
        <div className="grid sm:grid-cols-2 gap-3">
          {fld('name', t('set.name'), { required: true })}{fld('ownerName', t('set.owner'))}
          {fld('address', t('set.address'))}{fld('city', t('set.city'))}
          <div><label className="label" htmlFor="s-reg">{t('set.regType')}</label><select id="s-reg" className="input" value={p.registrationType} onChange={set('registrationType')}>{['REGULAR', 'COMPOSITION', 'UNREGISTERED'].map((r) => <option key={r} value={r}>{t('set.' + r)}</option>)}</select></div>
          {p.registrationType !== 'UNREGISTERED' ? fld('gstin', t('set.gstin'), { maxLength: 15 }) : (
            <div><label className="label" htmlFor="s-st">{t('set.state')}</label><select id="s-st" className="input" value={p.stateCode} onChange={set('stateCode')}>{Object.entries(config?.states || {}).map(([c, n]) => <option key={c} value={c}>{n}</option>)}</select></div>)}
          {fld('turnover', t('set.turnover'), { inputMode: 'decimal' })}
          <label className="flex items-center gap-3 min-h-[48px] sm:mt-6"><input type="checkbox" className="w-6 h-6" checked={!!p.udyamRegistered} onChange={set('udyamRegistered')} />{t('set.udyam')}</label>
          {p.udyamRegistered && <>{fld('udyamNumber', t('set.udyamNo'))}
            <div><label className="label" htmlFor="s-cat">{t('set.category')}</label><select id="s-cat" className="input" value={p.msmeCategory || ''} onChange={set('msmeCategory')}>{['MICRO', 'SMALL', 'MEDIUM'].map((c) => <option key={c} value={c}>{t('set.' + c)}</option>)}</select></div></>}
          {fld('phone', t('set.phone'))}{fld('email', t('set.email'))}
          {fld('bankName', t('set.bank'))}{fld('accountName', t('set.accName'))}
          {fld('accountNumber', t('set.accNo'))}{fld('ifsc', t('set.ifsc'))}
          {fld('upiId', t('set.upi'))}
          <div><label className="label" htmlFor="s-logo">{t('set.logo')}</label><input id="s-logo" type="file" accept="image/*" className="block min-h-[48px]" onChange={(e) => { const f = e.target.files?.[0]; if (!f) return; const r = new FileReader(); r.onload = () => setP({ ...p, logoDataUrl: r.result }); r.readAsDataURL(f); }} /></div>
          {fld('cash', t('set.cash'), { inputMode: 'decimal' })}{fld('buffer', t('set.buffer'), { inputMode: 'decimal' })}
        </div>
        <button className="btn-primary mt-3">{t('common.save')}</button>
      </form>

      <form className="card mb-4" onSubmit={(e) => { e.preventDefault(); run(() => api.put('/settings/ladder', lad), t('common.saved')); }}>
        <h2 className="h2">{t('set.ladder')}</h2>
        <p className="muted mb-2">{t('set.ladderHelp')}</p>
        <ul className="space-y-2">
          {lad.map((s, i) => (
            <li key={s.code} className="flex flex-wrap items-center gap-3">
              <span className="w-full sm:w-48 font-semibold">{t('fu.stage.' + s.code)}</span>
              <label className="sr-only" htmlFor={`lo${i}`}>{t('set.offset')}</label>
              <input id={`lo${i}`} type="number" className="input w-28" value={s.offsetDays} onChange={(e) => setLad(lad.map((x, j) => (j === i ? { ...x, offsetDays: Number(e.target.value) } : x)))} />
              <label className="flex items-center gap-2 min-h-[48px]"><input type="checkbox" className="w-6 h-6" checked={s.enabled} onChange={(e) => setLad(lad.map((x, j) => (j === i ? { ...x, enabled: e.target.checked } : x)))} />{t('set.enabled')}</label>
            </li>
          ))}
        </ul>
        <button className="btn-primary mt-3">{t('common.save')}</button>
      </form>
    </div>
  );
}
