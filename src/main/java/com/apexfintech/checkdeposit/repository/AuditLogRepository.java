package com.apexfintech.checkdeposit.repository;

import com.apexfintech.checkdeposit.domain.AuditLog;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

  List<AuditLog> findByTransferIdAndAction(UUID transferId, String action);
}
