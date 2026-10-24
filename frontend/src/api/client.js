async function request(method, path, body, contentType = 'application/json') {
  const opts = { method, headers: {} };
  if (body !== undefined) {
    opts.headers['Content-Type'] = contentType;
    opts.body = contentType === 'application/json' ? JSON.stringify(body) : body;
  }
  const res = await fetch('/api' + path, opts);
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
