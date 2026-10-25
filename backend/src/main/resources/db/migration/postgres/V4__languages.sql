ALTER TABLE customers ADD COLUMN preferred_language VARCHAR(5) NOT NULL DEFAULT 'en';
ALTER TABLE business_profile ADD COLUMN doc_second_language VARCHAR(5) NULL;
