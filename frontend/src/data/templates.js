// Reminder message for each stage of the escalation ladder, in English, Hindi and Marathi.
// The text lives in reminder-templates.json, which the backend's daily email job also uses
// (copy of backend/src/main/resources/reminder-templates.json).
// Placeholders: {contact} {customer} {business} {invoices} {amount} {days} {dueDate} {link} {owner} {phone} {email}
import data from './reminder-templates.json';

const templates = { en: data.en.stages, hi: data.hi.stages, mr: data.mr.stages };
export default templates;
export const reminderText = data;

export function fillTemplate(lang, stage, vars) {
  const pack = data[lang] || data.en;
  let s = (pack.stages[stage] || '') + ' ' + pack.footer;
  for (const [k, v] of Object.entries(vars)) s = s.replaceAll(`{${k}}`, v ?? '');
  return s;
}
