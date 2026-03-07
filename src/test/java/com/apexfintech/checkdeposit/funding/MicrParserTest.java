package com.apexfintech.checkdeposit.funding;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class MicrParserTest {

  @Test
  void extractRoutingNumber_returnsFirst9Digits() {
    assertThat(MicrParser.extractRoutingNumber("02100002112345678901"))
        .isEqualTo("021000021");
  }

  @Test
  void extractRoutingNumber_ignoresNonDigits() {
    assertThat(MicrParser.extractRoutingNumber("021-000-021-1234"))
        .isEqualTo("021000021");
  }

  @Test
  void extractRoutingNumber_returnsNull_whenFewerThan9Digits() {
    assertThat(MicrParser.extractRoutingNumber("12345678")).isNull();
  }

  @Test
  void extractRoutingNumber_returnsNull_whenNull() {
    assertThat(MicrParser.extractRoutingNumber(null)).isNull();
  }

  @Test
  void extractRoutingNumber_returnsNull_whenBlank() {
    assertThat(MicrParser.extractRoutingNumber("   ")).isNull();
  }

  @Test
  void extractAccountNumber_returnsDigitsBetweenRoutingAndCheck() {
    assertThat(MicrParser.extractAccountNumber("02100002112345678901")).isEqualTo("12345678");
  }

  @Test
  void extractAccountNumber_ignoresNonDigits() {
    assertThat(MicrParser.extractAccountNumber("021-000-021-1234-5678-901"))
        .isEqualTo("12345678");
  }

  @Test
  void extractAccountNumber_returnsNull_whenFewerThan20Digits() {
    assertThat(MicrParser.extractAccountNumber("02100002112345678")).isNull();
  }

  @Test
  void extractAccountNumber_returnsNull_whenNull() {
    assertThat(MicrParser.extractAccountNumber(null)).isNull();
  }

  @Test
  void extractCheckNumber_returnsLast3Digits() {
    assertThat(MicrParser.extractCheckNumber("02100002112345678901")).isEqualTo("901");
  }

  @Test
  void extractCheckNumber_ignoresNonDigits() {
    assertThat(MicrParser.extractCheckNumber("02100002112345678-901")).isEqualTo("901");
  }

  @Test
  void extractCheckNumber_returnsNull_whenFewerThan20Digits() {
    assertThat(MicrParser.extractCheckNumber("02100002112345678")).isNull();
  }

  @Test
  void extractCheckNumber_returnsNull_whenNull() {
    assertThat(MicrParser.extractCheckNumber(null)).isNull();
  }
}
