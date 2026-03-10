package com.apexfintech.checkdeposit.controller;

import com.apexfintech.checkdeposit.auth.AuthContextHolder;
import com.apexfintech.checkdeposit.deposit.DepositService;
import com.apexfintech.checkdeposit.dto.DepositRequest;
import com.apexfintech.checkdeposit.dto.DepositResponse;
import com.apexfintech.checkdeposit.dto.IqaFailureResponse;
import com.apexfintech.checkdeposit.dto.TraceEventResponse;
import com.apexfintech.checkdeposit.dto.TransferStatusResponse;
import com.apexfintech.checkdeposit.trace.TraceEventService;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/deposits")
public class DepositController {

  private final DepositService depositService;
  private final TraceEventService traceEventService;

  public DepositController(DepositService depositService, TraceEventService traceEventService) {
    this.depositService = depositService;
    this.traceEventService = traceEventService;
  }

  @PostMapping
  public ResponseEntity<?> submit(@RequestBody DepositRequest request) {
    String scenarioAccountId =
        AuthContextHolder.get() != null ? AuthContextHolder.get().accountId() : "clean-pass";

    Object result = depositService.submit(request, scenarioAccountId);

    if (result instanceof IqaFailureResponse failure) {
      return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(failure);
    }
    return ResponseEntity.status(HttpStatus.CREATED).body((DepositResponse) result);
  }

  @GetMapping("/{transferId}")
  public ResponseEntity<TransferStatusResponse> getStatus(@PathVariable UUID transferId) {
    return ResponseEntity.ok(depositService.getStatus(transferId));
  }

  @GetMapping("/{transferId}/trace")
  public ResponseEntity<List<TraceEventResponse>> getTrace(@PathVariable UUID transferId) {
    return ResponseEntity.ok(traceEventService.getTrace(transferId));
  }
}
