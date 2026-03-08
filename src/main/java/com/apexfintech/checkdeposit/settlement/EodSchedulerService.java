package com.apexfintech.checkdeposit.settlement;

import com.apexfintech.checkdeposit.domain.TransferState;
import com.apexfintech.checkdeposit.repository.TransferRepository;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Scheduled task representing the 6:30 PM CT End-of-Day cutoff. Runs daily at the configured
 * time. Triggers settlement file generation for APPROVED transfers with settlementDate = today.
 * If file generation fails while APPROVED transfers exist, logs a structured SETTLEMENT_FILE_MISSING
 * warning.
 */
@Service
public class EodSchedulerService {

  private static final Logger log = LoggerFactory.getLogger(EodSchedulerService.class);
  public static final String SETTLEMENT_FILE_MISSING = "SETTLEMENT_FILE_MISSING";

  private final SettlementFileService settlementFileService;
  private final TransferRepository transferRepository;
  private final SettlementDateService settlementDateService;
  private final AtomicInteger executionCount = new AtomicInteger(0);

  public EodSchedulerService(
      SettlementFileService settlementFileService,
      TransferRepository transferRepository,
      SettlementDateService settlementDateService) {
    this.settlementFileService = settlementFileService;
    this.transferRepository = transferRepository;
    this.settlementDateService = settlementDateService;
  }

  @Scheduled(
      cron = "${eod.cron:0 30 18 * * *}",
      zone = "${eod.zone:America/Chicago}")
  public void runEodBatch() {
    executionCount.incrementAndGet();
    log.info("EOD batch triggered (6:30 PM CT cutoff)");
    try {
      Path file = settlementFileService.generateSettlementFile();
      if (file != null) {
        log.info("Settlement file generated: {}", file);
      }
    } catch (SettlementFileService.SettlementFileGenerationException e) {
      LocalDate today = settlementDateService.computeSettlementDateNow();
      long approvedCount =
          transferRepository.findByStateAndSettlementDate(TransferState.APPROVED, today).size();
      if (approvedCount > 0) {
        log.warn(
            "{} settlementDate={} approvedTransferCount={} error={}",
            SETTLEMENT_FILE_MISSING,
            today,
            approvedCount,
            e.getMessage());
      }
    }
  }

  /**
   * Returns the number of times the EOD batch has been triggered. Used for test verification.
   */
  public int getExecutionCount() {
    return executionCount.get();
  }
}
