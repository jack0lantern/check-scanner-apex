package com.apexfintech.checkdeposit.ledger;

import com.apexfintech.checkdeposit.domain.AuditLog;
import com.apexfintech.checkdeposit.domain.LedgerEntry;
import com.apexfintech.checkdeposit.domain.Transfer;
import com.apexfintech.checkdeposit.domain.TransferState;
import com.apexfintech.checkdeposit.deposit.TransferNotReturnableException;
import com.apexfintech.checkdeposit.deposit.TransferNotFoundException;
import com.apexfintech.checkdeposit.repository.AuditLogRepository;
import com.apexfintech.checkdeposit.repository.LedgerEntryRepository;
import com.apexfintech.checkdeposit.repository.TransferRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handles return notifications: creates reversal ledger entries, fee entry, transitions transfer to
 * RETURNED, and writes INVESTOR_NOTIFIED audit event.
 */
@Service
public class ReturnService {

  private static final String DEBIT = "DEBIT";
  private static final String CREDIT = "CREDIT";
  private static final String INVESTOR_NOTIFIED = "INVESTOR_NOTIFIED";

  private final TransferRepository transferRepository;
  private final LedgerEntryRepository ledgerEntryRepository;
  private final AuditLogRepository auditLogRepository;
  private final BigDecimal returnFeeAmount;
  private final ObjectMapper objectMapper;

  public ReturnService(
      TransferRepository transferRepository,
      LedgerEntryRepository ledgerEntryRepository,
      AuditLogRepository auditLogRepository,
      @Value("${return.fee-amount:30}") BigDecimal returnFeeAmount,
      ObjectMapper objectMapper) {
    this.transferRepository = transferRepository;
    this.ledgerEntryRepository = ledgerEntryRepository;
    this.auditLogRepository = auditLogRepository;
    this.returnFeeAmount = returnFeeAmount;
    this.objectMapper = objectMapper;
  }

  /**
   * Processes a return notification. Atomically: creates two reversal ledger entries (debit
   * investor, credit omnibus), one fee entry (debit investor $30), updates transfer state to
   * RETURNED, and writes INVESTOR_NOTIFIED to audit_logs.
   *
   * @param transferId the transfer to return
   * @param returnReason reason for the return
   * @throws TransferNotFoundException if the transfer does not exist
   * @throws IllegalStateException if the transfer is not in APPROVED state
   */
  @Transactional
  public void processReturn(UUID transferId, String returnReason) {
    Transfer transfer =
        transferRepository
            .findById(transferId)
            .orElseThrow(() -> new TransferNotFoundException(transferId));

    if (transfer.getState() != TransferState.APPROVED) {
      throw new TransferNotReturnableException(
          transferId, "transfer is not in APPROVED state");
    }

    Instant now = Instant.now();
    UUID reversalTransactionId = UUID.randomUUID();
    UUID feeTransactionId = UUID.randomUUID();

    // Reversal: mirror of original posting — debit investor, credit omnibus
    LedgerEntry reversalDebit =
        new LedgerEntry(
            UUID.randomUUID(),
            transfer.getToAccountId(),
            reversalTransactionId,
            DEBIT,
            transfer.getAmount(),
            transfer.getFromAccountId(),
            now);
    LedgerEntry reversalCredit =
        new LedgerEntry(
            UUID.randomUUID(),
            transfer.getFromAccountId(),
            reversalTransactionId,
            CREDIT,
            transfer.getAmount(),
            transfer.getToAccountId(),
            now);

    // Fee: debit investor $30
    LedgerEntry feeEntry =
        new LedgerEntry(
            UUID.randomUUID(),
            transfer.getToAccountId(),
            feeTransactionId,
            DEBIT,
            returnFeeAmount,
            null,
            now);

    ledgerEntryRepository.save(reversalDebit);
    ledgerEntryRepository.save(reversalCredit);
    ledgerEntryRepository.save(feeEntry);

    transfer.setState(TransferState.RETURNED);
    transfer.setUpdatedAt(now);
    transferRepository.save(transfer);

    // INVESTOR_NOTIFIED audit event: transferId, returnReason, feeAmount=$30, timestamp
    String detail;
    try {
      detail =
          objectMapper.writeValueAsString(
              Map.of(
                  "returnReason", returnReason,
                  "feeAmount", returnFeeAmount,
                  "timestamp", now.toString()));
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("Failed to serialize audit detail", e);
    }

    AuditLog auditEntry =
        new AuditLog(
            UUID.randomUUID(),
            null,
            INVESTOR_NOTIFIED,
            transferId,
            detail,
            now);
    auditLogRepository.save(auditEntry);
  }

  /** Net investor impact = original amount + fee (both are debits = deductions). */
  public BigDecimal computeInvestorNetImpact(BigDecimal originalAmount) {
    return originalAmount.add(returnFeeAmount);
  }
}
