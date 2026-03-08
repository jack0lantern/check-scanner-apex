package com.apexfintech.checkdeposit.settlement;

import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Scheduled task representing the 6:30 PM CT End-of-Day cutoff. Runs daily at the configured
 * time. Triggers settlement file generation for APPROVED transfers with settlementDate = today.
 */
@Service
public class EodSchedulerService {

  private static final Logger log = LoggerFactory.getLogger(EodSchedulerService.class);

  private final SettlementFileService settlementFileService;
  private final AtomicInteger executionCount = new AtomicInteger(0);

  public EodSchedulerService(SettlementFileService settlementFileService) {
    this.settlementFileService = settlementFileService;
  }

  @Scheduled(
      cron = "${eod.cron:0 30 18 * * *}",
      zone = "${eod.zone:America/Chicago}")
  public void runEodBatch() {
    executionCount.incrementAndGet();
    log.info("EOD batch triggered (6:30 PM CT cutoff)");
    Path file = settlementFileService.generateSettlementFile();
    if (file != null) {
      log.info("Settlement file generated: {}", file);
    }
  }

  /**
   * Returns the number of times the EOD batch has been triggered. Used for test verification.
   */
  public int getExecutionCount() {
    return executionCount.get();
  }
}
