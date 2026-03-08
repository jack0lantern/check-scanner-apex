package com.apexfintech.checkdeposit.domain;

/** Stage in the deposit decision trace. */
public enum TraceStage {
  SUBMISSION,
  VENDOR_RESULT,
  BUSINESS_RULE,
  OPERATOR_ACTION,
  SETTLEMENT,
  RETURN
}
