package com.apexfintech.checkdeposit.controller;

import com.apexfintech.checkdeposit.auth.AuthContextHolder;
import com.apexfintech.checkdeposit.dto.ApproveRequest;
import com.apexfintech.checkdeposit.dto.OperatorActionDto;
import com.apexfintech.checkdeposit.dto.RejectRequest;
import com.apexfintech.checkdeposit.operator.OperatorService;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
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

  public OperatorController(OperatorService operatorService) {
    this.operatorService = operatorService;
  }

  @GetMapping("/actions")
  public ResponseEntity<List<OperatorActionDto>> getPastActions(
      @RequestParam(defaultValue = "100") int limit,
      @RequestParam(required = false) String action,
      @RequestParam(required = false) String dateFrom,
      @RequestParam(required = false) String dateTo,
      @RequestParam(required = false) String accountId,
      @RequestParam(required = false) String minAmount,
      @RequestParam(required = false) String maxAmount) {
    BigDecimal min = parseBigDecimal(minAmount);
    BigDecimal max = parseBigDecimal(maxAmount);
    return ResponseEntity.ok(
        operatorService.getPastActions(
            limit, action, dateFrom, dateTo, accountId, min, max));
  }

  @GetMapping("/queue")
  public ResponseEntity<?> getQueue(
      @RequestParam(required = false) String dateFrom,
      @RequestParam(required = false) String dateTo,
      @RequestParam(required = false) String accountId,
      @RequestParam(required = false) String minAmount,
      @RequestParam(required = false) String maxAmount) {
    BigDecimal min = parseBigDecimal(minAmount);
    BigDecimal max = parseBigDecimal(maxAmount);
    return ResponseEntity.ok(
        operatorService.getQueue(null, dateFrom, dateTo, accountId, min, max));
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
