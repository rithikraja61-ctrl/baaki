ALTER TABLE ladder_stages ADD COLUMN auto_send BOOLEAN NOT NULL DEFAULT FALSE;
UPDATE ladder_stages SET auto_send = TRUE WHERE code IN ('HEADS_UP', 'DUE_TODAY');

ALTER TABLE business_profile ADD COLUMN owner_email VARCHAR(200);
ALTER TABLE business_profile ADD COLUMN auto_send_enabled BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE business_profile ADD COLUMN auto_send_test_mode BOOLEAN NOT NULL DEFAULT TRUE;
UPDATE business_profile SET owner_email = 'rakesh@sharmapackaging.example' WHERE id = 1;

CREATE TABLE send_log (
  id BIGSERIAL PRIMARY KEY,
  send_date DATE NOT NULL,
  created_at TIMESTAMP NOT NULL,
  updated_at TIMESTAMP NOT NULL,
  customer_id BIGINT NOT NULL,
  customer_name VARCHAR(200),
  stage VARCHAR(20) NOT NULL,
  invoice_ids VARCHAR(400) NOT NULL,
  invoice_numbers VARCHAR(400),
  channel VARCHAR(12) NOT NULL,
  recipient VARCHAR(200),
  test_mode BOOLEAN NOT NULL,
  origin VARCHAR(10) NOT NULL,
  status VARCHAR(10) NOT NULL,
  attempts INT NOT NULL DEFAULT 0,
  provider_id VARCHAR(200),
  error TEXT
);
CREATE INDEX ix_send_date ON send_log (send_date);
CREATE INDEX ix_send_status ON send_log (status);
