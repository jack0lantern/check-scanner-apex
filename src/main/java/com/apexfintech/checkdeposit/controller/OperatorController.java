package com.apexfintech.checkdeposit.controller;

import com.apexfintech.checkdeposit.auth.AuthContextHolder;
import com.apexfintech.checkdeposit.domain.AuditLog;
import com.apexfintech.checkdeposit.domain.TransferState;
import com.apexfintech.checkdeposit.dto.ApproveRequest;
import com.apexfintech.checkdeposit.dto.OperatorActionDto;
import com.apexfintech.checkdeposit.dto.RejectRequest;
import com.apexfintech.checkdeposit.operator.OperatorService;
import com.apexfintech.checkdeposit.repository.AuditLogRepository;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/operator")
public class OperatorController {

  private static final int ACTIONS_PAGE_SIZE = 100;

  private final OperatorService operatorService;
  private final AuditLogRepository auditLogRepository;

  public OperatorController(OperatorService operatorService, AuditLogRepository auditLogRepository) {
    this.operatorService = operatorService;
    this.auditLogRepository = auditLogRepository;
  }

  @GetMapping("/actions")
  public ResponseEntity<List<OperatorActionDto>> getPastActions(
      @RequestParam(defaultValue = "100") int limit) {
    int size = Math.min(Math.max(1, limit), 200);
    List<AuditLog> logs =
        auditLogRepository.findOperatorActionsOrderByCreatedAtDesc(PageRequest.of(0, size));
    List<OperatorActionDto> dtos =
        logs.stream()
            .map(
                a ->
                    new OperatorActionDto(
                        a.getId(),
                        a.getOperatorId(),
                        a.getAction(),
                        a.getTransferId(),
                        a.getDetail(),
                        a.getCreatedAt()))
            .toList();
    return ResponseEntity.ok(dtos);
  }

  @GetMapping("/queue")
  public ResponseEntity<?> getQueue(
      @RequestParam(required = false) String status,
      @RequestParam(required = false) String dateFrom,
      @RequestParam(required = false) String dateTo,
      @RequestParam(required = false) String accountId,
      @RequestParam(required = false) String minAmount,
      @RequestParam(required = false) String maxAmount) {
    TransferState statusEnum = parseTransferState(status);
    BigDecimal min = parseBigDecimal(minAmount);
    BigDecimal max = parseBigDecimal(maxAmount);
    return ResponseEntity.ok(
        operatorService.getQueue(statusEnum, dateFrom, dateTo, accountId, min, max));
  }

  private static TransferState parseTransferState(String s) {
    if (s == null || s.isBlank()) return null;
    try {
      return TransferState.valueOf(s.trim().toUpperCase());
    } catch (IllegalArgumentException e) {
      return null;
    }
  }

  private static BigDecimal parseBigDecimal(String s) {
    if (s == null || s.isBlank()) return null;
    try {
      return new BigDecimal(s.trim());
    } catch (NumberFormatException e) {
      return null;
    }
  }

  @PostMapping("/queue/{transferId}/approve")
  public ResponseEntity<Void> approve(
      @PathVariable UUID transferId, @RequestBody(required = false) ApproveRequest request) {
    String operatorId = AuthContextHolder.get().accountId();
    operatorService.approve(transferId, request, operatorId);
    return ResponseEntity.ok().build();
  }

  @PostMapping("/queue/{transferId}/reject")
  public ResponseEntity<Void> reject(
      @PathVariable UUID transferId, @Valid @RequestBody RejectRequest request) {
    String operatorId = AuthContextHolder.get().accountId();
    operatorService.reject(transferId, request, operatorId);
    return ResponseEntity.ok().build();
  }
}
