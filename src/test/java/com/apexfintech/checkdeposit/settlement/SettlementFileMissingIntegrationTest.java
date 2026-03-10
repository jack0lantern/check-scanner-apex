package com.apexfintech.checkdeposit.settlement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.apexfintech.checkdeposit.domain.Transfer;
import com.apexfintech.checkdeposit.domain.TransferState;
import com.apexfintech.checkdeposit.repository.TransferRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

/**
 * Integration test for Missing Settlement File Monitor. When file generation fails but APPROVED
 * transfers exist for settlementDate = today, the EOD run must log a structured
 * SETTLEMENT_FILE_MISSING warning.
 */
@SpringBootTest(properties = "spring.task.scheduling.enabled=false")
@ActiveProfiles("test")
class SettlementFileMissingIntegrationTest {

  @Autowired private EodSchedulerService eodSchedulerService;
  @Autowired private SettlementDateService settlementDateService;
  @Autowired private TransferRepository transferRepository;

  @MockBean private SettlementFileService settlementFileService;

  private static final byte[] PLACEHOLDER_IMAGE = new byte[] {0x01, 0x02, 0x03};

  @BeforeEach
  void setUp() {
    transferRepository.deleteAll();
  }

  @Test
  void eodBatch_whenFileGenerationFailsButApprovedTransfersExist_logsSettlementFileMissing() {
    LocalDate today = settlementDateService.computeSettlementDateNow();
    Transfer t =
        createApprovedTransfer(
            today, new BigDecimal("100.00"), PLACEHOLDER_IMAGE, PLACEHOLDER_IMAGE);
    transferRepository.save(t);

    when(settlementFileService.generateSettlementFile())
        .thenThrow(
            new SettlementFileService.SettlementFileGenerationException(
                "Failed to generate settlement file", new RuntimeException("disk full")));

    var listAppender = new ListAppender<ILoggingEvent>();
    listAppender.start();
    var logger = (Logger) LoggerFactory.getLogger(EodSchedulerService.class);
    logger.addAppender(listAppender);

    try {
      eodSchedulerService.runEodBatch();

      boolean hasMissingLog =
          listAppender.list.stream()
              .anyMatch(
                  e ->
                      Level.WARN.equals(e.getLevel())
                          && e.getFormattedMessage().contains("SETTLEMENT_FILE_MISSING"));
      assertThat(hasMissingLog).isTrue();
    } finally {
      logger.detachAppender(listAppender);
    }
  }

  private Transfer createApprovedTransfer(
      LocalDate settlementDate, BigDecimal amount, byte[] frontImage, byte[] backImage) {
    return new Transfer(
        UUID.randomUUID(),
        frontImage,
        backImage,
        amount,
        "INT-12345678",
        "OMN-999",
        TransferState.APPROVED,
        0.95,
        "02100002112345678901",
        0.99,
        amount,
        "INDIVIDUAL",
        null,
        settlementDate);
  }
}
