// Reminder message for each stage of the escalation ladder, in English and Hindi.
// Placeholders: {contact} {customer} {business} {invoices} {amount} {days} {dueDate} {link} {owner} {phone}
const templates = {
  en: {
    HEADS_UP: 'Hello {contact}, a friendly note from {business}: invoice {invoices} for {amount} is due on {dueDate}. You can view and pay it here: {link}. Thank you!',
    DUE_TODAY: 'Hello {contact}, invoice {invoices} for {amount} from {business} is due today. Please arrange payment: {link}. Thank you.',
    FIRM: 'Hello {contact}, invoice {invoices} for {amount} is now {days} days past its due date. Please let us know when we can expect payment, or pay here: {link}. Regards, {owner}, {business}.',
    FINAL: 'Dear {contact}, this is our final reminder for invoice {invoices}: {amount} is {days} days overdue. Please pay within 3 days or call me on {phone} to agree a date: {link}. {owner}, {business}.',
    DEMAND_LETTER: 'Dear {contact}, {amount} for invoice {invoices} is {days} days overdue. We are sending you a Payment Demand Letter with the details and a 7-day deadline. Pay here: {link}. {owner}, {business}.',
    NEXT_STEPS: 'Dear {contact}, invoice {invoices} ({amount}) is {days} days overdue. Please call {owner} on {phone} today to settle this. Pay here: {link}. {business}.',
  },
  hi: {
    HEADS_UP: 'नमस्ते {contact}, {business} की ओर से याद दिलाना: बिल {invoices} की राशि {amount} {dueDate} को देय है। यहाँ देखें और भुगतान करें: {link}। धन्यवाद!',
    DUE_TODAY: 'नमस्ते {contact}, {business} का बिल {invoices} ({amount}) आज देय है। कृपया भुगतान करें: {link}। धन्यवाद।',
    FIRM: 'नमस्ते {contact}, बिल {invoices} ({amount}) की देय तारीख़ को {days} दिन हो गए हैं। कृपया बताएँ भुगतान कब होगा, या यहाँ भुगतान करें: {link}। सादर, {owner}, {business}।',
    FINAL: 'प्रिय {contact}, बिल {invoices} के लिए यह आख़िरी रिमाइंडर है: {amount} को {days} दिन हो गए हैं। कृपया 3 दिन में भुगतान करें या तारीख़ तय करने के लिए मुझे {phone} पर फ़ोन करें: {link}। {owner}, {business}।',
    DEMAND_LETTER: 'प्रिय {contact}, बिल {invoices} के {amount} को {days} दिन हो गए हैं। हम आपको विवरण और 7 दिन की समय-सीमा के साथ भुगतान माँग पत्र भेज रहे हैं। यहाँ भुगतान करें: {link}। {owner}, {business}।',
    NEXT_STEPS: 'प्रिय {contact}, बिल {invoices} ({amount}) को {days} दिन हो गए हैं। कृपया आज ही {owner} को {phone} पर फ़ोन करें। यहाँ भुगतान करें: {link}। {business}।',
  },
};
export default templates;

export function fillTemplate(lang, stage, vars) {
  let s = (templates[lang] || templates.en)[stage] || '';
  for (const [k, v] of Object.entries(vars)) s = s.replaceAll(`{${k}}`, v ?? '');
  return s;
}
