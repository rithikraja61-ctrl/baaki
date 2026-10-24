-- Marathi as a third language: each customer's preferred language (reminders + pay link),
-- and an optional second language printed under each heading on invoices and demand letters.
ALTER TABLE customers ADD COLUMN preferred_language VARCHAR(5) NOT NULL DEFAULT 'en';
ALTER TABLE business_profile ADD COLUMN doc_second_language VARCHAR(5) NULL;
UPDATE customers SET preferred_language = 'mr' WHERE id IN (3, 5, 7, 8, 11);
UPDATE customers SET preferred_language = 'hi' WHERE id = 12;
