package com.apexfintech.checkdeposit.funding;

public record FundingValidationResult(
    boolean passed, String rejectionReason, String defaultContributionType) {

  public static FundingValidationResult pass(String defaultContributionType) {
    return new FundingValidationResult(true, null, defaultContributionType);
  }

  public static FundingValidationResult pass() {
    return new FundingValidationResult(true, null, null);
  }

  public static FundingValidationResult reject(String reason) {
    return new FundingValidationResult(false, reason, null);
  }
}
