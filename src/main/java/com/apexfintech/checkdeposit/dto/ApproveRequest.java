package com.apexfintech.checkdeposit.dto;

/** Optional body for POST /operator/queue/{transferId}/approve. */
public record ApproveRequest(String contributionTypeOverride) {}
