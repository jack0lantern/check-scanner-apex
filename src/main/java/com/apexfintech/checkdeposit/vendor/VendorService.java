package com.apexfintech.checkdeposit.vendor;

import com.apexfintech.checkdeposit.dto.VendorAssessmentResult;
import java.math.BigDecimal;

/**
 * Internal interface for check image processing: image quality assessment (IQA), MICR extraction,
 * OCR, and duplicate detection.
 *
 * <p>Implementations are swappable via Spring {@code @Primary} or a factory pattern without
 * modifying the Funding Service or deposit submission code.
 */
public interface VendorService {

  /**
   * Assesses a check: IQA, MICR extraction, OCR, and duplicate detection.
   *
   * @param frontImageData front check image bytes
   * @param backImageData back check image bytes
   * @param enteredAmount amount entered by user
   * @param accountId account ID (e.g. from X-Account-Id) for scenario triggering
   * @return assessment result with scenario, scores, extracted data, and optional actionable
   *     message
   */
  VendorAssessmentResult assessCheck(
      byte[] frontImageData, byte[] backImageData, BigDecimal enteredAmount, String accountId);
}
