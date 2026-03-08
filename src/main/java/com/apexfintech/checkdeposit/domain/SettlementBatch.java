package com.apexfintech.checkdeposit.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "settlement_batch")
public class SettlementBatch {

  @Id
  @Column(name = "batch_id", nullable = false)
  private UUID batchId;

  @Column(name = "generation_timestamp", nullable = false)
  private Instant generationTimestamp;

  @Column(name = "total_record_count", nullable = false)
  private int totalRecordCount;

  @Column(name = "total_amount", nullable = false, precision = 19, scale = 4)
  private BigDecimal totalAmount;

  @Column(name = "ack_status")
  @Enumerated(EnumType.STRING)
  private AckStatus ackStatus;

  @Column(name = "ack_details")
  private String ackDetails;

  @Column(name = "ack_timestamp")
  private Instant ackTimestamp;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  public enum AckStatus {
    ACCEPTED,
    REJECTED
  }

  protected SettlementBatch() {}

  public SettlementBatch(
      UUID batchId,
      Instant generationTimestamp,
      int totalRecordCount,
      BigDecimal totalAmount) {
    this.batchId = batchId;
    this.generationTimestamp = generationTimestamp;
    this.totalRecordCount = totalRecordCount;
    this.totalAmount = totalAmount;
    this.ackStatus = null;
    this.ackDetails = null;
    this.ackTimestamp = null;
    this.createdAt = Instant.now();
  }

  public UUID getBatchId() {
    return batchId;
  }

  public Instant getGenerationTimestamp() {
    return generationTimestamp;
  }

  public int getTotalRecordCount() {
    return totalRecordCount;
  }

  public BigDecimal getTotalAmount() {
    return totalAmount;
  }

  public AckStatus getAckStatus() {
    return ackStatus;
  }

  public void setAckStatus(AckStatus ackStatus) {
    this.ackStatus = ackStatus;
  }

  public String getAckDetails() {
    return ackDetails;
  }

  public void setAckDetails(String ackDetails) {
    this.ackDetails = ackDetails;
  }

  public Instant getAckTimestamp() {
    return ackTimestamp;
  }

  public void setAckTimestamp(Instant ackTimestamp) {
    this.ackTimestamp = ackTimestamp;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
