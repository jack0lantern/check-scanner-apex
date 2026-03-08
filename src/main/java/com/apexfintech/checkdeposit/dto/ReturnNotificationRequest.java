package com.apexfintech.checkdeposit.dto;

import java.util.UUID;

public record ReturnNotificationRequest(UUID transferId, String returnReason) {}
