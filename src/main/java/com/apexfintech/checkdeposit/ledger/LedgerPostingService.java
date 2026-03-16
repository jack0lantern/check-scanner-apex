package com.apexfintech.checkdeposit.ledger;

import com.apexfintech.checkdeposit.deposit.TransferNotFoundException;
import com.apexfintech.checkdeposit.domain.LedgerEntry;
import com.apexfintech.checkdeposit.domain.Transfer;
import com.apexfintech.checkdeposit.domain.TransferState;
import com.apexfintech.checkdeposit.repository.LedgerEntryRepository;
import com.apexfintech.checkdeposit.repository.TransferRepository;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Posts approved deposits to the ledger. Updates the transfer to APPROVED and creates paired debit
 * and credit ledger entries.
 */
@Service
public class LedgerPostingService {

  private static final String DEBIT = "DEBIT";
  private static final String CREDIT = "CREDIT";
  private static final String MOVEMENT = "MOVEMENT";
  private static final String FREE = "FREE";
  private static final String DEPOSIT = "DEPOSIT";
  private static final String CHECK = "CHECK";
  private static final String USD = "USD";

  private final TransferRepository transferRepository;
  private final LedgerEntryRepository ledgerEntryRepository;

  public LedgerPostingService(
      TransferRepository transferRepository, LedgerEntryRepository ledgerEntryRepository) {
    this.transferRepository = transferRepository;
    this.ledgerEntryRepository = ledgerEntryRepository;
  }

  /**
   * Approves a deposit and posts it to the ledger. Atomically: updates transfer state to APPROVED,
   * populates required attributes, and creates two ledger entries (debit omnibus, credit investor).
   *
   * @param transferId the transfer to approve and post
   * @throws TransferNotFoundException if the transfer does not exist
   * @throws IllegalStateException if the transfer is not in an approvable state (ANALYZING)
   */
  @Transactional
  public void postApprovedDeposit(UUID transferId) {
    Transfer transfer =
        transferRepository
            .findById(transferId)
            .orElseThrow(() -> new TransferNotFoundException(transferId));

    if (transfer.getState() != TransferState.ANALYZING) {
      throw new IllegalStateException(
          "Transfer " + transferId + " is not in ANALYZING state, cannot approve");
    }

    Instant now = Instant.now();
    UUID transactionId = transfer.getId();

    transfer.setState(TransferState.APPROVED);
    transfer.setType(MOVEMENT);
    transfer.setMemo(FREE);
    transfer.setSubType(DEPOSIT);
    transfer.setTransferType(CHECK);
    transfer.setCurrency(USD);
    transfer.setSourceApplicationId(transfer.getId().toString());
    transfer.setUpdatedAt(now);
    transferRepository.save(transfer);

    LedgerEntry debitEntry =
        new LedgerEntry(
            UUID.randomUUID(),
            transfer.getFromAccountId(),
            transactionId,
            DEBIT,
            transfer.getAmount(),
            transfer.getToAccountId(),
            now);
    LedgerEntry creditEntry =
        new LedgerEntry(
            UUID.randomUUID(),
            transfer.getToAccountId(),
            transactionId,
            CREDIT,
            transfer.getAmount(),
            transfer.getFromAccountId(),
            now);
    ledgerEntryRepository.save(debitEntry);
    ledgerEntryRepository.save(creditEntry);
  }
}
