-- Settlement batch tracking for bank acknowledgment
CREATE TABLE settlement_batch (
    batch_id              UUID PRIMARY KEY,
    generation_timestamp  TIMESTAMP NOT NULL,
    total_record_count    INTEGER NOT NULL,
    total_amount          DECIMAL(19, 4) NOT NULL,
    ack_status           VARCHAR(20),
    ack_details          TEXT,
    ack_timestamp         TIMESTAMP,
    created_at            TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_settlement_batch_ack_status ON settlement_batch(ack_status);
CREATE INDEX idx_settlement_batch_generation_timestamp ON settlement_batch(generation_timestamp);
