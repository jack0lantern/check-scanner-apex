package com.apexfintech.checkdeposit.controller;

import com.apexfintech.checkdeposit.auth.AuthContextHolder;
import com.apexfintech.checkdeposit.deposit.DepositService;
import com.apexfintech.checkdeposit.dto.DepositRequest;
import com.apexfintech.checkdeposit.dto.DepositResponse;
import com.apexfintech.checkdeposit.dto.IqaFailureResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/deposits")
public class DepositController {

  private final DepositService depositService;

  public DepositController(DepositService depositService) {
    this.depositService = depositService;
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
}
