package com.apexfintech.checkdeposit.settlement;

import static org.assertj.core.api.Assertions.assertThat;

import com.apexfintech.checkdeposit.domain.Transfer;
import com.apexfintech.checkdeposit.domain.TransferState;
import com.apexfintech.checkdeposit.repository.TransferRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Integration test for settlement file generation. Creates 3 approved transfers with
 * settlementDate = today, triggers the service, and asserts: (1) file created on disk, (2) file
 * contains exactly 3 records, (3) each record has non-null micrData and image references, (4) batch
 * totalAmount equals sum of the 3 amounts, (5) all 3 transfers are in COMPLETED state.
 */
@SpringBootTest(properties = "spring.task.scheduling.enabled=false")
@ActiveProfiles("test")
class SettlementFileGenerationIntegrationTest {

  @Autowired private SettlementFileService settlementFileService;
  @Autowired private SettlementDateService settlementDateService;
  @Autowired private TransferRepository transferRepository;

  @BeforeEach
  void setUp() {
    transferRepository.deleteAll();
  }

  @Test
  void generateSettlementFile_createsFileWithThreeRecordsAndMarksTransfersCompleted() {
    LocalDate today = settlementDateService.computeSettlementDateNow();
    byte[] frontImage = new byte[] {0x01, 0x02, 0x03};
    byte[] backImage = new byte[] {0x04, 0x05, 0x06};

    Transfer t1 = createApprovedTransfer(today, new BigDecimal("100.00"), frontImage, backImage);
    Transfer t2 = createApprovedTransfer(today, new BigDecimal("250.50"), frontImage, backImage);
    Transfer t3 = createApprovedTransfer(today, new BigDecimal("75.25"), frontImage, backImage);

    transferRepository.saveAll(List.of(t1, t2, t3));

    Path file = settlementFileService.generateSettlementFile();

    assertThat(file).isNotNull();
    assertThat(Files.exists(file)).isTrue();

    JsonNode root = parseJson(file);
    assertThat(root.get("batchId")).isNotNull();
    assertThat(root.get("generationTimestamp")).isNotNull();
    assertThat(root.get("totalRecordCount").asInt()).isEqualTo(3);
    assertThat(root.get("totalAmount").asDouble()).isEqualTo(425.75);

    JsonNode records = root.get("records");
    assertThat(records).isNotNull();
    assertThat(records.isArray()).isTrue();
    assertThat(records.size()).isEqualTo(3);

    BigDecimal sum = BigDecimal.ZERO;
    for (int i = 0; i < 3; i++) {
      JsonNode rec = records.get(i);
      assertThat(rec.get("sequenceNumber").asInt()).isEqualTo(i + 1);
      assertThat(rec.get("micrData")).isNotNull();
      assertThat(rec.get("micrData").asText()).isNotBlank();
      assertThat(rec.get("frontImageRef")).isNotNull();
      assertThat(rec.get("frontImageRef").asText()).isNotBlank();
      assertThat(rec.get("backImageRef")).isNotNull();
      assertThat(rec.get("backImageRef").asText()).isNotBlank();
      sum = sum.add(BigDecimal.valueOf(rec.get("amount").asDouble()));
    }
    assertThat(sum).isEqualByComparingTo(new BigDecimal("425.75"));

    assertThat(transferRepository.findById(t1.getId()).orElseThrow().getState())
        .isEqualTo(TransferState.COMPLETED);
    assertThat(transferRepository.findById(t2.getId()).orElseThrow().getState())
        .isEqualTo(TransferState.COMPLETED);
    assertThat(transferRepository.findById(t3.getId()).orElseThrow().getState())
        .isEqualTo(TransferState.COMPLETED);
  }

  @Test
  void generateSettlementFile_withNoTransfers_returnsNull() {
    LocalDate today = settlementDateService.computeSettlementDateNow();
    List<Transfer> batch =
        transferRepository.findByStateAndSettlementDate(TransferState.APPROVED, today);
    assertThat(batch).isEmpty();

    Path file = settlementFileService.generateSettlementFile();

    assertThat(file).isNull();
  }

  private Transfer createApprovedTransfer(
      LocalDate settlementDate, BigDecimal amount, byte[] frontImage, byte[] backImage) {
    return new Transfer(
        UUID.randomUUID(),
        frontImage,
        backImage,
        amount,
        "INT-12345678",
        "OMN-999",
        TransferState.APPROVED,
        0.95,
        "02100002112345678901",
        0.99,
        amount,
        "INDIVIDUAL",
        null,
        settlementDate);
  }

  private JsonNode parseJson(Path file) {
    try {
      return new ObjectMapper().readTree(Files.readString(file));
    } catch (Exception e) {
      throw new RuntimeException("Failed to parse settlement JSON", e);
    }
  }
}
