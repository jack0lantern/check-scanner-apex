package com.apexfintech.checkdeposit.dto;

import jakarta.validation.constraints.NotBlank;

/** Required body for POST /operator/queue/{transferId}/reject. */
public record RejectRequest(@NotBlank(message = "Reject requires non-empty reason") String reason) {}
