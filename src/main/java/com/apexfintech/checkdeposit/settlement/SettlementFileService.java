package com.apexfintech.checkdeposit.settlement;

import com.apexfintech.checkdeposit.domain.SettlementBatch;
import com.apexfintech.checkdeposit.domain.TraceStage;
import com.apexfintech.checkdeposit.domain.Transfer;
import com.apexfintech.checkdeposit.domain.TransferState;
import com.apexfintech.checkdeposit.repository.SettlementBatchRepository;
import com.apexfintech.checkdeposit.repository.TransferRepository;
import com.apexfintech.checkdeposit.trace.TraceEventService;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Generates settlement files in X9 ICL–style JSON format. Queries APPROVED transfers with
 * settlementDate = today, writes a file with MICR data, image references, amounts, and batch
 * metadata, then updates all included transfers to COMPLETED atomically.
 */
@Service
public class SettlementFileService {

  private static final Logger log = LoggerFactory.getLogger(SettlementFileService.class);

  private final TransferRepository transferRepository;
  private final SettlementBatchRepository settlementBatchRepository;
  private final SettlementDateService settlementDateService;
  private final TraceEventService traceEventService;
  private final Path outputDir;

  public SettlementFileService(
      TransferRepository transferRepository,
      SettlementBatchRepository settlementBatchRepository,
      SettlementDateService settlementDateService,
      TraceEventService traceEventService,
      @Value("${settlement.output-path:./settlement-output}") String outputPath) {
    this.transferRepository = transferRepository;
    this.settlementBatchRepository = settlementBatchRepository;
    this.settlementDateService = settlementDateService;
    this.traceEventService = traceEventService;
    this.outputDir = Path.of(outputPath);
  }

  /**
   * Generates a settlement file for today's APPROVED transfers. Writes the file to disk, persists
   * the batch record for ack tracking, then updates all included transfers to COMPLETED in a
   * single transaction.
   *
   * @return the path of the generated file, or null if no transfers to settle
   */
  @Transactional
  public Path generateSettlementFile() {
    LocalDate today = settlementDateService.computeSettlementDateNow();
    List<Transfer> transfers =
        transferRepository.findByStateAndSettlementDate(TransferState.APPROVED, today);

    if (transfers.isEmpty()) {
      log.info("No APPROVED transfers for settlement date {}; skipping file generation", today);
      return null;
    }

    UUID batchId = UUID.randomUUID();
    Instant generationTimestamp = Instant.now();
    BigDecimal totalAmount =
        transfers.stream().map(Transfer::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

    String json = buildSettlementJson(transfers, batchId, generationTimestamp, totalAmount);

    try {
      Files.createDirectories(outputDir);
      Path filePath =
          outputDir.resolve(
              "settlement-%s-%s.json".formatted(today, batchId.toString().substring(0, 8)));
      Files.writeString(filePath, json);
      log.info(
          "Settlement file written: {} ({} records, totalAmount={})",
          filePath,
          transfers.size(),
          totalAmount);

      SettlementBatch batch =
          new SettlementBatch(batchId, generationTimestamp, transfers.size(), totalAmount);
      settlementBatchRepository.save(batch);

      markTransfersCompleted(transfers, batchId);
      return filePath;
    } catch (Exception e) {
      log.error("Failed to write settlement file for batch {}", batchId, e);
      throw new SettlementFileGenerationException("Failed to generate settlement file", e);
    }
  }

  private String buildSettlementJson(
      List<Transfer> transfers,
      UUID batchId,
      Instant generationTimestamp,
      BigDecimal totalAmount) {
    StringBuilder sb = new StringBuilder();
    sb.append("{\n");
    sb.append("  \"batchId\": \"").append(batchId).append("\",\n");
    sb.append("  \"generationTimestamp\": \"").append(generationTimestamp).append("\",\n");
    sb.append("  \"totalRecordCount\": ").append(transfers.size()).append(",\n");
    sb.append("  \"totalAmount\": ").append(totalAmount).append(",\n");
    sb.append("  \"records\": [\n");

    for (int i = 0; i < transfers.size(); i++) {
      Transfer t = transfers.get(i);
      int seq = i + 1;
      String frontRef = imageToBase64Ref(t.getFrontImageData());
      String backRef = imageToBase64Ref(t.getBackImageData());
      sb.append("    {\n");
      sb.append("      \"sequenceNumber\": ").append(seq).append(",\n");
      sb.append("      \"transferId\": \"").append(t.getId()).append("\",\n");
      sb.append("      \"micrData\": \"").append(escapeJson(t.getMicrData())).append("\",\n");
      sb.append("      \"frontImageRef\": \"").append(escapeJson(frontRef)).append("\",\n");
      sb.append("      \"backImageRef\": \"").append(escapeJson(backRef)).append("\",\n");
      sb.append("      \"amount\": ").append(t.getAmount()).append("\n");
      sb.append(i < transfers.size() - 1 ? "    },\n" : "    }\n");
    }

    sb.append("  ]\n");
    sb.append("}\n");
    return sb.toString();
  }

  private static String imageToBase64Ref(byte[] data) {
    if (data == null || data.length == 0) {
      return "";
    }
    return Base64.getEncoder().encodeToString(data);
  }

  private static String escapeJson(String s) {
    if (s == null) return "";
    return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
  }

  @Transactional
  protected void markTransfersCompleted(List<Transfer> transfers, UUID batchId) {
    Instant now = Instant.now();
    for (Transfer t : transfers) {
      t.setState(TransferState.COMPLETED);
      t.setUpdatedAt(now);
      traceEventService.record(
          t.getId(),
          TraceStage.SETTLEMENT,
          "INCLUDED",
          java.util.Map.of("batchId", batchId.toString()));
    }
    transferRepository.saveAll(transfers);
  }

  public static class SettlementFileGenerationException extends RuntimeException {
    public SettlementFileGenerationException(String message, Throwable cause) {
      super(message, cause);
    }
  }
}
