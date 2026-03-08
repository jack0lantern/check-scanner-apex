package com.apexfintech.checkdeposit.settlement;

import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Scheduled task representing the 6:30 PM CT End-of-Day cutoff. Runs daily at the configured
 * time. Settlement file generation and batch logic will be implemented in Phase 6 Steps 2–4.
 */
@Service
public class EodSchedulerService {

  private static final Logger log = LoggerFactory.getLogger(EodSchedulerService.class);

  private final AtomicInteger executionCount = new AtomicInteger(0);

  @Scheduled(
      cron = "${eod.cron:0 30 18 * * *}",
      zone = "${eod.zone:America/Chicago}")
  public void runEodBatch() {
    executionCount.incrementAndGet();
    log.info("EOD batch triggered (6:30 PM CT cutoff)");
    // Settlement file generation and batch processing will be implemented in Phase 6 Steps 2–4
  }

  /**
   * Returns the number of times the EOD batch has been triggered. Used for test verification.
   */
  public int getExecutionCount() {
    return executionCount.get();
  }
}
