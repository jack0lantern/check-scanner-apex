package com.apexfintech.checkdeposit.settlement;

import com.apexfintech.checkdeposit.domain.SettlementBatch;
import com.apexfintech.checkdeposit.domain.SettlementBatch.AckStatus;
import com.apexfintech.checkdeposit.dto.SettlementAckRequest;
import com.apexfintech.checkdeposit.repository.SettlementBatchRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Processes settlement bank acknowledgments. Updates the batch record with ack status, details, and
 * timestamp.
 */
@Service
public class SettlementAckService {

  private final SettlementBatchRepository settlementBatchRepository;

  public SettlementAckService(SettlementBatchRepository settlementBatchRepository) {
    this.settlementBatchRepository = settlementBatchRepository;
  }

  /**
   * Records an acknowledgment for a settlement batch.
   *
   * @param request the ack request containing batchId, status (ACCEPTED/REJECTED), and optional
   *     details
   * @return the updated batch if found, empty if batch does not exist
   */
  @Transactional
  public Optional<SettlementBatch> recordAck(SettlementAckRequest request) {
    if (request == null || request.batchId() == null || request.status() == null) {
      return Optional.empty();
    }

    return settlementBatchRepository
        .findById(request.batchId())
        .map(
            batch -> {
              batch.setAckStatus(request.status());
              batch.setAckDetails(request.details());
              batch.setAckTimestamp(Instant.now());
              return settlementBatchRepository.save(batch);
            });
  }

  public Optional<SettlementBatch> findByBatchId(UUID batchId) {
    return settlementBatchRepository.findById(batchId);
  }
}
