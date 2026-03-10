package com.apexfintech.checkdeposit.funding;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.apexfintech.checkdeposit.domain.Transfer;
import com.apexfintech.checkdeposit.domain.TransferState;
import com.apexfintech.checkdeposit.dto.ResolvedAccount;
import com.apexfintech.checkdeposit.repository.TransferRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class FundingServiceTest {

  private static final String OMNIBUS_ID = "OMN-999";
  private static final String TO_ACCOUNT_ID = "INT-12345678";
  private static final String MICR_ACCOUNT = "12345678";

  /** MICR with routing 021000021, account 12345678, check 901 (matches TEST001). */
  private static final String MICR_DATA = "02100002112345678901";

  @MockBean private TransferRepository transferRepository;

  @Autowired private FundingService fundingService;

  @Test
  void validate_amount5000_passes() {
    Transfer transfer = createTransfer(new BigDecimal("5000"));
    ResolvedAccount resolved =
        new ResolvedAccount(TO_ACCOUNT_ID, "021000021", MICR_ACCOUNT, OMNIBUS_ID, "RETIREMENT");

    when(transferRepository.existsNonRejectedDuplicateByCheckNumber(
            eq(OMNIBUS_ID),
            eq("021000021"),
            eq(MICR_ACCOUNT),
            eq("901"),
            eq(transfer.getId()),
            any(Instant.class)))
        .thenReturn(false);
    when(transferRepository.sumApprovedContributionsForAccountInYear(
            eq(TO_ACCOUNT_ID), any(List.class), any(Instant.class), any(Instant.class)))
        .thenReturn(BigDecimal.ZERO);

    FundingValidationResult result = fundingService.validate(transfer, resolved);

    assertThat(result.passed()).isTrue();
    assertThat(result.rejectionReason()).isNull();
    assertThat(result.defaultContributionType()).isEqualTo("INDIVIDUAL");
  }

  @Test
  void validate_amount5001_fails() {
    Transfer transfer = createTransfer(new BigDecimal("5001"));
    ResolvedAccount resolved =
        new ResolvedAccount(TO_ACCOUNT_ID, "021000021", MICR_ACCOUNT, OMNIBUS_ID, "RETIREMENT");

    FundingValidationResult result = fundingService.validate(transfer, resolved);

    assertThat(result.passed()).isFalse();
    assertThat(result.rejectionReason()).contains("5000");
    verify(transferRepository, never())
        .existsNonRejectedDuplicateByCheckNumber(any(), any(), any(), any(), any(), any());
  }

  @Test
  void validate_retirementAccount_defaultsContributionTypeToIndividual() {
    Transfer transfer = createTransfer(new BigDecimal("1000"));
    ResolvedAccount resolved =
        new ResolvedAccount(TO_ACCOUNT_ID, "021000021", MICR_ACCOUNT, OMNIBUS_ID, "RETIREMENT");

    when(transferRepository.existsNonRejectedDuplicateByCheckNumber(
            eq(OMNIBUS_ID),
            eq("021000021"),
            eq(MICR_ACCOUNT),
            eq("901"),
            eq(transfer.getId()),
            any(Instant.class)))
        .thenReturn(false);
    when(transferRepository.sumApprovedContributionsForAccountInYear(
            eq(TO_ACCOUNT_ID), any(List.class), any(Instant.class), any(Instant.class)))
        .thenReturn(BigDecimal.ZERO);

    FundingValidationResult result = fundingService.validate(transfer, resolved);

    assertThat(result.passed()).isTrue();
    assertThat(result.defaultContributionType()).isEqualTo("INDIVIDUAL");
  }

  @Test
  void validate_contributionCapViolation_rejected() {
    Transfer transfer = createTransfer(new BigDecimal("3000"));
    ResolvedAccount resolved =
        new ResolvedAccount(TO_ACCOUNT_ID, "021000021", MICR_ACCOUNT, OMNIBUS_ID, "RETIREMENT");

    when(transferRepository.sumApprovedContributionsForAccountInYear(
            eq(TO_ACCOUNT_ID), any(List.class), any(Instant.class), any(Instant.class)))
        .thenReturn(new BigDecimal("5000"));

    FundingValidationResult result = fundingService.validate(transfer, resolved);

    assertThat(result.passed()).isFalse();
    assertThat(result.rejectionReason()).containsIgnoringCase("contribution");
    verify(transferRepository, never())
        .existsNonRejectedDuplicateByCheckNumber(any(), any(), any(), any(), any(), any());
  }

  @Test
  void validate_duplicateTransfer_rejected() {
    Transfer transfer = createTransfer(new BigDecimal("1000"));
    ResolvedAccount resolved =
        new ResolvedAccount(TO_ACCOUNT_ID, "021000021", MICR_ACCOUNT, OMNIBUS_ID, "RETIREMENT");

    when(transferRepository.sumApprovedContributionsForAccountInYear(
            eq(TO_ACCOUNT_ID), any(List.class), any(Instant.class), any(Instant.class)))
        .thenReturn(BigDecimal.ZERO);
    when(transferRepository.existsNonRejectedDuplicateByCheckNumber(
            eq(OMNIBUS_ID),
            eq("021000021"),
            eq(MICR_ACCOUNT),
            eq("901"),
            eq(transfer.getId()),
            any(Instant.class)))
        .thenReturn(true);

    FundingValidationResult result = fundingService.validate(transfer, resolved);

    assertThat(result.passed()).isFalse();
    assertThat(result.rejectionReason()).containsIgnoringCase("duplicate");
  }

  @Test
  void validate_routingMismatch_rejected() {
    Transfer transfer = createTransfer(new BigDecimal("1000"));
    transfer.setMicrData("99999999912345678901"); // Wrong routing
    ResolvedAccount resolved =
        new ResolvedAccount(TO_ACCOUNT_ID, "021000021", MICR_ACCOUNT, OMNIBUS_ID, "BROKERAGE");

    FundingValidationResult result = fundingService.validate(transfer, resolved);

    assertThat(result.passed()).isFalse();
    assertThat(result.rejectionReason()).containsIgnoringCase("routing");
    verify(transferRepository, never())
        .existsNonRejectedDuplicateByCheckNumber(any(), any(), any(), any(), any(), any());
  }

  @Test
  void validate_missingMicrData_rejected() {
    Transfer transfer = createTransfer(new BigDecimal("1000"));
    transfer.setMicrData(null);
    ResolvedAccount resolved =
        new ResolvedAccount(TO_ACCOUNT_ID, "021000021", MICR_ACCOUNT, OMNIBUS_ID, "BROKERAGE");

    FundingValidationResult result = fundingService.validate(transfer, resolved);

    assertThat(result.passed()).isFalse();
    assertThat(result.rejectionReason()).containsIgnoringCase("routing");
  }

  @Test
  void validate_nonRetirementAccount_noContributionTypeDefault() {
    Transfer transfer = createTransfer(new BigDecimal("1000"));
    ResolvedAccount resolved =
        new ResolvedAccount(TO_ACCOUNT_ID, "021000021", MICR_ACCOUNT, OMNIBUS_ID, "BROKERAGE");

    when(transferRepository.existsNonRejectedDuplicateByCheckNumber(
            eq(OMNIBUS_ID),
            eq("021000021"),
            eq(MICR_ACCOUNT),
            eq("901"),
            eq(transfer.getId()),
            any(Instant.class)))
        .thenReturn(false);

    FundingValidationResult result = fundingService.validate(transfer, resolved);

    assertThat(result.passed()).isTrue();
    assertThat(result.defaultContributionType()).isNull();
  }

  @Test
  void validate_accountNumberMismatch_rejected() {
    Transfer transfer = createTransfer(new BigDecimal("1000"));
    ResolvedAccount resolved =
        new ResolvedAccount(TO_ACCOUNT_ID, "021000021", "99999999", OMNIBUS_ID, "BROKERAGE");

    FundingValidationResult result = fundingService.validate(transfer, resolved);

    assertThat(result.passed()).isFalse();
    assertThat(result.rejectionReason()).containsIgnoringCase("account");
  }

  @Test
  void validate_micrTooShort_rejected() {
    Transfer transfer = createTransfer(new BigDecimal("1000"));
    transfer.setMicrData("02100002112345678"); // Only 17 digits; account/check extraction fails
    ResolvedAccount resolved =
        new ResolvedAccount(TO_ACCOUNT_ID, "021000021", MICR_ACCOUNT, OMNIBUS_ID, "BROKERAGE");

    FundingValidationResult result = fundingService.validate(transfer, resolved);

    assertThat(result.passed()).isFalse();
    assertThat(result.rejectionReason()).containsIgnoringCase("could not be read");
  }

  private Transfer createTransfer(BigDecimal amount) {
    return new Transfer(
        UUID.randomUUID(),
        new byte[0],
        new byte[0],
        amount,
        TO_ACCOUNT_ID,
        OMNIBUS_ID,
        TransferState.ANALYZING,
        0.95,
        MICR_DATA,
        0.99,
        amount,
        null,
        null,
        null);
  }
}
