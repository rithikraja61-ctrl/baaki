CREATE TABLE business_profile (
  id BIGINT PRIMARY KEY,
  name VARCHAR(200) NOT NULL,
  owner_name VARCHAR(120),
  address VARCHAR(400),
  city VARCHAR(100),
  state_code CHAR(2) NOT NULL,
  gstin VARCHAR(15),
  registration_type VARCHAR(20) NOT NULL,
  turnover_paise BIGINT NOT NULL DEFAULT 0,
  udyam_registered BOOLEAN NOT NULL DEFAULT FALSE,
  udyam_number VARCHAR(40),
  msme_category VARCHAR(10),
  phone VARCHAR(30),
  email VARCHAR(120),
  bank_name VARCHAR(120),
  account_name VARCHAR(120),
  account_number VARCHAR(30),
  ifsc VARCHAR(15),
  upi_id VARCHAR(80),
  cash_in_bank_paise BIGINT NOT NULL DEFAULT 0,
  safety_buffer_paise BIGINT NOT NULL DEFAULT 0,
  demo_date DATE,
  language VARCHAR(5) NOT NULL DEFAULT 'en',
  text_size VARCHAR(10) NOT NULL DEFAULT 'normal',
  logo_data_url MEDIUMTEXT
);
CREATE TABLE customers (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(200) NOT NULL,
  contact_person VARCHAR(120),
  phone VARCHAR(30),
  email VARCHAR(120),
  address VARCHAR(400),
  city VARCHAR(100),
  state_code CHAR(2) NOT NULL,
  gstin VARCHAR(15),
  credit_days INT NOT NULL DEFAULT 30
);
CREATE TABLE payment_history (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  customer_id BIGINT NOT NULL,
  reference VARCHAR(40),
  due_date DATE NOT NULL,
  paid_date DATE NOT NULL,
  amount_paise BIGINT NOT NULL,
  CONSTRAINT fk_ph_c FOREIGN KEY (customer_id) REFERENCES customers(id)
);
CREATE TABLE invoices (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  invoice_number VARCHAR(16) UNIQUE,
  customer_id BIGINT NOT NULL,
  invoice_date DATE NOT NULL,
  due_date DATE NOT NULL,
  status VARCHAR(12) NOT NULL,
  doc_type VARCHAR(20) NOT NULL,
  place_of_supply CHAR(2) NOT NULL,
  reverse_charge BOOLEAN NOT NULL DEFAULT FALSE,
  delivery_address VARCHAR(400),
  taxable_paise BIGINT NOT NULL,
  cgst_paise BIGINT NOT NULL,
  sgst_paise BIGINT NOT NULL,
  igst_paise BIGINT NOT NULL,
  round_off_paise BIGINT NOT NULL,
  total_paise BIGINT NOT NULL,
  pay_token VARCHAR(24) NOT NULL UNIQUE,
  sent_at DATETIME,
  promised_date DATE,
  snoozed_until DATE,
  notes VARCHAR(500),
  CONSTRAINT fk_inv_c FOREIGN KEY (customer_id) REFERENCES customers(id)
);
CREATE TABLE invoice_lines (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  invoice_id BIGINT NOT NULL,
  line_no INT NOT NULL,
  description VARCHAR(300) NOT NULL,
  hsn_sac VARCHAR(8) NOT NULL,
  quantity DECIMAL(12,3) NOT NULL,
  unit VARCHAR(12) NOT NULL,
  rate_paise BIGINT NOT NULL,
  discount_paise BIGINT NOT NULL DEFAULT 0,
  gst_rate_bp INT NOT NULL,
  taxable_paise BIGINT NOT NULL,
  cgst_paise BIGINT NOT NULL,
  sgst_paise BIGINT NOT NULL,
  igst_paise BIGINT NOT NULL,
  total_paise BIGINT NOT NULL,
  CONSTRAINT fk_line_inv FOREIGN KEY (invoice_id) REFERENCES invoices(id) ON DELETE CASCADE
);
CREATE TABLE payments (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  payment_date DATE NOT NULL,
  amount_paise BIGINT NOT NULL,
  reference VARCHAR(60),
  description VARCHAR(300),
  payer_name VARCHAR(200),
  source VARCHAR(12) NOT NULL,
  status VARCHAR(12) NOT NULL,
  note VARCHAR(300)
);
CREATE TABLE payment_allocations (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  payment_id BIGINT NOT NULL,
  invoice_id BIGINT NOT NULL,
  amount_paise BIGINT NOT NULL
);
CREATE TABLE credit_notes (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  note_number VARCHAR(16) NOT NULL UNIQUE,
  invoice_id BIGINT NOT NULL,
  note_date DATE NOT NULL,
  amount_paise BIGINT NOT NULL,
  reason VARCHAR(400)
);
CREATE TABLE invoice_events (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  invoice_id BIGINT NOT NULL,
  customer_id BIGINT NOT NULL,
  event_at DATETIME NOT NULL,
  type VARCHAR(20) NOT NULL,
  stage VARCHAR(20),
  channel VARCHAR(12),
  details TEXT,
  INDEX ix_ev_inv (invoice_id)
);
CREATE TABLE inbox_items (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  invoice_id BIGINT NOT NULL,
  kind VARCHAR(16) NOT NULL,
  reason TEXT,
  reference VARCHAR(60),
  paid_date DATE,
  status VARCHAR(12) NOT NULL,
  reply TEXT,
  created_at DATETIME NOT NULL,
  resolved_at DATETIME
);
CREATE TABLE expenses (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(200) NOT NULL,
  category VARCHAR(20) NOT NULL,
  amount_paise BIGINT NOT NULL,
  recurrence VARCHAR(10) NOT NULL,
  day_of_month INT,
  due_date DATE
);
CREATE TABLE invoice_sequence (
  seq_key VARCHAR(20) PRIMARY KEY,
  next_value BIGINT NOT NULL
);
CREATE TABLE ladder_stages (
  code VARCHAR(20) PRIMARY KEY,
  offset_days INT NOT NULL,
  sort_order INT NOT NULL,
  enabled BOOLEAN NOT NULL DEFAULT TRUE
);

-- Default escalation ladder (editable in Settings)
INSERT INTO ladder_stages (code, offset_days, sort_order, enabled) VALUES
('HEADS_UP', -3, 1, TRUE), ('DUE_TODAY', 0, 2, TRUE), ('FIRM', 7, 3, TRUE),
('FINAL', 15, 4, TRUE), ('DEMAND_LETTER', 30, 5, TRUE), ('NEXT_STEPS', 45, 6, TRUE);
