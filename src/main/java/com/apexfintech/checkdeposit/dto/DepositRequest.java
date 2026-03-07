package com.apexfintech.checkdeposit.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Request body for deposit submission.
 *
 * @param frontImage Base64-encoded front check image
 * @param backImage Base64-encoded back check image
 * @param amount deposit amount
 * @param accountId investor account ID (e.g. TEST001)
 * @param retryForTransferId when present, updates this existing transfer instead of creating new
 */
public record DepositRequest(
    String frontImage,
    String backImage,
    BigDecimal amount,
    String accountId,
    UUID retryForTransferId) {}
