package com.apexfintech.checkdeposit.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_logs")
public class AuditLog {

  @Id
  private UUID id;

  @Column(name = "operator_id")
  private String operatorId;

  @Column(nullable = false, length = 50)
  private String action;

  @Column(name = "transfer_id")
  private UUID transferId;

  @Column(name = "detail", columnDefinition = "TEXT")
  private String detail;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  protected AuditLog() {}

  public AuditLog(UUID id, String operatorId, String action, UUID transferId, String detail, Instant createdAt) {
    this.id = id;
    this.operatorId = operatorId;
    this.action = action;
    this.transferId = transferId;
    this.detail = detail;
    this.createdAt = createdAt;
  }

  public UUID getId() {
    return id;
  }

  public String getOperatorId() {
    return operatorId;
  }

  public String getAction() {
    return action;
  }

  public UUID getTransferId() {
    return transferId;
  }

  public String getDetail() {
    return detail;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
