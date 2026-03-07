package com.apexfintech.checkdeposit.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ledger_entries")
public class LedgerEntry {

  @Id
  private UUID id;

  @Column(name = "account_id", nullable = false)
  private String accountId;

  @Column(name = "transaction_id", nullable = false)
  private UUID transactionId;

  @Column(nullable = false, length = 20)
  private String type;

  @Column(nullable = false, precision = 19, scale = 4)
  private BigDecimal amount;

  @Column(name = "counterparty_account_id")
  private String counterpartyAccountId;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  protected LedgerEntry() {}

  public LedgerEntry(
      UUID id,
      String accountId,
      UUID transactionId,
      String type,
      BigDecimal amount,
      String counterpartyAccountId,
      Instant createdAt) {
    this.id = id;
    this.accountId = accountId;
    this.transactionId = transactionId;
    this.type = type;
    this.amount = amount;
    this.counterpartyAccountId = counterpartyAccountId;
    this.createdAt = createdAt;
  }

  public UUID getId() {
    return id;
  }

  public String getAccountId() {
    return accountId;
  }

  public UUID getTransactionId() {
    return transactionId;
  }

  public String getType() {
    return type;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public String getCounterpartyAccountId() {
    return counterpartyAccountId;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
