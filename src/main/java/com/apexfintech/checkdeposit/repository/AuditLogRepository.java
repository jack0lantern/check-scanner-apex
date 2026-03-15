package com.apexfintech.checkdeposit.repository;

import com.apexfintech.checkdeposit.domain.AuditLog;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

  List<AuditLog> findByTransferIdAndAction(UUID transferId, String action);

  @Query(
      "SELECT a FROM AuditLog a WHERE a.action IN ('APPROVE', 'REJECT', 'CONTRIBUTION_TYPE_OVERRIDE') ORDER BY a.createdAt DESC")
  List<AuditLog> findOperatorActionsOrderByCreatedAtDesc(Pageable pageable);

  @Query(
      "SELECT a FROM AuditLog a WHERE a.action = :action ORDER BY a.createdAt DESC")
  List<AuditLog> findOperatorActionsByActionOrderByCreatedAtDesc(
      @Param("action") String action, Pageable pageable);
}
