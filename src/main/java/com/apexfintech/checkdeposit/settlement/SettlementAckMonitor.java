package com.apexfintech.checkdeposit.settlement;

import com.apexfintech.checkdeposit.domain.SettlementBatch;
import com.apexfintech.checkdeposit.repository.SettlementBatchRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Monitors settlement batches for missing bank acknowledgments. Logs a SETTLEMENT_ACK_TIMEOUT
 * warning for each batch that has not received an ack within the configured timeout after file
 * generation.
 */
@Service
public class SettlementAckMonitor {

  private static final Logger log = LoggerFactory.getLogger(SettlementAckMonitor.class);
  public static final String SETTLEMENT_ACK_TIMEOUT = "SETTLEMENT_ACK_TIMEOUT";

  private final SettlementBatchRepository settlementBatchRepository;
  private final Duration ackTimeout;

  public SettlementAckMonitor(
      SettlementBatchRepository settlementBatchRepository,
      @Value("${settlement.ack-timeout-minutes:60}") int ackTimeoutMinutes) {
    this.settlementBatchRepository = settlementBatchRepository;
    this.ackTimeout = Duration.ofMinutes(ackTimeoutMinutes);
  }

  /**
   * Checks for unacknowledged batches older than the configured timeout and logs a warning for
   * each. Runs every 10 minutes.
   */
  @Scheduled(
      cron = "${settlement.ack-monitor.cron:0 */10 * * * *}",
      zone = "${eod.zone:America/Chicago}")
  public void checkForAckTimeout() {
    Instant cutoff = Instant.now().minus(ackTimeout);
    List<SettlementBatch> overdue =
        settlementBatchRepository.findUnacknowledgedBatchesOlderThan(cutoff);

    for (SettlementBatch batch : overdue) {
      log.warn(
          "{} batchId={} generationTimestamp={} totalRecordCount={} totalAmount={}",
          SETTLEMENT_ACK_TIMEOUT,
          batch.getBatchId(),
          batch.getGenerationTimestamp(),
          batch.getTotalRecordCount(),
          batch.getTotalAmount());
    }
  }
}
