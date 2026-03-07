-- Phase 1 Step 7: Database schema for check deposit system
-- Tables: accounts, transfers, ledger_entries, audit_logs, trace_events
-- Portable SQL for PostgreSQL and H2 (tests)

CREATE TABLE accounts (
    id              UUID PRIMARY KEY,
    external_id     VARCHAR(50) NOT NULL UNIQUE,
    internal_number VARCHAR(50) NOT NULL,
    routing_number  VARCHAR(20) NOT NULL,
    omnibus_id      VARCHAR(50) NOT NULL,
    account_type    VARCHAR(50),
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- transfers table: all Transfer record attributes from Phase 2 Step 2
CREATE TABLE transfers (
    id                    UUID PRIMARY KEY,
    front_image_data      TEXT,
    back_image_data       TEXT,
    amount                DECIMAL(19, 4) NOT NULL,
    to_account_id         VARCHAR(50) NOT NULL,
    from_account_id       VARCHAR(50) NOT NULL,
    type                  VARCHAR(50) NOT NULL DEFAULT 'MOVEMENT',
    memo                  VARCHAR(50) NOT NULL DEFAULT 'FREE',
    sub_type              VARCHAR(50) NOT NULL DEFAULT 'DEPOSIT',
    transfer_type         VARCHAR(50) NOT NULL DEFAULT 'CHECK',
    currency              VARCHAR(10) NOT NULL DEFAULT 'USD',
    source_application_id VARCHAR(100),
    state                 VARCHAR(50) NOT NULL,
    vendor_score          DOUBLE PRECISION,
    micr_data             VARCHAR(255),
    micr_confidence       DOUBLE PRECISION,
    ocr_amount            DECIMAL(19, 4),
    contribution_type     VARCHAR(50) DEFAULT 'INDIVIDUAL',
    deposit_source        VARCHAR(50),
    settlement_date       DATE,
    created_at            TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE ledger_entries (
    id                     UUID PRIMARY KEY,
    account_id             VARCHAR(50) NOT NULL,
    transaction_id        UUID NOT NULL,
    type                   VARCHAR(20) NOT NULL,
    amount                 DECIMAL(19, 4) NOT NULL,
    counterparty_account_id VARCHAR(50),
    created_at             TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE audit_logs (
    id          UUID PRIMARY KEY,
    operator_id VARCHAR(50),
    action      VARCHAR(50) NOT NULL,
    transfer_id UUID,
    detail      TEXT,
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE trace_events (
    id          UUID PRIMARY KEY,
    transfer_id UUID NOT NULL,
    stage       VARCHAR(50) NOT NULL,
    outcome     VARCHAR(50),
    detail      TEXT,
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
