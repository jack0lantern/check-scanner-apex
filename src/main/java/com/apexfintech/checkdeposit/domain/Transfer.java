package com.apexfintech.checkdeposit.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "transfers")
public class Transfer {

  @Id
  private UUID id;

  @Lob
  @Column(name = "front_image_data")
  private byte[] frontImageData;

  @Lob
  @Column(name = "back_image_data")
  private byte[] backImageData;

  @Column(nullable = false, precision = 19, scale = 4)
  private BigDecimal amount;

  @Column(name = "to_account_id", nullable = false)
  private String toAccountId;

  @Column(name = "from_account_id", nullable = false)
  private String fromAccountId;

  @Column(nullable = false)
  private String type = "MOVEMENT";

  @Column(nullable = false)
  private String memo = "FREE";

  @Column(name = "sub_type", nullable = false)
  private String subType = "DEPOSIT";

  @Column(name = "transfer_type", nullable = false)
  private String transferType = "CHECK";

  @Column(nullable = false)
  private String currency = "USD";

  @Column(name = "source_application_id")
  private String sourceApplicationId;

  @Column(nullable = false)
  @Enumerated(EnumType.STRING)
  private TransferState state;

  @Column(name = "vendor_score")
  private Double vendorScore;

  @Column(name = "micr_data")
  private String micrData;

  @Column(name = "micr_confidence")
  private Double micrConfidence;

  @Column(name = "ocr_amount", precision = 19, scale = 4)
  private BigDecimal ocrAmount;

  @Column(name = "contribution_type")
  private String contributionType = "INDIVIDUAL";

  @Column(name = "deposit_source")
  private String depositSource;

  @Column(name = "settlement_date")
  private LocalDate settlementDate;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected Transfer() {}

  public Transfer(
      UUID id,
      byte[] frontImageData,
      byte[] backImageData,
      BigDecimal amount,
      String toAccountId,
      String fromAccountId,
      TransferState state,
      Double vendorScore,
      String micrData,
      Double micrConfidence,
      BigDecimal ocrAmount,
      String contributionType,
      String depositSource,
      LocalDate settlementDate) {
    this.id = id;
    this.frontImageData = frontImageData;
    this.backImageData = backImageData;
    this.amount = amount;
    this.toAccountId = toAccountId;
    this.fromAccountId = fromAccountId;
    this.state = state;
    this.vendorScore = vendorScore;
    this.micrData = micrData;
    this.micrConfidence = micrConfidence;
    this.ocrAmount = ocrAmount;
    this.contributionType = contributionType != null ? contributionType : "INDIVIDUAL";
    this.depositSource = depositSource;
    this.settlementDate = settlementDate;
    this.sourceApplicationId = id.toString();
    this.createdAt = Instant.now();
    this.updatedAt = Instant.now();
  }

  public UUID getId() {
    return id;
  }

  public byte[] getFrontImageData() {
    return frontImageData;
  }

  public byte[] getBackImageData() {
    return backImageData;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public String getToAccountId() {
    return toAccountId;
  }

  public String getFromAccountId() {
    return fromAccountId;
  }

  public String getType() {
    return type;
  }

  public String getMemo() {
    return memo;
  }

  public String getSubType() {
    return subType;
  }

  public String getTransferType() {
    return transferType;
  }

  public String getCurrency() {
    return currency;
  }

  public String getSourceApplicationId() {
    return sourceApplicationId;
  }

  public TransferState getState() {
    return state;
  }

  public Double getVendorScore() {
    return vendorScore;
  }

  public String getMicrData() {
    return micrData;
  }

  public Double getMicrConfidence() {
    return micrConfidence;
  }

  public BigDecimal getOcrAmount() {
    return ocrAmount;
  }

  public String getContributionType() {
    return contributionType;
  }

  public String getDepositSource() {
    return depositSource;
  }

  public LocalDate getSettlementDate() {
    return settlementDate;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }
}
