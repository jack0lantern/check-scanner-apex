package com.apexfintech.checkdeposit.vendor;

/** Outcome scenarios from vendor check assessment (IQA, MICR, OCR, duplicate detection). */
public enum VendorScenario {
  IQA_PASS,
  IQA_FAIL_BLUR,
  IQA_FAIL_GLARE,
  MICR_READ_FAILURE,
  DUPLICATE_DETECTED,
  AMOUNT_MISMATCH,
  ROUTING_MISMATCH,
  CLEAN_PASS
}
