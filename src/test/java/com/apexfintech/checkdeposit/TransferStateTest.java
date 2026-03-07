package com.apexfintech.checkdeposit;

import static org.assertj.core.api.Assertions.assertThat;

import com.apexfintech.checkdeposit.domain.TransferState;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class TransferStateTest {

  private static final Set<String> EXPECTED_STATES =
      Set.of(
          "REQUESTED",
          "VALIDATING",
          "ANALYZING",
          "APPROVED",
          "FUNDS_POSTED",
          "COMPLETED",
          "REJECTED",
          "RETURNED");

  @Test
  void hasExactlyEightEnumValues() {
    assertThat(TransferState.values()).hasSize(8);
  }

  @Test
  void allEnumValuesAreSpelledCorrectly() {
    Set<String> actualNames =
        Arrays.stream(TransferState.values()).map(Enum::name).collect(Collectors.toSet());
    assertThat(actualNames).isEqualTo(EXPECTED_STATES);
  }
}
