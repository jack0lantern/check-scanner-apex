package com.apexfintech.checkdeposit.vendor;

import static org.assertj.core.api.Assertions.assertThat;

import com.apexfintech.checkdeposit.dto.VendorAssessmentResult;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * One dedicated JUnit test per Vendor stub scenario. Each test asserts: correct scenario enum
 * returned; actionableMessage non-null for all failure scenarios; response time &lt; 1 second.
 */
class StubVendorServiceTest {

  private StubVendorService stub;
  private static final BigDecimal AMOUNT = new BigDecimal("150.00");

  @BeforeEach
  void setUp() {
    stub = new StubVendorService();
  }

  @Test
  void iqaPass_returnsIqaPassScenario_noActionableMessage() {
    VendorAssessmentResult result = stub.assessCheck(new byte[0], new byte[0], AMOUNT, "iqa-pass");
    assertThat(result.scenario()).isEqualTo(VendorScenario.IQA_PASS);
    assertThat(result.actionableMessage()).isNull();
    assertResponseTimeUnderOneSecond("iqa-pass");
  }

  @Test
  void iqaFailBlur_returnsIqaFailBlurScenario_withActionableMessage() {
    VendorAssessmentResult result = stub.assessCheck(new byte[0], new byte[0], AMOUNT, "iqa-blur");
    assertThat(result.scenario()).isEqualTo(VendorScenario.IQA_FAIL_BLUR);
    assertThat(result.actionableMessage()).isNotNull().isNotBlank();
    assertResponseTimeUnderOneSecond("iqa-blur");
  }

  @Test
  void iqaFailGlare_returnsIqaFailGlareScenario_withActionableMessage() {
    VendorAssessmentResult result = stub.assessCheck(new byte[0], new byte[0], AMOUNT, "iqa-glare");
    assertThat(result.scenario()).isEqualTo(VendorScenario.IQA_FAIL_GLARE);
    assertThat(result.actionableMessage()).isNotNull().isNotBlank();
    assertResponseTimeUnderOneSecond("iqa-glare");
  }

  @Test
  void micrReadFailure_returnsMicrReadFailureScenario_withActionableMessage() {
    VendorAssessmentResult result = stub.assessCheck(new byte[0], new byte[0], AMOUNT, "micr-fail");
    assertThat(result.scenario()).isEqualTo(VendorScenario.MICR_READ_FAILURE);
    assertThat(result.actionableMessage()).isNotNull().isNotBlank();
    assertResponseTimeUnderOneSecond("micr-fail");
  }

  @Test
  void duplicateDetected_returnsDuplicateDetectedScenario_withActionableMessage() {
    VendorAssessmentResult result = stub.assessCheck(new byte[0], new byte[0], AMOUNT, "duplicate");
    assertThat(result.scenario()).isEqualTo(VendorScenario.DUPLICATE_DETECTED);
    assertThat(result.actionableMessage()).isNotNull().isNotBlank();
    assertResponseTimeUnderOneSecond("duplicate");
  }

  @Test
  void amountMismatch_returnsAmountMismatchScenario_withActionableMessage() {
    VendorAssessmentResult result =
        stub.assessCheck(new byte[0], new byte[0], AMOUNT, "amount-mismatch");
    assertThat(result.scenario()).isEqualTo(VendorScenario.AMOUNT_MISMATCH);
    assertThat(result.actionableMessage()).isNotNull().isNotBlank();
    assertResponseTimeUnderOneSecond("amount-mismatch");
  }

  @Test
  void cleanPass_returnsCleanPassScenario_noActionableMessage() {
    VendorAssessmentResult result =
        stub.assessCheck(new byte[0], new byte[0], AMOUNT, "clean-pass");
    assertThat(result.scenario()).isEqualTo(VendorScenario.CLEAN_PASS);
    assertThat(result.actionableMessage()).isNull();
    assertResponseTimeUnderOneSecond("clean-pass");
  }

  private void assertResponseTimeUnderOneSecond(String accountId) {
    long start = System.nanoTime();
    stub.assessCheck(new byte[0], new byte[0], AMOUNT, accountId);
    long elapsedMs = (System.nanoTime() - start) / 1_000_000;
    assertThat(elapsedMs).isLessThan(1000);
  }
}
