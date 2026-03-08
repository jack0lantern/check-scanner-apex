package com.apexfintech.checkdeposit.repository;

import com.apexfintech.checkdeposit.domain.SettlementBatch;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface SettlementBatchRepository extends JpaRepository<SettlementBatch, UUID> {

  @Query("SELECT b FROM SettlementBatch b WHERE b.ackStatus IS NULL ORDER BY b.generationTimestamp ASC")
  List<SettlementBatch> findUnacknowledgedBatches();

  @Query(
      "SELECT b FROM SettlementBatch b WHERE b.ackStatus IS NULL AND b.generationTimestamp < :before ORDER BY b.generationTimestamp ASC")
  List<SettlementBatch> findUnacknowledgedBatchesOlderThan(Instant before);
}
