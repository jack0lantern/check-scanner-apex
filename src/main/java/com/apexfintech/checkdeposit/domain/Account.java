package com.apexfintech.checkdeposit.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "accounts")
public class Account {

  @Id private UUID id;

  @Column(name = "external_id", nullable = false, unique = true)
  private String externalId;

  @Column(name = "internal_number", nullable = false)
  private String internalNumber;

  @Column(name = "routing_number", nullable = false)
  private String routingNumber;

  @Column(name = "micr_account_number")
  private String micrAccountNumber;

  @Column(name = "omnibus_id", nullable = false)
  private String omnibusId;

  @Column(name = "account_type")
  private String accountType;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected Account() {}

  public Account(
      UUID id,
      String externalId,
      String internalNumber,
      String routingNumber,
      String micrAccountNumber,
      String omnibusId,
      String accountType) {
    this.id = id;
    this.externalId = externalId;
    this.internalNumber = internalNumber;
    this.routingNumber = routingNumber;
    this.micrAccountNumber = micrAccountNumber;
    this.omnibusId = omnibusId;
    this.accountType = accountType;
    this.createdAt = Instant.now();
    this.updatedAt = Instant.now();
  }

  public UUID getId() {
    return id;
  }

  public String getExternalId() {
    return externalId;
  }

  public String getInternalNumber() {
    return internalNumber;
  }

  public String getRoutingNumber() {
    return routingNumber;
  }

  public String getMicrAccountNumber() {
    return micrAccountNumber;
  }

  public String getOmnibusId() {
    return omnibusId;
  }

  public String getAccountType() {
    return accountType;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }
}
