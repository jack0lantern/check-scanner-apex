package com.apexfintech.checkdeposit.controller;

import com.apexfintech.checkdeposit.auth.AuthContextHolder;
import com.apexfintech.checkdeposit.dto.ReturnNotificationRequest;
import com.apexfintech.checkdeposit.ledger.ReturnService;
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

  public InternalController(ReturnService returnService) {
    this.returnService = returnService;
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
}
