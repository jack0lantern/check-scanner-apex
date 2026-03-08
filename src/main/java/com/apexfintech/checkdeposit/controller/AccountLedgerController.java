package com.apexfintech.checkdeposit.controller;

import com.apexfintech.checkdeposit.dto.BalanceResponse;
import com.apexfintech.checkdeposit.dto.LedgerEntryResponse;
import com.apexfintech.checkdeposit.ledger.LedgerQueryService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/accounts")
public class AccountLedgerController {

  private final LedgerQueryService ledgerQueryService;

  public AccountLedgerController(LedgerQueryService ledgerQueryService) {
    this.ledgerQueryService = ledgerQueryService;
  }

  @GetMapping("/{accountId}/balance")
  public ResponseEntity<BalanceResponse> getBalance(@PathVariable String accountId) {
    return ResponseEntity.ok(ledgerQueryService.getBalance(accountId));
  }

  @GetMapping("/{accountId}/ledger")
  public ResponseEntity<Page<LedgerEntryResponse>> getLedger(
      @PathVariable String accountId,
      @PageableDefault(size = 20) Pageable pageable) {
    return ResponseEntity.ok(ledgerQueryService.getLedgerEntries(accountId, pageable));
  }
}
