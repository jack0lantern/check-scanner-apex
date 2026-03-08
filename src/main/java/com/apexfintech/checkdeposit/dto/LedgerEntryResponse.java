package com.apexfintech.checkdeposit.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record LedgerEntryResponse(
    UUID entryId,
    String type,
    BigDecimal amount,
    String counterpartyAccountId,
    UUID transactionId,
    Instant timestamp) {}
