package com.apexfintech.checkdeposit.dto;

import java.util.UUID;

/** IQA or validation failure response (422 Unprocessable Entity). */
public record IqaFailureResponse(UUID transferId, String actionableMessage) {}
