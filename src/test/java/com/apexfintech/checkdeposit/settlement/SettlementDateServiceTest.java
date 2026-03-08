package com.apexfintech.checkdeposit.settlement;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for next-business-day rollover logic. Verifies cutoff behavior and weekend/holiday
 * skipping.
 */
class SettlementDateServiceTest {

  private static final ZoneId CT = ZoneId.of("America/Chicago");

  @Test
  void beforeCutoff_returnsToday() {
    // Friday March 7, 2025 at 2:00 PM CT (before 6:30 PM)
    ZonedDateTime fridayAfternoon = ZonedDateTime.of(2025, 3, 7, 14, 0, 0, 0, CT);
    SettlementDateService service =
        new SettlementDateService(Clock.fixed(fridayAfternoon.toInstant(), CT));

    LocalDate result = service.computeSettlementDateNow();

    assertThat(result).isEqualTo(LocalDate.of(2025, 3, 7));
  }

  @Test
  void atCutoff_returnsNextBusinessDay() {
    // Friday March 7, 2025 at exactly 6:30 PM CT
    ZonedDateTime atCutoff = ZonedDateTime.of(2025, 3, 7, 18, 30, 0, 0, CT);
    SettlementDateService service = new SettlementDateService(Clock.fixed(atCutoff.toInstant(), CT));

    LocalDate result = service.computeSettlementDateNow();

    assertThat(result).isEqualTo(LocalDate.of(2025, 3, 10)); // Monday
  }

  @Test
  void afterCutoff_returnsNextBusinessDay() {
    // Friday March 7, 2025 at 7:00 PM CT (after 6:30 PM)
    ZonedDateTime fridayEvening = ZonedDateTime.of(2025, 3, 7, 19, 0, 0, 0, CT);
    SettlementDateService service =
        new SettlementDateService(Clock.fixed(fridayEvening.toInstant(), CT));

    LocalDate result = service.computeSettlementDateNow();

    assertThat(result).isEqualTo(LocalDate.of(2025, 3, 10)); // Monday (skipping weekend)
  }

  @Test
  void fridayAfterCutoff_rollsToMonday() {
    // Friday March 7, 2025 at 8:00 PM CT
    ZonedDateTime fridayNight = ZonedDateTime.of(2025, 3, 7, 20, 0, 0, 0, CT);
    SettlementDateService service =
        new SettlementDateService(Clock.fixed(fridayNight.toInstant(), CT));

    LocalDate result = service.computeSettlementDate(fridayNight.toInstant());

    assertThat(result).isEqualTo(LocalDate.of(2025, 3, 10)); // Monday
  }

  @Test
  void thursdayAfterCutoff_returnsFriday() {
    // Thursday March 6, 2025 at 7:00 PM CT
    ZonedDateTime thursdayEvening = ZonedDateTime.of(2025, 3, 6, 19, 0, 0, 0, CT);
    SettlementDateService service =
        new SettlementDateService(Clock.fixed(thursdayEvening.toInstant(), CT));

    LocalDate result = service.computeSettlementDateNow();

    assertThat(result).isEqualTo(LocalDate.of(2025, 3, 7)); // Friday
  }

  @Test
  void computeSettlementDate_withExplicitInstant_respectsCutoff() {
    SettlementDateService service =
        new SettlementDateService(Clock.fixed(Instant.EPOCH, ZoneId.of("UTC")));

    // Before cutoff Friday
    ZonedDateTime beforeCutoff = ZonedDateTime.of(2025, 3, 7, 12, 0, 0, 0, CT);
    assertThat(service.computeSettlementDate(beforeCutoff.toInstant()))
        .isEqualTo(LocalDate.of(2025, 3, 7));

    // After cutoff Friday
    ZonedDateTime afterCutoff = ZonedDateTime.of(2025, 3, 7, 19, 0, 0, 0, CT);
    assertThat(service.computeSettlementDate(afterCutoff.toInstant()))
        .isEqualTo(LocalDate.of(2025, 3, 10));
  }
}
