package com.apexfintech.checkdeposit.dto;

public record ResolvedAccount(
    String internalNumber,
    String externalId,
    String routingNumber,
    String micrAccountNumber,
    String omnibusAccountId,
    String accountType) {}
