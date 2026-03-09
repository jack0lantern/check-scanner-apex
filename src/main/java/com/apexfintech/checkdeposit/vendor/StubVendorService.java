package com.apexfintech.checkdeposit.vendor;

import com.apexfintech.checkdeposit.dto.VendorAssessmentResult;
import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

/**
 * Stub implementation of {@link VendorService}. Returns deterministic responses based on
 * {@code accountId} (e.g. X-Account-Id). Scenario is selected without code changes by using
 * account IDs that map to each scenario.
 *
 * <p>Trigger account IDs: iqa-pass, iqa-blur, iqa-glare, micr-fail, duplicate, amount-mismatch,
 * clean-pass (or any other value defaults to clean-pass).
 *
 * <p>Marked {@code @Primary} so it is the default implementation. Swap by providing another
 * {@link VendorService} bean and annotating it {@code @Primary}, or by using a factory/qualifier.
 */
@Service
@Primary
public class StubVendorService implements VendorService {

  /** MICR base: routing (9) + account (8). Check number (3) appended per-request for uniqueness. */
  private static final String DEFAULT_MICR_BASE = "02100002112345678";
  private static final AtomicInteger checkNumberCounter = new AtomicInteger(100);

  /** Returns unique MICR per call so repeated test deposits are not flagged as duplicates. */
  private static String nextDefaultMicr() {
    int n = checkNumberCounter.getAndIncrement();
    int checkNum = (n % 900) + 100; // 100–999
    return DEFAULT_MICR_BASE + String.format("%03d", checkNum);
  }

  /** MICR with non-matching routing for routing-mismatch scenario. */
  private static final String ROUTING_MISMATCH_MICR = "99999999912345678901";
  private static final double DEFAULT_CONFIDENCE = 0.99;
  private static final double DEFAULT_VENDOR_SCORE = 1.0;
  private static final double DEFAULT_RISK_SCORE = 0.0;

  private static final Map<String, VendorScenario> ACCOUNT_TO_SCENARIO =
      Map.ofEntries(
          Map.entry("iqa-pass", VendorScenario.IQA_PASS),
          Map.entry("iqa-blur", VendorScenario.IQA_FAIL_BLUR),
          Map.entry("iqa-glare", VendorScenario.IQA_FAIL_GLARE),
          Map.entry("micr-fail", VendorScenario.MICR_READ_FAILURE),
          Map.entry("duplicate", VendorScenario.DUPLICATE_DETECTED),
          Map.entry("amount-mismatch", VendorScenario.AMOUNT_MISMATCH),
          Map.entry("routing-mismatch", VendorScenario.ROUTING_MISMATCH),
          Map.entry("clean-pass", VendorScenario.CLEAN_PASS));

  private static final Map<VendorScenario, String> ACTIONABLE_MESSAGES =
      Map.ofEntries(
          Map.entry(
              VendorScenario.IQA_FAIL_BLUR,
              "Image too blurry — please retake in better lighting"),
          Map.entry(
              VendorScenario.IQA_FAIL_GLARE,
              "Glare detected — please move to a darker surface"),
          Map.entry(
              VendorScenario.MICR_READ_FAILURE,
              "Cannot read check routing line — please try again or deposit at a branch"),
          Map.entry(
              VendorScenario.DUPLICATE_DETECTED,
              "This check has already been deposited"),
          Map.entry(
              VendorScenario.AMOUNT_MISMATCH,
              "Recognized amount differs from entered amount — please verify"));

  @Override
  public VendorAssessmentResult assessCheck(
      byte[] frontImageData,
      byte[] backImageData,
      BigDecimal enteredAmount,
      String accountId) {
    VendorScenario scenario = resolveScenario(accountId);
    String actionableMessage = ACTIONABLE_MESSAGES.get(scenario);

    return switch (scenario) {
      case IQA_PASS -> new VendorAssessmentResult(
          scenario,
          DEFAULT_VENDOR_SCORE,
          nextDefaultMicr(),
          DEFAULT_CONFIDENCE,
          enteredAmount,
          null,
          DEFAULT_RISK_SCORE);
      case IQA_FAIL_BLUR, IQA_FAIL_GLARE -> new VendorAssessmentResult(
          scenario,
          0.4,
          null,
          null,
          null,
          actionableMessage,
          DEFAULT_RISK_SCORE);
      case MICR_READ_FAILURE -> new VendorAssessmentResult(
          scenario,
          0.6,
          null,
          null,
          enteredAmount,
          actionableMessage,
          DEFAULT_RISK_SCORE);
      case DUPLICATE_DETECTED, AMOUNT_MISMATCH -> new VendorAssessmentResult(
          scenario,
          DEFAULT_VENDOR_SCORE,
          nextDefaultMicr(),
          DEFAULT_CONFIDENCE,
          enteredAmount,
          actionableMessage,
          DEFAULT_RISK_SCORE);
      case ROUTING_MISMATCH -> new VendorAssessmentResult(
          scenario,
          DEFAULT_VENDOR_SCORE,
          ROUTING_MISMATCH_MICR,
          DEFAULT_CONFIDENCE,
          enteredAmount,
          null,
          DEFAULT_RISK_SCORE);
      case CLEAN_PASS -> new VendorAssessmentResult(
          scenario,
          DEFAULT_VENDOR_SCORE,
          nextDefaultMicr(),
          DEFAULT_CONFIDENCE,
          enteredAmount,
          null,
          DEFAULT_RISK_SCORE);
    };
  }

  private static VendorScenario resolveScenario(String accountId) {
    if (accountId == null || accountId.isBlank()) {
      return VendorScenario.CLEAN_PASS;
    }
    String key = accountId.trim().toLowerCase();
    return ACCOUNT_TO_SCENARIO.getOrDefault(key, VendorScenario.CLEAN_PASS);
  }
}
