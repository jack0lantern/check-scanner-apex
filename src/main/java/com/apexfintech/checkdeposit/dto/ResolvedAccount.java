package com.apexfintech.checkdeposit.dto;

public record ResolvedAccount(
    String internalNumber,
    String routingNumber,
    String micrAccountNumber,
    String omnibusAccountId,
    String accountType) {}
