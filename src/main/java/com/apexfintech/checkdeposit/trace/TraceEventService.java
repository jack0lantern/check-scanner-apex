package com.apexfintech.checkdeposit.trace;

import com.apexfintech.checkdeposit.domain.TraceEvent;
import com.apexfintech.checkdeposit.domain.TraceStage;
import com.apexfintech.checkdeposit.dto.TraceEventResponse;
import com.apexfintech.checkdeposit.repository.TraceEventRepository;
import com.apexfintech.checkdeposit.repository.TransferRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TraceEventService {

  private final TraceEventRepository traceEventRepository;
  private final TransferRepository transferRepository;
  private final ObjectMapper objectMapper;

  public TraceEventService(
      TraceEventRepository traceEventRepository,
      TransferRepository transferRepository,
      ObjectMapper objectMapper) {
    this.traceEventRepository = traceEventRepository;
    this.transferRepository = transferRepository;
    this.objectMapper = objectMapper;
  }

  /**
   * Records a trace event for a deposit decision point. Call from components as they process the
   * deposit.
   */
  @Transactional
  public void record(UUID transferId, TraceStage stage, String outcome, Object detail) {
    String detailJson = serializeDetail(detail);
    TraceEvent event =
        new TraceEvent(UUID.randomUUID(), transferId, stage, outcome, detailJson, Instant.now());
    traceEventRepository.save(event);
  }

  /** Returns chronological trace events for a transfer. Throws if transfer does not exist. */
  public List<TraceEventResponse> getTrace(UUID transferId) {
    if (!transferRepository.existsById(transferId)) {
      throw new com.apexfintech.checkdeposit.deposit.TransferNotFoundException(transferId);
    }
    List<TraceEvent> events = traceEventRepository.findByTransferIdOrderByCreatedAtAsc(transferId);
    return events.stream().map(this::toResponse).toList();
  }

  private TraceEventResponse toResponse(TraceEvent e) {
    JsonNode detailNode = parseDetail(e.getDetail());
    return new TraceEventResponse(e.getStage(), e.getOutcome(), detailNode, e.getCreatedAt());
  }

  private String serializeDetail(Object detail) {
    if (detail == null) {
      return "{}";
    }
    if (detail instanceof String s) {
      return s.isEmpty() ? "{}" : s;
    }
    try {
      return objectMapper.writeValueAsString(detail);
    } catch (JsonProcessingException ex) {
      return "{\"error\":\"serialization failed\"}";
    }
  }

  private JsonNode parseDetail(String detail) {
    if (detail == null || detail.isBlank()) {
      return objectMapper.createObjectNode();
    }
    try {
      return objectMapper.readTree(detail);
    } catch (JsonProcessingException ex) {
      return objectMapper.createObjectNode().put("raw", detail);
    }
  }
}
