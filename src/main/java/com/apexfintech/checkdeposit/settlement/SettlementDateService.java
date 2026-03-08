package com.apexfintech.checkdeposit.settlement;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * Computes settlement date for deposits based on the 6:30 PM CT cutoff. Deposits submitted before
 * cutoff settle today; deposits at or after cutoff roll to the next business day (skipping
 * weekends and US federal holidays).
 */
@Service
public class SettlementDateService {

  private static final ZoneId CT_ZONE = ZoneId.of("America/Chicago");
  private static final LocalTime CUTOFF_TIME = LocalTime.of(18, 30); // 6:30 PM

  private final Clock clock;

  public SettlementDateService(Clock clock) {
    this.clock = clock;
  }

  /**
   * Returns the settlement date for a deposit submitted at the given instant. If submitted before
   * 6:30 PM CT today, returns today; otherwise returns the next business day.
   */
  public LocalDate computeSettlementDate(Instant submissionTime) {
    ZonedDateTime ct = submissionTime.atZone(CT_ZONE);
    LocalDate today = ct.toLocalDate();
    ZonedDateTime cutoffToday = today.atTime(CUTOFF_TIME).atZone(CT_ZONE);

    if (submissionTime.isBefore(cutoffToday.toInstant())) {
      return today;
    }
    return nextBusinessDay(today);
  }

  /**
   * Returns the settlement date for a deposit submitted now (uses the service's clock).
   */
  public LocalDate computeSettlementDateNow() {
    return computeSettlementDate(clock.instant());
  }

  private LocalDate nextBusinessDay(LocalDate from) {
    LocalDate candidate = from.plusDays(1);
    while (isWeekend(candidate) || isUsHoliday(candidate)) {
      candidate = candidate.plusDays(1);
    }
    return candidate;
  }

  private static boolean isWeekend(LocalDate date) {
    DayOfWeek dow = date.getDayOfWeek();
    return dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY;
  }

  private static boolean isUsHoliday(LocalDate date) {
    return usHolidaysForYear(date.getYear()).contains(date);
  }

  /**
   * Hardcoded list of US federal holidays for a given year. Includes fixed and computed moving
   * holidays.
   */
  private static Set<LocalDate> usHolidaysForYear(int year) {
    return java.util.stream.Stream.of(
            LocalDate.of(year, 1, 1), // New Year's Day
            thirdMondayOf(year, 1), // MLK Day
            thirdMondayOf(year, 2), // Presidents Day
            lastMondayOf(year, 5), // Memorial Day
            LocalDate.of(year, 7, 4), // Independence Day
            firstMondayOf(year, 9), // Labor Day
            secondMondayOf(year, 10), // Columbus Day
            LocalDate.of(year, 11, 11), // Veterans Day
            fourthThursdayOf(year, 11), // Thanksgiving
            LocalDate.of(year, 12, 25) // Christmas
            )
        .collect(Collectors.toSet());
  }

  private static LocalDate firstMondayOf(int year, int month) {
    return LocalDate.of(year, month, 1)
        .with(java.time.temporal.TemporalAdjusters.firstInMonth(DayOfWeek.MONDAY));
  }

  private static LocalDate secondMondayOf(int year, int month) {
    return LocalDate.of(year, month, 1)
        .with(java.time.temporal.TemporalAdjusters.dayOfWeekInMonth(2, DayOfWeek.MONDAY));
  }

  private static LocalDate thirdMondayOf(int year, int month) {
    return LocalDate.of(year, month, 1)
        .with(java.time.temporal.TemporalAdjusters.dayOfWeekInMonth(3, DayOfWeek.MONDAY));
  }

  private static LocalDate fourthThursdayOf(int year, int month) {
    return LocalDate.of(year, month, 1)
        .with(java.time.temporal.TemporalAdjusters.dayOfWeekInMonth(4, DayOfWeek.THURSDAY));
  }

  private static LocalDate lastMondayOf(int year, int month) {
    return LocalDate.of(year, month, 1)
        .with(java.time.temporal.TemporalAdjusters.lastInMonth(DayOfWeek.MONDAY));
  }
}
