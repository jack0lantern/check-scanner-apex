package com.apexfintech.checkdeposit.dto;

import com.apexfintech.checkdeposit.domain.TransferState;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Queue item for GET /operator/queue. */
public record OperatorQueueItem(
    UUID transferId,
    TransferState state,
    String investorAccountId,
    BigDecimal enteredAmount,
    BigDecimal ocrAmount,
    String micrData,
    Double micrConfidence,
    Double vendorScore,
    RiskIndicators riskIndicators,
    String frontImage,
    String backImage,
    Instant submittedAt) {

  public record RiskIndicators(boolean amountMismatch, boolean lowVendorScore) {}
}
