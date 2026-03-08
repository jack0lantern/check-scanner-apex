package com.apexfintech.checkdeposit.controller;

import com.apexfintech.checkdeposit.auth.AuthContextHolder;
import com.apexfintech.checkdeposit.domain.TransferState;
import com.apexfintech.checkdeposit.dto.ApproveRequest;
import com.apexfintech.checkdeposit.dto.RejectRequest;
import com.apexfintech.checkdeposit.operator.OperatorService;
import jakarta.validation.Valid;
import java.math.BigDecimal;
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

  private final OperatorService operatorService;

  public OperatorController(OperatorService operatorService) {
    this.operatorService = operatorService;
  }

  @GetMapping("/queue")
  public ResponseEntity<?> getQueue(
      @RequestParam(required = false) TransferState status,
      @RequestParam(required = false) String dateFrom,
      @RequestParam(required = false) String dateTo,
      @RequestParam(required = false) String accountId,
      @RequestParam(required = false) BigDecimal minAmount,
      @RequestParam(required = false) BigDecimal maxAmount) {
    return ResponseEntity.ok(
        operatorService.getQueue(status, dateFrom, dateTo, accountId, minAmount, maxAmount));
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
