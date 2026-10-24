# Baaki — know who to chase this week

Hackathon prototype for **Sharma Packaging, Pune** (made-up business). Tracks credit invoices, tells the owner who to
follow up with today, writes GST invoices and Payment Demand Letters, matches bank credits and forecasts cash 13 weeks ahead.

Stack (team override of the brief): **Java 21+ / Spring Boot 3.5 + MySQL 8** backend, **React + Vite + Tailwind** frontend.
All data is made up. Nothing connects to a bank, GST portal or messaging service.

## Run (Windows)

```powershell
docker compose up -d                 # MySQL 8 on localhost:3307
cd backend; .\mvnw.cmd spring-boot:run   # API on http://localhost:8080 (Flyway creates schema + sample data)
cd ..\frontend; npm install; npm run dev # http://localhost:5173
```

Tests: `cd backend; .\mvnw.cmd test`

## Money rules
- All money is `BIGINT` paise. GST is per line on taxable value, CGST/SGST rounded separately, grand total rounded to the rupee with a Round off line.
- Invoice numbers `INV/26-27/0001` come from `invoice_sequence` read with `SELECT ... FOR UPDATE`, per financial year (1 Apr–31 Mar).
- A `Clock` bean honours the **Demo date** in Settings.
- GST rates, RBI bank rate, letter deadline: `backend/src/main/resources/application.yml` (check cbic-gst.gov.in and rbi.org.in).
- "Reset demo" re-runs the Flyway migrations (`V1__schema.sql`, `V2__seed.sql`).
