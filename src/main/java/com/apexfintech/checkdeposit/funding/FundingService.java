package com.apexfintech.checkdeposit.funding;

import com.apexfintech.checkdeposit.domain.Transfer;
import com.apexfintech.checkdeposit.domain.TransferState;
import com.apexfintech.checkdeposit.dto.ResolvedAccount;
import com.apexfintech.checkdeposit.repository.TransferRepository;
import java.math.BigDecimal;
import java.util.Optional;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class FundingService {

  private static final BigDecimal MAX_DEPOSIT_DEFAULT = new BigDecimal("5000");
  private static final int DUPLICATE_WINDOW_HOURS_DEFAULT = 24;
  private static final BigDecimal RETIREMENT_CAP_DEFAULT = new BigDecimal("7000");
  private static final List<TransferState> APPROVED_STATES =
      List.of(TransferState.APPROVED, TransferState.FUNDS_POSTED, TransferState.COMPLETED);

  private final TransferRepository transferRepository;
  private final BigDecimal maxDepositAmount;
  private final int duplicateWindowHours;
  private final BigDecimal retirementContributionCap;

  public FundingService(
      TransferRepository transferRepository,
      @Value("${funding.max-deposit-amount:5000}") BigDecimal maxDepositAmount,
      @Value("${funding.duplicate-window-hours:24}") int duplicateWindowHours,
      @Value("${funding.contribution-cap.retirement:7000}") BigDecimal retirementContributionCap) {
    this.transferRepository = transferRepository;
    this.maxDepositAmount = maxDepositAmount != null ? maxDepositAmount : MAX_DEPOSIT_DEFAULT;
    this.duplicateWindowHours =
        duplicateWindowHours > 0 ? duplicateWindowHours : DUPLICATE_WINDOW_HOURS_DEFAULT;
    this.retirementContributionCap =
        retirementContributionCap != null ? retirementContributionCap : RETIREMENT_CAP_DEFAULT;
  }

  /**
   * Validates amount against the per-deposit limit. Use this before vendor assessment so amount
   * &gt; max always fails regardless of account ID or vendor scenario.
   */
  public Optional<String> validateAmountOnly(BigDecimal amount) {
    if (amount != null && amount.compareTo(maxDepositAmount) > 0) {
      return Optional.of(
          "Deposit amount exceeds maximum of $" + maxDepositAmount + " per deposit");
    }
    return Optional.empty();
  }

  /**
   * Validates a transfer against business rules in order: (1) MICR extraction (routing, account,
   * check number), (2) routing and account number match, (3) amount limit, (4) contribution type
   * defaults and caps for retirement accounts, (5) duplicate detection by check number.
   */
  public FundingValidationResult validate(Transfer transfer, ResolvedAccount resolvedAccount) {
    if (transfer.getMicrData() == null || transfer.getMicrData().isBlank()) {
      return FundingValidationResult.reject(
          "Check routing number could not be read — please try again or deposit at a branch");
    }
    String micrRouting = MicrParser.extractRoutingNumber(transfer.getMicrData());
    if (micrRouting == null) {
      return FundingValidationResult.reject(
          "Check routing number could not be parsed — please try again or deposit at a branch");
    }
    String micrAccount = MicrParser.extractAccountNumber(transfer.getMicrData());
    if (micrAccount == null) {
      return FundingValidationResult.reject(
          "Check account number could not be read — please try again or deposit at a branch");
    }
    String micrCheck = MicrParser.extractCheckNumber(transfer.getMicrData());
    if (micrCheck == null) {
      return FundingValidationResult.reject(
          "Check number could not be read — please try again or deposit at a branch");
    }

    String accountRouting = resolvedAccount.routingNumber();
    if (accountRouting == null || !micrRouting.equals(accountRouting.replaceAll("\\D", ""))) {
      return FundingValidationResult.reject(
          "Check routing number does not match the selected account — please verify you are depositing to the correct account");
    }

    String expectedMicrAccount = resolvedAccount.micrAccountNumber();
    if (expectedMicrAccount != null) {
      String normalizedExpected = expectedMicrAccount.replaceAll("\\D", "").trim();
      String normalizedMicr = micrAccount.replaceAll("\\D", "").trim();
      if (!normalizedMicr.equals(normalizedExpected)) {
        return FundingValidationResult.reject(
            "Check account number does not match the selected account — please verify you are depositing to the correct account");
      }
    }

    if (transfer.getAmount().compareTo(maxDepositAmount) > 0) {
      return FundingValidationResult.reject(
          "Deposit amount exceeds maximum of $" + maxDepositAmount + " per deposit");
    }

    boolean isRetirement = isRetirementAccount(resolvedAccount.accountType());
    String defaultContributionType = isRetirement ? "INDIVIDUAL" : null;

    if (isRetirement) {
      BigDecimal existingContributions =
          transferRepository.sumApprovedContributionsForAccountInYear(
              transfer.getToAccountId(),
              APPROVED_STATES,
              yearStart(transfer.getCreatedAt()),
              yearEnd(transfer.getCreatedAt()));
      BigDecimal existing = existingContributions != null ? existingContributions : BigDecimal.ZERO;
      BigDecimal newTotal = existing.add(transfer.getAmount());
      if (newTotal.compareTo(retirementContributionCap) > 0) {
        return FundingValidationResult.reject(
            "Contribution cap of $"
                + retirementContributionCap
                + " for retirement accounts would be exceeded");
      }
    }

    Instant windowStart = transfer.getCreatedAt().minusSeconds(duplicateWindowHours * 3600L);
    boolean isDuplicate =
        isDuplicateByCheckNumber(transfer, micrRouting, micrAccount, micrCheck, windowStart)
            || isDuplicateByMicrData(transfer, windowStart);
    if (isDuplicate) {
      return FundingValidationResult.reject(
          "Duplicate deposit detected: another transfer with same check exists");
    }

    return defaultContributionType != null
        ? FundingValidationResult.pass(defaultContributionType)
        : FundingValidationResult.pass();
  }

  private boolean isDuplicateByCheckNumber(
      Transfer transfer,
      String micrRouting,
      String micrAccount,
      String micrCheck,
      Instant windowStart) {
    if (micrRouting == null || micrAccount == null || micrCheck == null) {
      return false;
    }
    return transferRepository.existsNonRejectedDuplicateByCheckNumber(
        transfer.getFromAccountId(),
        micrRouting,
        micrAccount,
        micrCheck,
        transfer.getId(),
        windowStart);
  }

  private boolean isDuplicateByMicrData(Transfer transfer, Instant windowStart) {
    if (transfer.getMicrRoutingNumber() == null
        || transfer.getMicrAccountNumber() == null
        || transfer.getMicrCheckNumber() == null) {
      return transfer.getMicrData() != null
          && transferRepository.existsNonRejectedDuplicate(
              transfer.getFromAccountId(),
              transfer.getAmount(),
              transfer.getMicrData(),
              transfer.getId(),
              windowStart);
    }
    return false;
  }

  private static boolean isRetirementAccount(String accountType) {
    return accountType != null && "RETIREMENT".equalsIgnoreCase(accountType);
  }

  private static Instant yearStart(Instant instant) {
    ZonedDateTime zdt = instant.atZone(ZoneId.systemDefault());
    return zdt.withDayOfYear(1).withHour(0).withMinute(0).withSecond(0).withNano(0).toInstant();
  }

  private static Instant yearEnd(Instant instant) {
    ZonedDateTime zdt = instant.atZone(ZoneId.systemDefault());
    return zdt.plusYears(1)
        .withDayOfYear(1)
        .withHour(0)
        .withMinute(0)
        .withSecond(0)
        .withNano(0)
        .toInstant();
  }
}
