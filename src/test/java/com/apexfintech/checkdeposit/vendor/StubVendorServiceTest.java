package com.apexfintech.checkdeposit.vendor;

import static org.assertj.core.api.Assertions.assertThat;

import com.apexfintech.checkdeposit.dto.VendorAssessmentResult;
import java.math.BigDecimal;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class StubVendorServiceTest {

  private StubVendorService stub;

  @BeforeEach
  void setUp() {
    stub = new StubVendorService();
  }

  static Stream<Arguments> scenarioTriggers() {
    return Stream.of(
        Arguments.of("iqa-pass", VendorScenario.IQA_PASS, false),
        Arguments.of("iqa-blur", VendorScenario.IQA_FAIL_BLUR, true),
        Arguments.of("iqa-glare", VendorScenario.IQA_FAIL_GLARE, true),
        Arguments.of("micr-fail", VendorScenario.MICR_READ_FAILURE, true),
        Arguments.of("duplicate", VendorScenario.DUPLICATE_DETECTED, true),
        Arguments.of("amount-mismatch", VendorScenario.AMOUNT_MISMATCH, true),
        Arguments.of("routing-mismatch", VendorScenario.ROUTING_MISMATCH, false),
        Arguments.of("clean-pass", VendorScenario.CLEAN_PASS, false));
  }

  @ParameterizedTest
  @MethodSource("scenarioTriggers")
  void assessCheck_returnsExpectedScenario_forTriggerAccountId(
      String accountId, VendorScenario expectedScenario, boolean expectsActionableMessage) {
    BigDecimal amount = new BigDecimal("150.00");

    VendorAssessmentResult result =
        stub.assessCheck(new byte[0], new byte[0], amount, accountId);

    assertThat(result.scenario()).isEqualTo(expectedScenario);
    if (expectsActionableMessage) {
      assertThat(result.actionableMessage()).isNotNull().isNotBlank();
    } else {
      assertThat(result.actionableMessage()).isNull();
    }
  }

  @ParameterizedTest
  @MethodSource("scenarioTriggers")
  void assessCheck_completesWithinOneSecond(String accountId, VendorScenario expectedScenario, boolean expectsActionableMessage) {
    BigDecimal amount = new BigDecimal("150.00");

    long start = System.nanoTime();
    stub.assessCheck(new byte[0], new byte[0], amount, accountId);
    long elapsedMs = (System.nanoTime() - start) / 1_000_000;

    assertThat(elapsedMs).isLessThan(1000);
  }
}
