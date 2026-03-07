package com.apexfintech.checkdeposit.controller;

import com.apexfintech.checkdeposit.dto.ResolvedAccount;
import com.apexfintech.checkdeposit.dto.VendorAssessmentResult;
import com.apexfintech.checkdeposit.funding.AccountResolutionService;
import com.apexfintech.checkdeposit.ledger.LedgerPostingService;
import com.apexfintech.checkdeposit.vendor.VendorService;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/debug")
public class DebugController {

  private final AccountResolutionService accountResolutionService;
  private final VendorService vendorService;
  private final LedgerPostingService ledgerPostingService;

  public DebugController(
      AccountResolutionService accountResolutionService,
      VendorService vendorService,
      LedgerPostingService ledgerPostingService) {
    this.accountResolutionService = accountResolutionService;
    this.vendorService = vendorService;
    this.ledgerPostingService = ledgerPostingService;
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

  /**
   * Temporary endpoint for manual testing of the vendor stub. Triggers different scenarios based on
   * accountId. Use accountId values: iqa-pass, iqa-blur, iqa-glare, micr-fail, duplicate,
   * amount-mismatch, clean-pass.
   */
  @GetMapping("/vendor-stub")
  public ResponseEntity<VendorAssessmentResult> vendorStub(
      @RequestParam(value = "accountId", defaultValue = "clean-pass") String accountId,
      @RequestParam(value = "amount", defaultValue = "100.00") BigDecimal amount) {
    VendorAssessmentResult result =
        vendorService.assessCheck(new byte[0], new byte[0], amount, accountId);
    return ResponseEntity.ok(result);
  }

  /**
   * Dummy endpoint for manual testing of ledger posting. Approves the transfer and posts to ledger.
   * Use: POST /debug/ledger-post?transferId=<uuid>
   */
  @PostMapping("/ledger-post")
  public ResponseEntity<Map<String, String>> ledgerPost(
      @RequestParam("transferId") UUID transferId) {
    ledgerPostingService.postApprovedDeposit(transferId);
    return ResponseEntity.ok(Map.of("status", "posted", "transferId", transferId.toString()));
  }
}
