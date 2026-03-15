package com.apexfintech.checkdeposit.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Past operator queue action for GET /operator/actions. */
public record OperatorActionDto(
    UUID id,
    String operatorId,
    String action,
    UUID transferId,
    String detail,
    Instant createdAt,
    String accountId,
    BigDecimal amount) {}
