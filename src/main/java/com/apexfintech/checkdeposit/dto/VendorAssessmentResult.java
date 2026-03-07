package com.apexfintech.checkdeposit.dto;

import com.apexfintech.checkdeposit.vendor.VendorScenario;
import java.math.BigDecimal;

/**
 * Result of vendor check assessment (IQA, MICR extraction, OCR, duplicate detection).
 *
 * @param scenario outcome scenario
 * @param vendorScore quality/confidence score from vendor
 * @param micrData extracted MICR line, or null if unreadable
 * @param micrConfidence MICR read confidence, or null
 * @param ocrAmount OCR-extracted amount, or null
 * @param actionableMessage user-facing message for failures, or null on success
 * @param riskScore optional risk score from vendor
 */
public record VendorAssessmentResult(
    VendorScenario scenario,
    Double vendorScore,
    String micrData,
    Double micrConfidence,
    BigDecimal ocrAmount,
    String actionableMessage,
    Double riskScore) {}
