package com.apexfintech.checkdeposit.dto;

import com.apexfintech.checkdeposit.domain.TransferState;
import java.util.UUID;

/** Successful deposit response (201 Created). */
public record DepositResponse(UUID transferId, TransferState state) {}
