package com.apexfintech.checkdeposit.ledger;

import static org.assertj.core.api.Assertions.assertThat;

import com.apexfintech.checkdeposit.domain.AuditLog;
import com.apexfintech.checkdeposit.domain.LedgerEntry;
import com.apexfintech.checkdeposit.domain.Transfer;
import com.apexfintech.checkdeposit.domain.TransferState;
import com.apexfintech.checkdeposit.repository.AuditLogRepository;
import com.apexfintech.checkdeposit.repository.LedgerEntryRepository;
import com.apexfintech.checkdeposit.repository.TransferRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Integration test: 3 new ledger_entries (2 reversal + 1 fee), state = RETURNED, INVESTOR_NOTIFIED
 * audit log entry present.
 */
@SpringBootTest
@ActiveProfiles("test")
class ReturnHandlingIntegrationTest {

  private static final String TO_ACCOUNT_ID = "INT-12345678";
  private static final String FROM_ACCOUNT_ID = "OMN-999";
  private static final String MICR_DATA = "02100002112345678901";
  private static final BigDecimal AMOUNT = new BigDecimal("150.50");
  private static final String RETURN_REASON = "NSF";

  @Autowired private LedgerPostingService ledgerPostingService;
  @Autowired private ReturnService returnService;
  @Autowired private TransferRepository transferRepository;
  @Autowired private LedgerEntryRepository ledgerEntryRepository;
  @Autowired private AuditLogRepository auditLogRepository;

  private Transfer transfer;

  @BeforeEach
  void setUp() {
    ledgerEntryRepository.deleteAll();
    auditLogRepository.deleteAll();
    transfer =
        transferRepository.save(
            new Transfer(
                UUID.randomUUID(),
                new byte[0],
                new byte[0],
                AMOUNT,
                TO_ACCOUNT_ID,
                FROM_ACCOUNT_ID,
                TransferState.ANALYZING,
                0.95,
                MICR_DATA,
                0.99,
                AMOUNT,
                "INDIVIDUAL",
                null,
                null));
    ledgerPostingService.postApprovedDeposit(transfer.getId());
  }

  @Test
  void processReturn_createsThreeLedgerEntries() {
    long countBefore = ledgerEntryRepository.count();

    returnService.processReturn(transfer.getId(), RETURN_REASON);

    assertThat(ledgerEntryRepository.count()).isEqualTo(countBefore + 3);
  }

  @Test
  void processReturn_updatesTransferStateToReturned() {
    returnService.processReturn(transfer.getId(), RETURN_REASON);

    Transfer updated = transferRepository.findById(transfer.getId()).orElseThrow();
    assertThat(updated.getState()).isEqualTo(TransferState.RETURNED);
  }

  @Test
  void processReturn_writesInvestorNotifiedAuditLog() {
    returnService.processReturn(transfer.getId(), RETURN_REASON);

    List<AuditLog> auditLogs =
        auditLogRepository.findByTransferIdAndAction(transfer.getId(), "INVESTOR_NOTIFIED");
    assertThat(auditLogs).hasSize(1);

    AuditLog entry = auditLogs.get(0);
    assertThat(entry.getTransferId()).isEqualTo(transfer.getId());
    assertThat(entry.getAction()).isEqualTo("INVESTOR_NOTIFIED");
    assertThat(entry.getDetail()).contains(RETURN_REASON);
    assertThat(entry.getDetail()).contains("30");
    assertThat(entry.getCreatedAt()).isNotNull();
  }

  @Test
  void processReturn_investorDebitedOriginalPlusFee_stateReturned_investorNotifiedAuditLog() {
    returnService.processReturn(transfer.getId(), RETURN_REASON);

    Transfer updated = transferRepository.findById(transfer.getId()).orElseThrow();
    assertThat(updated.getState()).isEqualTo(TransferState.RETURNED);

    List<LedgerEntry> investorDebits =
        ledgerEntryRepository.findAll().stream()
            .filter(e -> TO_ACCOUNT_ID.equals(e.getAccountId()) && "DEBIT".equals(e.getType()))
            .toList();
    assertThat(investorDebits).hasSize(2);
    var amounts = investorDebits.stream().map(LedgerEntry::getAmount).toList();
    assertThat(amounts).anyMatch(a -> a.compareTo(AMOUNT) == 0);
    assertThat(amounts).anyMatch(a -> a.compareTo(new BigDecimal("30")) == 0);

    List<AuditLog> auditLogs =
        auditLogRepository.findByTransferIdAndAction(transfer.getId(), "INVESTOR_NOTIFIED");
    assertThat(auditLogs).hasSize(1);
    assertThat(auditLogs.get(0).getDetail()).contains(RETURN_REASON).contains("30");
  }

  @Test
  void processReturn_reversalEntriesMirrorOriginalPosting() {
    returnService.processReturn(transfer.getId(), RETURN_REASON);

    List<LedgerEntry> investorDebits =
        ledgerEntryRepository.findAll().stream()
            .filter(e -> TO_ACCOUNT_ID.equals(e.getAccountId()) && "DEBIT".equals(e.getType()))
            .toList();

    // Investor should have: 1 reversal debit (original amount) + 1 fee debit ($30)
    assertThat(investorDebits).hasSize(2);
    var amounts = investorDebits.stream().map(LedgerEntry::getAmount).toList();
    assertThat(amounts).anyMatch(a -> a.compareTo(AMOUNT) == 0);
    assertThat(amounts).anyMatch(a -> a.compareTo(new BigDecimal("30")) == 0);

    List<LedgerEntry> omnibusCredits =
        ledgerEntryRepository.findAll().stream()
            .filter(e -> FROM_ACCOUNT_ID.equals(e.getAccountId()) && "CREDIT".equals(e.getType()))
            .toList();
    assertThat(omnibusCredits).hasSize(1);
    assertThat(omnibusCredits.get(0).getAmount()).isEqualByComparingTo(AMOUNT);
  }
}
