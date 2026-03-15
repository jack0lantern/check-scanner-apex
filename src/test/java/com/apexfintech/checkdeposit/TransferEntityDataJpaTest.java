package com.apexfintech.checkdeposit;

import static org.assertj.core.api.Assertions.assertThat;

import com.apexfintech.checkdeposit.domain.Transfer;
import com.apexfintech.checkdeposit.domain.TransferState;
import com.apexfintech.checkdeposit.repository.TransferRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
class TransferEntityDataJpaTest {

  @Autowired private TransferRepository transferRepository;

  @Test
  void savesAndRetrievesTransferWithAllFieldsRoundTripping() {
    UUID id = UUID.randomUUID();
    byte[] frontImage = new byte[] {0x01, 0x02, 0x03};
    byte[] backImage = new byte[] {0x04, 0x05, 0x06};
    BigDecimal amount = new BigDecimal("123.45");
    String toAccountId = "INV-001";
    String fromAccountId = "OMN-001";
    TransferState state = TransferState.APPROVED;
    Double vendorScore = 0.95;
    String micrData = "12345678901234567890";
    Double micrConfidence = 0.88;
    BigDecimal ocrAmount = new BigDecimal("123.45");
    String contributionType = "INDIVIDUAL";
    String depositSource = "MOBILE";
    LocalDate settlementDate = LocalDate.of(2026, 3, 7);

    Transfer transfer =
        new Transfer(
            id,
            frontImage,
            backImage,
            amount,
            toAccountId,
            "INV-001",
            fromAccountId,
            state,
            vendorScore,
            micrData,
            micrConfidence,
            ocrAmount,
            contributionType,
            depositSource,
            settlementDate);

    Transfer saved = transferRepository.saveAndFlush(transfer);
    assertThat(saved.getId()).isEqualTo(id);

    Transfer retrieved = transferRepository.findById(id).orElseThrow();

    assertThat(retrieved.getId()).isEqualTo(id);
    assertThat(retrieved.getFrontImageData()).isEqualTo(frontImage);
    assertThat(retrieved.getBackImageData()).isEqualTo(backImage);
    assertThat(retrieved.getAmount()).isEqualByComparingTo(amount);
    assertThat(retrieved.getToAccountId()).isEqualTo(toAccountId);
    assertThat(retrieved.getFromAccountId()).isEqualTo(fromAccountId);
    assertThat(retrieved.getType()).isEqualTo("MOVEMENT");
    assertThat(retrieved.getMemo()).isEqualTo("FREE");
    assertThat(retrieved.getSubType()).isEqualTo("DEPOSIT");
    assertThat(retrieved.getTransferType()).isEqualTo("CHECK");
    assertThat(retrieved.getCurrency()).isEqualTo("USD");
    assertThat(retrieved.getSourceApplicationId()).isEqualTo(id.toString());
    assertThat(retrieved.getState()).isEqualTo(state);
    assertThat(retrieved.getVendorScore()).isEqualTo(vendorScore);
    assertThat(retrieved.getMicrData()).isEqualTo(micrData);
    assertThat(retrieved.getMicrConfidence()).isEqualTo(micrConfidence);
    assertThat(retrieved.getOcrAmount()).isEqualByComparingTo(ocrAmount);
    assertThat(retrieved.getContributionType()).isEqualTo(contributionType);
    assertThat(retrieved.getDepositSource()).isEqualTo(depositSource);
    assertThat(retrieved.getSettlementDate()).isEqualTo(settlementDate);
    assertThat(retrieved.getCreatedAt()).isNotNull();
    assertThat(retrieved.getUpdatedAt()).isNotNull();
  }
}
