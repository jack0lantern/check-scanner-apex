package com.apexfintech.checkdeposit.dto;

import com.apexfintech.checkdeposit.domain.TransferState;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Transfer status response for GET /deposits/{transferId}. */
public record TransferStatusResponse(
    UUID transferId,
    TransferState state,
    BigDecimal amount,
    String accountId,
    Instant createdAt,
    Instant updatedAt,
    Double vendorScore,
    String micrData,
    Double micrConfidence,
    BigDecimal ocrAmount,
    String vendorMessage) {}
