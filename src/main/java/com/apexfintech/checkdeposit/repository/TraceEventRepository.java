package com.apexfintech.checkdeposit.repository;

import com.apexfintech.checkdeposit.domain.TraceEvent;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TraceEventRepository extends JpaRepository<TraceEvent, UUID> {

  List<TraceEvent> findByTransferIdOrderByCreatedAtAsc(UUID transferId);
}
