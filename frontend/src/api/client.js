export const API_BASE = (import.meta.env.VITE_API_BASE_URL || '/api').replace(/\/+$/, '');

async function request(method, path, body, contentType = 'application/json') {
  const opts = { method, headers: {} };
  const token = localStorage.getItem('baaki.auth.token');
  if (token) {
    opts.headers['Authorization'] = `Bearer ${token}`;
  }
  if (body !== undefined) {
    opts.headers['Content-Type'] = contentType;
    opts.body = contentType === 'application/json' ? JSON.stringify(body) : body;
  }
  const res = await fetch(`${API_BASE}${path}`, opts);
  if (res.status === 401 && !path.startsWith('/auth/')) {
    localStorage.removeItem('baaki.auth.token');
    window.dispatchEvent(new CustomEvent('baaki:auth-expired'));
  }
  const text = await res.text();
  const data = text ? JSON.parse(text) : null;
  if (!res.ok) throw new Error((data && data.error) || `Request failed (${res.status})`);
  return data;
}

export const api = {
  get: (p) => request('GET', p),
  post: (p, b) => request('POST', p, b ?? {}),
  put: (p, b) => request('PUT', p, b),
  del: (p) => request('DELETE', p),
  postCsv: (p, csv) => request('POST', p, csv, 'text/csv'),
};
