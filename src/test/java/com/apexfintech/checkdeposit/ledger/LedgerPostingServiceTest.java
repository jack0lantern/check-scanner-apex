package com.apexfintech.checkdeposit.ledger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.apexfintech.checkdeposit.deposit.TransferNotFoundException;
import com.apexfintech.checkdeposit.domain.LedgerEntry;
import com.apexfintech.checkdeposit.domain.Transfer;
import com.apexfintech.checkdeposit.domain.TransferState;
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

@SpringBootTest
@ActiveProfiles("test")
class LedgerPostingServiceTest {

  private static final String TO_ACCOUNT_ID = "INT-12345678";
  private static final String FROM_ACCOUNT_ID = "OMN-999";
  private static final String MICR_DATA = "02100002112345678901";
  private static final BigDecimal AMOUNT = new BigDecimal("150.50");

  @Autowired private LedgerPostingService ledgerPostingService;
  @Autowired private TransferRepository transferRepository;
  @Autowired private LedgerEntryRepository ledgerEntryRepository;

  private Transfer transfer;

  @BeforeEach
  void setUp() {
    ledgerEntryRepository.deleteAll();
    transfer =
        transferRepository.save(
            new Transfer(
                UUID.randomUUID(),
                new byte[0],
                new byte[0],
                AMOUNT,
                TO_ACCOUNT_ID,
                "TEST001",
                FROM_ACCOUNT_ID,
                TransferState.ANALYZING,
                0.95,
                MICR_DATA,
                0.99,
                AMOUNT,
                "INDIVIDUAL",
                null,
                null));
  }

  @Test
  void postApprovedDeposit_createsTwoLedgerEntriesWithMatchingTransactionId() {
    long countBefore = ledgerEntryRepository.count();

    ledgerPostingService.postApprovedDeposit(transfer.getId());

    List<LedgerEntry> entries = ledgerEntryRepository.findAll();
    assertThat(entries).hasSize((int) (countBefore + 2));

    LedgerEntry firstNew = entries.get(entries.size() - 2);
    LedgerEntry secondNew = entries.get(entries.size() - 1);
    assertThat(firstNew.getTransactionId()).isEqualTo(secondNew.getTransactionId());
    assertThat(firstNew.getCreatedAt()).isEqualTo(secondNew.getCreatedAt());
  }

  @Test
  void postApprovedDeposit_updatesTransferStateToApproved() {
    ledgerPostingService.postApprovedDeposit(transfer.getId());

    Transfer updated = transferRepository.findById(transfer.getId()).orElseThrow();
    assertThat(updated.getState()).isEqualTo(TransferState.APPROVED);
  }

  @Test
  void postApprovedDeposit_populatesAllRequiredTransferAttributes() {
    ledgerPostingService.postApprovedDeposit(transfer.getId());

    Transfer updated = transferRepository.findById(transfer.getId()).orElseThrow();
    assertThat(updated.getType()).isEqualTo("MOVEMENT");
    assertThat(updated.getMemo()).isEqualTo("FREE");
    assertThat(updated.getSubType()).isEqualTo("DEPOSIT");
    assertThat(updated.getTransferType()).isEqualTo("CHECK");
    assertThat(updated.getCurrency()).isEqualTo("USD");
    assertThat(updated.getSourceApplicationId()).isEqualTo(transfer.getId().toString());
  }

  @Test
  void postApprovedDeposit_createsDebitAndCreditEntries() {
    ledgerPostingService.postApprovedDeposit(transfer.getId());

    List<LedgerEntry> entries = ledgerEntryRepository.findAll();
    LedgerEntry debit =
        entries.stream().filter(e -> "DEBIT".equals(e.getType())).findFirst().orElseThrow();
    LedgerEntry credit =
        entries.stream().filter(e -> "CREDIT".equals(e.getType())).findFirst().orElseThrow();

    assertThat(debit.getAccountId()).isEqualTo(FROM_ACCOUNT_ID);
    assertThat(debit.getCounterpartyAccountId()).isEqualTo(TO_ACCOUNT_ID);
    assertThat(debit.getAmount()).isEqualByComparingTo(AMOUNT);

    assertThat(credit.getAccountId()).isEqualTo(TO_ACCOUNT_ID);
    assertThat(credit.getCounterpartyAccountId()).isEqualTo(FROM_ACCOUNT_ID);
    assertThat(credit.getAmount()).isEqualByComparingTo(AMOUNT);
  }

  @Test
  void postApprovedDeposit_unknownTransferId_throwsTransferNotFoundException() {
    UUID unknownId = UUID.randomUUID();

    assertThatThrownBy(() -> ledgerPostingService.postApprovedDeposit(unknownId))
        .isInstanceOf(TransferNotFoundException.class)
        .hasMessageContaining(unknownId.toString());
  }

  @Test
  void postApprovedDeposit_transferNotInAnalyzingState_throwsIllegalStateException() {
    transfer.setState(TransferState.APPROVED);
    transferRepository.save(transfer);

    assertThatThrownBy(() -> ledgerPostingService.postApprovedDeposit(transfer.getId()))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("ANALYZING");
  }
}
