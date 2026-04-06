-- Initial Schema Setup for Transaction Recon Engine

-- 1. Transactions Table
CREATE TABLE IF NOT EXISTS transactions (
    id BIGSERIAL PRIMARY KEY,
    request_id VARCHAR(255) UNIQUE NOT NULL,
    user_id BIGINT NOT NULL,
    amount DECIMAL(19, 2) NOT NULL,
    status VARCHAR(50) NOT NULL,
    timestamp TIMESTAMP NOT NULL
);

-- 2. Ledger Accounts Table
CREATE TABLE IF NOT EXISTS ledger_accounts (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) UNIQUE NOT NULL,
    name VARCHAR(255) NOT NULL,
    active BOOLEAN DEFAULT TRUE
);

-- 3. Ledger Journal Headers Table
CREATE TABLE IF NOT EXISTS journal_headers (
    id BIGSERIAL PRIMARY KEY,
    journal_id VARCHAR(255) UNIQUE NOT NULL,
    reference_id VARCHAR(255),
    description VARCHAR(255)
);

-- 4. Ledger Entries Table
CREATE TABLE IF NOT EXISTS ledger_entries (
    id BIGSERIAL PRIMARY KEY,
    journal_id BIGINT NOT NULL,
    account_id BIGINT NOT NULL,
    entry_type VARCHAR(20) NOT NULL,
    amount DECIMAL(19, 2) NOT NULL,
    entry_description VARCHAR(255),
    CONSTRAINT fk_journal FOREIGN KEY (journal_id) REFERENCES journal_headers(id),
    CONSTRAINT fk_account FOREIGN KEY (account_id) REFERENCES ledger_accounts(id)
);

-- 5. External Transaction Snapshots Table (Recon)
CREATE TABLE IF NOT EXISTS external_transaction_snapshots (
    id BIGSERIAL PRIMARY KEY,
    source_system VARCHAR(100) NOT NULL,
    reference_id VARCHAR(255) NOT NULL,
    amount DECIMAL(19, 2) NOT NULL,
    currency VARCHAR(10) NOT NULL,
    event_time TIMESTAMP NOT NULL,
    UNIQUE (source_system, reference_id)
);

-- 6. Recon Results Table
CREATE TABLE IF NOT EXISTS recon_results (
    id BIGSERIAL PRIMARY KEY,
    source_system VARCHAR(100) NOT NULL,
    reference_id VARCHAR(255) NOT NULL,
    ledger_amount DECIMAL(19, 2),
    external_amount DECIMAL(19, 2),
    status VARCHAR(50) NOT NULL,
    matched_at TIMESTAMP,
    UNIQUE (source_system, reference_id)
);

-- Insert Default/Base Chart of Accounts (COA)
INSERT INTO ledger_accounts (code, name, active) VALUES
('CLEARING', 'General Clearing Account', true),
('CASH', 'Main Cash Account', true),
('REV_FEES', 'Revenue from Fees', true),
('EXP_TAX', 'Tax Expenses', true)
ON CONFLICT (code) DO NOTHING;
