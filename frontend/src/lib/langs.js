// Languages offered everywhere. Each name is written in its own script.
export const LANGS = [
  { code: 'en', name: 'English' },
  { code: 'hi', name: 'हिंदी' },
  { code: 'mr', name: 'मराठी' },
];
export const langName = (code) => (LANGS.find((l) => l.code === code) || LANGS[0]).name;
