package com.apexfintech.checkdeposit.settlement;

import static org.assertj.core.api.Assertions.assertThat;

import com.apexfintech.checkdeposit.domain.Transfer;
import com.apexfintech.checkdeposit.domain.TransferState;
import com.apexfintech.checkdeposit.repository.TransferRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Integration tests for next-business-day rollover. Verifies that the batch query selects only
 * pre-cutoff deposits and that post-cutoff deposits have settlementDate = nextBusinessDay.
 */
@SpringBootTest
@ActiveProfiles("test")
class SettlementRolloverIntegrationTest {

  private static final ZoneId CT = ZoneId.of("America/Chicago");

  @Autowired private TransferRepository transferRepository;
  @Autowired private SettlementDateService settlementDateService;

  @Test
  void batchQuery_selectsOnlyTransfersWithSettlementDateToday() {
    LocalDate today = LocalDate.of(2025, 3, 7); // Friday
    LocalDate nextBusinessDay = LocalDate.of(2025, 3, 10); // Monday

    Transfer preCutoff = createApprovedTransfer(today);
    Transfer postCutoff = createApprovedTransfer(nextBusinessDay);

    transferRepository.save(preCutoff);
    transferRepository.save(postCutoff);

    List<Transfer> batch =
        transferRepository.findByStateAndSettlementDate(TransferState.APPROVED, today);

    assertThat(batch).hasSize(1);
    assertThat(batch.get(0).getId()).isEqualTo(preCutoff.getId());
    assertThat(batch.get(0).getSettlementDate()).isEqualTo(today);

    Transfer postCutoffFromDb = transferRepository.findById(postCutoff.getId()).orElseThrow();
    assertThat(postCutoffFromDb.getSettlementDate()).isEqualTo(nextBusinessDay);
  }

  @Test
  void settlementDateService_withFixedClock_beforeCutoffReturnsToday() {
    // Use a fixed clock: Friday March 7, 2025 at 2 PM CT
    ZonedDateTime beforeCutoff = ZonedDateTime.of(2025, 3, 7, 14, 0, 0, 0, CT);
    SettlementDateService serviceWithClock =
        new SettlementDateService(Clock.fixed(beforeCutoff.toInstant(), CT));

    LocalDate result = serviceWithClock.computeSettlementDateNow();

    assertThat(result).isEqualTo(LocalDate.of(2025, 3, 7));
  }

  @Test
  void settlementDateService_withFixedClock_afterCutoffReturnsNextBusinessDay() {
    // Use a fixed clock: Friday March 7, 2025 at 8 PM CT
    ZonedDateTime afterCutoff = ZonedDateTime.of(2025, 3, 7, 20, 0, 0, 0, CT);
    SettlementDateService serviceWithClock =
        new SettlementDateService(Clock.fixed(afterCutoff.toInstant(), CT));

    LocalDate result = serviceWithClock.computeSettlementDateNow();

    assertThat(result).isEqualTo(LocalDate.of(2025, 3, 10)); // Monday
  }

  private Transfer createApprovedTransfer(LocalDate settlementDate) {
    Instant now = Instant.now();
    Transfer t =
        new Transfer(
            UUID.randomUUID(),
            new byte[0],
            new byte[0],
            new BigDecimal("100.00"),
            "INT-12345678",
            "TEST001",
            "OMN-999",
            TransferState.APPROVED,
            0.95,
            "02100002112345678901",
            0.99,
            new BigDecimal("100.00"),
            "INDIVIDUAL",
            null,
            settlementDate);
    return t;
  }
}
