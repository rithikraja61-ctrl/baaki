# Baaki — know who to chase this week

Hackathon prototype for **Sharma Packaging, Pune** (made-up business). Tracks credit invoices, tells the owner who to
follow up with today, writes GST invoices and Payment Demand Letters, matches bank credits and forecasts cash 13 weeks ahead.

Stack (team override of the brief): **Java 21+ / Spring Boot 3.5 + MySQL 8** backend, **React + Vite + Tailwind** frontend.
All data is made up. Nothing connects to a bank, GST portal or messaging service.

## Run (everything in Docker)

```powershell
docker compose up -d --build     # MySQL + API + app
```
- App: http://localhost:5190  ·  API: http://localhost:8090/api  ·  MySQL: localhost:3310 (baaki/baaki)
- The backend image compiles with Java 21 and runs the JUnit tests during `docker compose build backend`.
- Flyway creates the schema and sample data on first start.

## Money rules
- All money is `BIGINT` paise. GST is per line on taxable value, CGST/SGST rounded separately, grand total rounded to the rupee with a Round off line.
- Invoice numbers `INV/26-27/0001` come from `invoice_sequence` read with `SELECT ... FOR UPDATE`, per financial year (1 Apr–31 Mar).
- A `Clock` bean honours the **Demo date** in Settings.
- GST rates, RBI bank rate, letter deadline: `backend/src/main/resources/application.yml` (check cbic-gst.gov.in and rbi.org.in).
- "Reset demo" re-runs the Flyway migrations (`V1__schema.sql`, `V2__seed.sql`).

## Automatic reminders (email)

The backend runs a daily job at **10:00 IST** (Spring `@Scheduled`, zone Asia/Kolkata) that emails the reminders due that day.
It runs inside the existing API container, so no extra server is needed.

**Set up**
1. Copy `.env.example` to `.env` and fill in `MAIL_PROVIDER` (`resend` or `smtp`), `MAIL_FROM` and the key or SMTP login.
2. `docker compose up -d --build backend`
3. In **Settings → Automatic email reminders**, set your own email and keep **Test mode** on for the first run.
   Press **Send today's reminders now** to try it without waiting for 10:00.

**Rules the job follows**
- Only the steps switched on under **Email automatically** in Settings. Default: Friendly heads-up and Due today.
- Payment Demand Letters and Next steps are **never** sent automatically. The owner always presses Send. The API rejects turning them on.
- A step is never sent twice for the same invoice. A real send is written to the invoice timeline, which moves the ladder on.
- Nothing is sent for a disputed, snoozed or promised invoice, or one with a customer reply waiting in the Inbox.
- A customer gets at most one message a day across all channels. If the owner already marked a message as sent today, the job skips that customer. When two steps are due, the higher step wins.
- Every email goes out in the business name, with reply-to set to the owner's email. It includes a "View invoice" link (`/invoice/<token>`) and a "Pay online" link (`/pay/<token>`) for each invoice, and ends with who to contact.
- Every attempt is logged in `send_log` (SENDING, SENT, FAILED or SKIPPED) before the provider is called. Failed sends appear in **Today's follow-ups** with a **Retry** button. A retry re-checks every rule first.
- **Test mode** sends every email to the owner's address, with "[TEST]" in the subject and a line saying who it would have gone to. Test sends never move the ladder.
- With Resend, each attempt carries an `Idempotency-Key`, so a network retry cannot send twice.

## WhatsApp

Today's follow-ups keep the one-tap **wa.me** link. It opens the owner's own WhatsApp with the message filled in and needs no approval from anyone.

Sending WhatsApp messages **automatically** is different. A business cannot send the first message in a conversation from its normal WhatsApp number.
It must use the **WhatsApp Business Platform (Cloud API)** through Meta or a Business Solution Provider. That needs:
- a Meta Business account, business verification, and a phone number registered on the platform;
- **message templates** approved by Meta before use. Messages sent outside the 24-hour window after a customer writes in must use an approved template. Payment reminders go in the **Utility** category;
- per-message charges set by Meta.

Baaki does not do this yet. These are the five templates we would submit. They use the Utility category, in English (`en`) and Hindi (`hi`), and `{{n}}` are template variables.

1. `invoice_heads_up`: Hello {{1}}, a friendly note from {{2}}: invoice {{3}} for {{4}} is due on {{5}}. View and pay: {{6}}. Questions? Contact {{7}} on {{8}}.
2. `invoice_due_today`: Hello {{1}}, invoice {{2}} for {{3}} from {{4}} is due today. Pay here: {{5}}. Questions? Contact {{6}} on {{7}}.
3. `invoice_overdue_reminder`: Hello {{1}}, invoice {{2}} for {{3}} from {{4}} is {{5}} days past its due date. Please let us know when we can expect payment, or pay here: {{6}}. Questions? Contact {{7}} on {{8}}.
4. `invoice_final_reminder`: Dear {{1}}, this is our final reminder for invoice {{2}}: {{3}} is {{4}} days overdue. Please pay within 3 days or call {{5}} on {{6}} to agree a date. Pay here: {{7}}. {{8}}.
5. `payment_received`: Hello {{1}}, we have received {{2}} against invoice {{3}}. Thank you for your payment. Questions? Contact {{4}}, {{5}}, on {{6}}.

Payment Demand Letters would still never be sent by template. The owner sends them by hand.

## Add an existing invoice from a photo (AI)

New invoice → **Add an existing invoice from a photo**. Take a photo or choose a JPG, PNG or PDF of up to 5 MB.

- The browser sends the file to `POST /api/invoices/scan`. The backend calls Google Gemini with the key in `AI_API_KEY`, so the key never reaches the frontend. Get a key at https://aistudio.google.com/apikey and put it in `.env` (see `.env.example`), then run `docker compose up -d --build backend`.
- The file is held in memory only. It is never written to disk or the database, and it is wiped after reading.
- The AI reads the invoice number, date, customer, GSTIN, items, HSN/SAC, quantities, rates, taxable value, CGST/SGST/IGST, round-off, total and any printed due date.
- Baaki then checks the result. It validates the GSTIN check digits, recalculates GST from the items with the same calculator used for saving, compares that with the printed totals (₹1 tolerance), and matches the customer by GSTIN, then by name. It also flags duplicate invoice numbers, GST rates that are not current, and short HSN codes.
- The New invoice form is filled in for review. Every field the AI was unsure of, or that failed a check, is highlighted. Saving stays locked until every highlight is cleared and the owner ticks "I have compared every field". The saved invoice keeps its original number and printed due date.

Code: `backend/.../scan/` (GeminiReader, InvoiceScanService), `web/ScanController.java`, `frontend/src/components/ScanInvoice.jsx`. Tests: `InvoiceScanTest`.
