package com.apexfintech.checkdeposit.domain;

public enum TransferState {
  REQUESTED,
  VALIDATING,
  ANALYZING,
  APPROVED,
  FUNDS_POSTED,
  COMPLETED,
  REJECTED,
  RETURNED
}
