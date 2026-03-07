package com.apexfintech.checkdeposit.controller;

import com.apexfintech.checkdeposit.dto.ResolvedAccount;
import com.apexfintech.checkdeposit.exception.AccountNotFoundException;
import com.apexfintech.checkdeposit.funding.AccountResolutionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/debug")
public class DebugController {

  private final AccountResolutionService accountResolutionService;

  public DebugController(AccountResolutionService accountResolutionService) {
    this.accountResolutionService = accountResolutionService;
  }

  @GetMapping("/account-resolve")
  public ResponseEntity<ResolvedAccount> resolveAccount(
      @RequestParam("accountId") String accountId) {
    ResolvedAccount resolved = accountResolutionService.resolve(accountId);
    return ResponseEntity.ok(resolved);
  }

  @GetMapping("/auth-test")
  public ResponseEntity<String> authTest() {
    return ResponseEntity.ok("OK");
  }
}
