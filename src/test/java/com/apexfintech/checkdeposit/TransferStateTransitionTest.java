package com.apexfintech.checkdeposit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.apexfintech.checkdeposit.domain.Transfer;
import com.apexfintech.checkdeposit.domain.TransferState;
import com.apexfintech.checkdeposit.dto.RejectRequest;
import com.apexfintech.checkdeposit.ledger.LedgerPostingService;
import com.apexfintech.checkdeposit.operator.OperatorService;
import com.apexfintech.checkdeposit.repository.TransferRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * State machine transition tests. Asserts: valid happy-path transitions proceed in order; invalid
 * transitions throw an exception (COMPLETED → APPROVED, REJECTED → APPROVED, RETURNED →
 * FUNDS_POSTED).
 */
@SpringBootTest(properties = "spring.task.scheduling.enabled=false")
@ActiveProfiles("test")
class TransferStateTransitionTest {

  private static final String TO_ACCOUNT = "INT-12345678";
  private static final String FROM_ACCOUNT = "OMN-999";
  private static final String MICR_DATA = "02100002112345678901";
  private static final BigDecimal AMOUNT = new BigDecimal("100.00");

  @Autowired private LedgerPostingService ledgerPostingService;
  @Autowired private OperatorService operatorService;
  @Autowired private TransferRepository transferRepository;

  @BeforeEach
  void setUp() {
    transferRepository.deleteAll();
  }

  @Test
  void validTransition_analyzingToApproved_viaPostApprovedDeposit() {
    Transfer transfer = saveTransfer(TransferState.ANALYZING);

    ledgerPostingService.postApprovedDeposit(transfer.getId());

    Transfer updated = transferRepository.findById(transfer.getId()).orElseThrow();
    assertThat(updated.getState()).isEqualTo(TransferState.APPROVED);
  }

  @Test
  void invalidTransition_completedToApproved_throwsException() {
    Transfer transfer = saveTransfer(TransferState.COMPLETED);

    assertThatThrownBy(() -> ledgerPostingService.postApprovedDeposit(transfer.getId()))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("not in ANALYZING state");
  }

  @Test
  void invalidTransition_rejectedToApproved_throwsException() {
    Transfer transfer = saveTransfer(TransferState.REJECTED);

    assertThatThrownBy(() -> ledgerPostingService.postApprovedDeposit(transfer.getId()))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("not in ANALYZING state");
  }

  @Test
  void invalidTransition_returnedToApproved_throwsException() {
    Transfer transfer = saveTransfer(TransferState.RETURNED);

    assertThatThrownBy(() -> ledgerPostingService.postApprovedDeposit(transfer.getId()))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("not in ANALYZING state");
  }

  @Test
  void invalidTransition_rejectedToReject_throwsException() {
    Transfer transfer = saveTransfer(TransferState.REJECTED);

    assertThatThrownBy(
            () -> operatorService.reject(transfer.getId(), new RejectRequest("reason"), "op1"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("not in ANALYZING state");
  }

  @Test
  void invalidTransition_completedToReject_throwsException() {
    Transfer transfer = saveTransfer(TransferState.COMPLETED);

    assertThatThrownBy(
            () -> operatorService.reject(transfer.getId(), new RejectRequest("reason"), "op1"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("not in ANALYZING state");
  }

  private Transfer saveTransfer(TransferState state) {
    Transfer t =
        new Transfer(
            UUID.randomUUID(),
            new byte[0],
            new byte[0],
            AMOUNT,
            TO_ACCOUNT,
            FROM_ACCOUNT,
            state,
            0.95,
            MICR_DATA,
            0.99,
            AMOUNT,
            "INDIVIDUAL",
            null,
            LocalDate.now());
    return transferRepository.save(t);
  }
}
