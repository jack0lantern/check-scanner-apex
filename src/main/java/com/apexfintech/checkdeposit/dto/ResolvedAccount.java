package com.apexfintech.checkdeposit.dto;

public record ResolvedAccount(
    String internalNumber, String routingNumber, String omnibusAccountId) {}
