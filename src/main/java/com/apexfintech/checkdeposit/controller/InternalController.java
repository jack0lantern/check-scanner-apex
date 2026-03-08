package com.apexfintech.checkdeposit.controller;

import com.apexfintech.checkdeposit.auth.AuthContextHolder;
import com.apexfintech.checkdeposit.dto.ReturnNotificationRequest;
import com.apexfintech.checkdeposit.dto.SettlementAckRequest;
import com.apexfintech.checkdeposit.ledger.ReturnService;
import com.apexfintech.checkdeposit.settlement.SettlementAckService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal")
public class InternalController {

  private static final String OPERATOR = "OPERATOR";

  private final ReturnService returnService;
  private final SettlementAckService settlementAckService;

  public InternalController(
      ReturnService returnService, SettlementAckService settlementAckService) {
    this.returnService = returnService;
    this.settlementAckService = settlementAckService;
  }

  /**
   * Simulates an inbound return notification from the Settlement Bank. Requires X-User-Role:
   * OPERATOR.
   */
  @PostMapping("/returns")
  public ResponseEntity<Void> processReturn(@RequestBody ReturnNotificationRequest request) {
    var auth = AuthContextHolder.get();
    if (auth == null || !OPERATOR.equals(auth.userRole())) {
      return ResponseEntity.status(403).build();
    }
    if (request == null || request.transferId() == null || request.returnReason() == null || request.returnReason().isBlank()) {
      return ResponseEntity.badRequest().build();
    }

    returnService.processReturn(request.transferId(), request.returnReason());
    return ResponseEntity.ok().build();
  }

  /**
   * Accepts settlement bank acknowledgment. Stores ack status on the batch record. Requires
   * X-User-Role: OPERATOR.
   *
   * <p>Payload: { "batchId": "uuid", "status": "ACCEPTED" | "REJECTED", "details": "..." }
   */
  @PostMapping("/settlement/ack")
  public ResponseEntity<Void> settlementAck(@RequestBody SettlementAckRequest request) {
    var auth = AuthContextHolder.get();
    if (auth == null || !OPERATOR.equals(auth.userRole())) {
      return ResponseEntity.status(403).build();
    }
    if (request == null || request.batchId() == null || request.status() == null) {
      return ResponseEntity.badRequest().build();
    }

    var updated = settlementAckService.recordAck(request);
    return updated.isPresent() ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
  }
}
