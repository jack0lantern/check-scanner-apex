package com.apexfintech.checkdeposit.dto;

import com.apexfintech.checkdeposit.domain.SettlementBatch.AckStatus;
import java.util.UUID;

public record SettlementAckRequest(UUID batchId, AckStatus status, String details) {}
