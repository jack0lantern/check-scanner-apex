package com.apexfintech.checkdeposit.settlement;

import static org.assertj.core.api.Assertions.assertThat;

import com.apexfintech.checkdeposit.domain.SettlementBatch;
import com.apexfintech.checkdeposit.domain.SettlementBatch.AckStatus;
import com.apexfintech.checkdeposit.domain.Transfer;
import com.apexfintech.checkdeposit.domain.TransferState;
import com.apexfintech.checkdeposit.repository.SettlementBatchRepository;
import com.apexfintech.checkdeposit.repository.TransferRepository;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;

/**
 * Integration tests for settlement bank acknowledgment tracking. Test 1: trigger file generation;
 * POST ack with ACCEPTED; assert batch record ackStatus = ACCEPTED. Test 2: simulate timeout by not
 * posting an ack; assert SETTLEMENT_ACK_TIMEOUT warning-level log is emitted.
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = "spring.task.scheduling.enabled=false")
@ActiveProfiles("test")
class SettlementAckIntegrationTest {

  @Autowired private SettlementFileService settlementFileService;
  @Autowired private SettlementDateService settlementDateService;
  @Autowired private SettlementBatchRepository settlementBatchRepository;
  @Autowired private TransferRepository transferRepository;
  @Autowired private SettlementAckMonitor settlementAckMonitor;
  @Autowired private TestRestTemplate restTemplate;

  private static final byte[] PLACEHOLDER_IMAGE = new byte[] {0x01, 0x02, 0x03};

  @BeforeEach
  void setUp() {
    transferRepository.deleteAll();
    settlementBatchRepository.deleteAll();
  }

  @Test
  void postAckWithAccepted_updatesBatchRecordAckStatus() {
    LocalDate today = settlementDateService.computeSettlementDateNow();
    Transfer t =
        createApprovedTransfer(
            today, new BigDecimal("100.00"), PLACEHOLDER_IMAGE, PLACEHOLDER_IMAGE);
    transferRepository.save(t);

    settlementFileService.generateSettlementFile();

    List<SettlementBatch> batches = settlementBatchRepository.findAll();
    assertThat(batches).hasSize(1);
    UUID batchId = batches.get(0).getBatchId();
    assertThat(batches.get(0).getAckStatus()).isNull();

    var headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.set("X-User-Role", "OPERATOR");
    headers.set("X-Account-Id", "OP-001");
    var body =
        Map.of(
            "batchId", batchId.toString(),
            "status", "ACCEPTED",
            "details", "Batch processed successfully");

    var response =
        restTemplate.postForEntity(
            "/internal/settlement/ack", new HttpEntity<>(body, headers), Void.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

    SettlementBatch updated = settlementBatchRepository.findById(batchId).orElseThrow();
    assertThat(updated.getAckStatus()).isEqualTo(AckStatus.ACCEPTED);
    assertThat(updated.getAckDetails()).isEqualTo("Batch processed successfully");
    assertThat(updated.getAckTimestamp()).isNotNull();
  }

  @Test
  void ackTimeoutMonitor_logsSetttlementAckTimeoutWarning() {
    UUID batchId = UUID.randomUUID();
    SettlementBatch batch =
        new SettlementBatch(
            batchId, Instant.now().minus(Duration.ofHours(2)), 3, new BigDecimal("500.00"));
    settlementBatchRepository.save(batch);

    var listAppender =
        new ch.qos.logback.core.read.ListAppender<ch.qos.logback.classic.spi.ILoggingEvent>();
    listAppender.start();
    var logger =
        (ch.qos.logback.classic.Logger)
            org.slf4j.LoggerFactory.getLogger(SettlementAckMonitor.class);
    logger.addAppender(listAppender);

    try {
      settlementAckMonitor.checkForAckTimeout();

      boolean hasTimeoutLog =
          listAppender.list.stream()
              .anyMatch(
                  e ->
                      ch.qos.logback.classic.Level.WARN.equals(e.getLevel())
                          && e.getFormattedMessage()
                              .contains(SettlementAckMonitor.SETTLEMENT_ACK_TIMEOUT));
      assertThat(hasTimeoutLog).isTrue();
    } finally {
      logger.detachAppender(listAppender);
    }
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
}
