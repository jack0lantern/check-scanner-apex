package com.apexfintech.checkdeposit.controller;

import com.apexfintech.checkdeposit.domain.Transfer;
import com.apexfintech.checkdeposit.domain.TransferState;
import com.apexfintech.checkdeposit.dto.ResolvedAccount;
import com.apexfintech.checkdeposit.dto.VendorAssessmentResult;
import com.apexfintech.checkdeposit.funding.AccountResolutionService;
import com.apexfintech.checkdeposit.funding.MicrParser;
import com.apexfintech.checkdeposit.ledger.LedgerPostingService;
import com.apexfintech.checkdeposit.repository.TransferRepository;
import com.apexfintech.checkdeposit.settlement.SettlementDateService;
import com.apexfintech.checkdeposit.vendor.VendorService;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
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

  private static final byte[] PLACEHOLDER_IMAGE = new byte[] {0x01, 0x02, 0x03};
  private static final String ROUTING = "021000021";
  private static final String ACCOUNT = "12345678";

  private final AccountResolutionService accountResolutionService;
  private final VendorService vendorService;
  private final LedgerPostingService ledgerPostingService;
  private final TransferRepository transferRepository;
  private final SettlementDateService settlementDateService;

  public DebugController(
      AccountResolutionService accountResolutionService,
      VendorService vendorService,
      LedgerPostingService ledgerPostingService,
      TransferRepository transferRepository,
      SettlementDateService settlementDateService) {
    this.accountResolutionService = accountResolutionService;
    this.vendorService = vendorService;
    this.ledgerPostingService = ledgerPostingService;
    this.transferRepository = transferRepository;
    this.settlementDateService = settlementDateService;
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

  /**
   * Creates and approves a batch of deposits with settlementDate = today. Each deposit has unique
   * MICR (check number) and amount. Use for settlement file generation testing.
   *
   * <p>Use: POST /debug/batch-settlement-deposits?count=10&accountId=TEST001
   */
  @PostMapping("/batch-settlement-deposits")
  public ResponseEntity<Map<String, Object>> batchSettlementDeposits(
      @RequestParam(value = "count", defaultValue = "10") int count,
      @RequestParam(value = "accountId", defaultValue = "TEST001") String accountId) {
    ResolvedAccount resolved = accountResolutionService.resolve(accountId);
    java.time.LocalDate today = settlementDateService.computeSettlementDateNow();

    List<UUID> transferIds = new ArrayList<>();
    for (int i = 1; i <= count; i++) {
      String checkNum = String.format("%03d", i);
      String micrData = ROUTING + ACCOUNT + checkNum;
      BigDecimal amount = BigDecimal.valueOf(100 + i);

      Transfer transfer =
          new Transfer(
              UUID.randomUUID(),
              PLACEHOLDER_IMAGE,
              PLACEHOLDER_IMAGE,
              amount,
              resolved.internalNumber(),
              resolved.omnibusAccountId(),
              TransferState.ANALYZING,
              0.95,
              micrData,
              0.99,
              amount,
              "INDIVIDUAL",
              null,
              today);
      transfer.setMicrRoutingNumber(MicrParser.extractRoutingNumber(micrData));
      transfer.setMicrAccountNumber(MicrParser.extractAccountNumber(micrData));
      transfer.setMicrCheckNumber(MicrParser.extractCheckNumber(micrData));

      transferRepository.save(transfer);
      ledgerPostingService.postApprovedDeposit(transfer.getId());
      transferIds.add(transfer.getId());
    }

    return ResponseEntity.ok(
        Map.of(
            "count", count,
            "settlementDate", today.toString(),
            "transferIds", transferIds));
  }
}
