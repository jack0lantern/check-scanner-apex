package com.apexfintech.checkdeposit.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "trace_events")
public class TraceEvent {

  @Id
  private UUID id;

  @Column(name = "transfer_id", nullable = false)
  private UUID transferId;

  @Column(nullable = false, length = 50)
  @Enumerated(EnumType.STRING)
  private TraceStage stage;

  @Column(length = 50)
  private String outcome;

  @Column(name = "detail", columnDefinition = "TEXT")
  private String detail;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  protected TraceEvent() {}

  public TraceEvent(
      UUID id, UUID transferId, TraceStage stage, String outcome, String detail, Instant createdAt) {
    this.id = id;
    this.transferId = transferId;
    this.stage = stage;
    this.outcome = outcome;
    this.detail = detail;
    this.createdAt = createdAt;
  }

  public UUID getId() {
    return id;
  }

  public UUID getTransferId() {
    return transferId;
  }

  public TraceStage getStage() {
    return stage;
  }

  public String getOutcome() {
    return outcome;
  }

  public String getDetail() {
    return detail;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
