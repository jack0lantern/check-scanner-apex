package com.apexfintech.checkdeposit.deposit;

import com.apexfintech.checkdeposit.domain.TraceStage;
import com.apexfintech.checkdeposit.domain.Transfer;
import com.apexfintech.checkdeposit.domain.TransferState;
import com.apexfintech.checkdeposit.dto.DepositRequest;
import com.apexfintech.checkdeposit.dto.DepositResponse;
import com.apexfintech.checkdeposit.dto.IqaFailureResponse;
import com.apexfintech.checkdeposit.dto.ResolvedAccount;
import com.apexfintech.checkdeposit.dto.TransferStatusResponse;
import com.apexfintech.checkdeposit.dto.VendorAssessmentResult;
import com.apexfintech.checkdeposit.funding.AccountResolutionService;
import com.apexfintech.checkdeposit.funding.FundingService;
import com.apexfintech.checkdeposit.funding.FundingValidationResult;
import com.apexfintech.checkdeposit.funding.MicrParser;
import com.apexfintech.checkdeposit.ledger.LedgerPostingService;
import com.apexfintech.checkdeposit.repository.AccountRepository;
import com.apexfintech.checkdeposit.repository.TransferRepository;
import com.apexfintech.checkdeposit.settlement.SettlementDateService;
import com.apexfintech.checkdeposit.trace.TraceEventService;
import com.apexfintech.checkdeposit.vendor.VendorScenario;
import com.apexfintech.checkdeposit.vendor.VendorService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Base64;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DepositService {

  private final AccountResolutionService accountResolutionService;
  private final VendorService vendorService;
  private final TransferRepository transferRepository;
  private final FundingService fundingService;
  private final LedgerPostingService ledgerPostingService;
  private final AccountRepository accountRepository;
  private final TraceEventService traceEventService;
  private final SettlementDateService settlementDateService;

  public DepositService(
      AccountResolutionService accountResolutionService,
      VendorService vendorService,
      TransferRepository transferRepository,
      FundingService fundingService,
      LedgerPostingService ledgerPostingService,
      AccountRepository accountRepository,
      TraceEventService traceEventService,
      SettlementDateService settlementDateService) {
    this.accountResolutionService = accountResolutionService;
    this.vendorService = vendorService;
    this.transferRepository = transferRepository;
    this.fundingService = fundingService;
    this.ledgerPostingService = ledgerPostingService;
    this.accountRepository = accountRepository;
    this.traceEventService = traceEventService;
    this.settlementDateService = settlementDateService;
  }

  /** Returns full transfer status for status polling. */
  public TransferStatusResponse getStatus(UUID transferId) {
    Transfer transfer =
        transferRepository
            .findById(transferId)
            .orElseThrow(() -> new TransferNotFoundException(transferId));

    String accountId =
        transfer.getInvestorAccountId() != null
            ? transfer.getInvestorAccountId()
            : accountRepository
                .findByInternalNumber(transfer.getToAccountId())
                .map(a -> a.getExternalId())
                .orElse(transfer.getToAccountId());

    return new TransferStatusResponse(
        transfer.getId(),
        transfer.getState(),
        transfer.getAmount(),
        accountId,
        transfer.getCreatedAt(),
        transfer.getUpdatedAt(),
        transfer.getVendorScore(),
        transfer.getMicrData(),
        transfer.getMicrConfidence(),
        transfer.getOcrAmount(),
        null);
  }

  /**
   * Submits a deposit (new or retry). Uses scenarioAccountId (e.g. from X-Account-Id) for vendor
   * stub scenario selection.
   */
  @Transactional
  public Object submit(DepositRequest request, String scenarioAccountId) {
    byte[] frontBytes = decodeBase64(request.frontImage());
    byte[] backBytes = decodeBase64(request.backImage());
    BigDecimal amount = request.amount();
    String accountId = request.accountId();

    if (request.retryForTransferId() != null) {
      return handleRetry(
          request.retryForTransferId(),
          frontBytes,
          backBytes,
          amount,
          accountId,
          scenarioAccountId);
    }
    return handleNewDeposit(frontBytes, backBytes, amount, accountId, scenarioAccountId);
  }

  private Object handleNewDeposit(
      byte[] frontBytes,
      byte[] backBytes,
      BigDecimal amount,
      String accountId,
      String scenarioAccountId) {
    ResolvedAccount resolved = accountResolutionService.resolve(accountId);
    String toAccountId = resolved.internalNumber();
    String fromAccountId = resolved.omnibusAccountId();

    // Amount limit applies regardless of account ID or vendor scenario — fail early
    var amountRejection = fundingService.validateAmountOnly(amount);
    if (amountRejection.isPresent()) {
      VendorAssessmentResult placeholder =
          new VendorAssessmentResult(
              VendorScenario.CLEAN_PASS, 0.0, "", 0.0, amount, null, 0.0);
      Transfer transfer =
          createTransfer(
              frontBytes,
              backBytes,
              amount,
              toAccountId,
              resolved.externalId(),
              fromAccountId,
              TransferState.REJECTED,
              placeholder,
              settlementDateService.computeSettlementDateNow());
      transferRepository.save(transfer);
      traceEventService.record(
          transfer.getId(),
          TraceStage.SUBMISSION,
          "CREATED",
          java.util.Map.of("state", "REJECTED"));
      traceEventService.record(
          transfer.getId(),
          TraceStage.BUSINESS_RULE,
          "FAIL",
          java.util.Map.of("reason", amountRejection.get()));
      return new IqaFailureResponse(transfer.getId(), amountRejection.get());
    }

    VendorAssessmentResult vendorResult =
        vendorService.assessCheck(frontBytes, backBytes, amount, scenarioAccountId);

    // IQA failures (blur, glare) and duplicate: return 422 for immediate retry or final rejection.
    // MICR_READ_FAILURE and AMOUNT_MISMATCH: proceed to ANALYZING for operator review.
    boolean return422 =
        vendorResult.actionableMessage() != null
            && vendorResult.scenario() != VendorScenario.MICR_READ_FAILURE
            && vendorResult.scenario() != VendorScenario.AMOUNT_MISMATCH;

    if (return422) {
    LocalDate settlementDate = settlementDateService.computeSettlementDateNow();
    Transfer transfer =
        createTransfer(
            frontBytes,
            backBytes,
            amount,
            toAccountId,
            resolved.externalId(),
            fromAccountId,
            TransferState.VALIDATING,
            vendorResult,
            settlementDate);
      transferRepository.save(transfer);
      traceEventService.record(
          transfer.getId(),
          TraceStage.SUBMISSION,
          "CREATED",
          java.util.Map.of("state", "VALIDATING"));
      traceEventService.record(
          transfer.getId(),
          TraceStage.VENDOR_RESULT,
          "FAIL",
          java.util.Map.of("actionableMessage", vendorResult.actionableMessage()));
      return new IqaFailureResponse(transfer.getId(), vendorResult.actionableMessage());
    }

    LocalDate settlementDate = settlementDateService.computeSettlementDateNow();
    Transfer transfer =
        createTransfer(
            frontBytes,
            backBytes,
            amount,
            toAccountId,
            resolved.externalId(),
            fromAccountId,
            TransferState.ANALYZING,
            vendorResult,
            settlementDate);

    transferRepository.save(transfer);
    traceEventService.record(
        transfer.getId(), TraceStage.SUBMISSION, "CREATED", java.util.Map.of("state", "ANALYZING"));
    traceEventService.record(
        transfer.getId(),
        TraceStage.VENDOR_RESULT,
        "PASS",
        java.util.Map.of(
            "vendorScore", vendorResult.vendorScore() != null ? vendorResult.vendorScore() : 0,
            "micrData", vendorResult.micrData() != null ? vendorResult.micrData() : ""));

    // MICR_READ_FAILURE: no micrData to validate; ROUTING_MISMATCH: routing confirmed bad by vendor.
    // Both skip funding and stay in ANALYZING for operator review.
    if (vendorResult.scenario() == VendorScenario.MICR_READ_FAILURE
        || vendorResult.scenario() == VendorScenario.ROUTING_MISMATCH) {
      return new DepositResponse(transfer.getId(), transfer.getState());
    }

    FundingValidationResult fundingResult = fundingService.validate(transfer, resolved);
    if (!fundingResult.passed()) {
      transfer.setState(TransferState.REJECTED);
      transferRepository.save(transfer);
      traceEventService.record(
          transfer.getId(),
          TraceStage.BUSINESS_RULE,
          "FAIL",
          java.util.Map.of(
              "reason",
              fundingResult.rejectionReason() != null
                  ? fundingResult.rejectionReason()
                  : "Deposit validation failed"));
      return new IqaFailureResponse(
          transfer.getId(),
          fundingResult.rejectionReason() != null
              ? fundingResult.rejectionReason()
              : "Deposit validation failed");
    }

    traceEventService.record(
        transfer.getId(),
        TraceStage.BUSINESS_RULE,
        "PASS",
        java.util.Map.of(
            "contributionType",
            fundingResult.defaultContributionType() != null
                ? fundingResult.defaultContributionType()
                : "INDIVIDUAL"));
    ledgerPostingService.postApprovedDeposit(transfer.getId());
    return new DepositResponse(transfer.getId(), TransferState.APPROVED);
  }

  private Object handleRetry(
      UUID transferId,
      byte[] frontBytes,
      byte[] backBytes,
      BigDecimal amount,
      String accountId,
      String scenarioAccountId) {
    Transfer transfer =
        transferRepository
            .findById(transferId)
            .orElseThrow(() -> new TransferNotFoundException(transferId));

    if (!isRetryableState(transfer.getState())) {
      throw new TransferNotRetryableException(transferId, transfer.getState());
    }

    ResolvedAccount resolved = accountResolutionService.resolve(accountId);
    if (!transfer.getToAccountId().equals(resolved.internalNumber())) {
      throw new TransferNotFoundException(transferId);
    }

    // Amount limit applies regardless of account ID or vendor scenario
    var amountRejection = fundingService.validateAmountOnly(amount);
    if (amountRejection.isPresent()) {
      transfer.setState(TransferState.REJECTED);
      transferRepository.save(transfer);
      traceEventService.record(
          transfer.getId(),
          TraceStage.BUSINESS_RULE,
          "FAIL",
          java.util.Map.of("reason", amountRejection.get()));
      return new IqaFailureResponse(transfer.getId(), amountRejection.get());
    }

    VendorAssessmentResult vendorResult =
        vendorService.assessCheck(frontBytes, backBytes, amount, scenarioAccountId);

    updateTransferForRetry(transfer, frontBytes, backBytes, vendorResult);
    transfer.setSettlementDate(settlementDateService.computeSettlementDateNow());

    if (vendorResult.actionableMessage() != null) {
      transferRepository.save(transfer);
      traceEventService.record(
          transfer.getId(),
          TraceStage.VENDOR_RESULT,
          "FAIL",
          java.util.Map.of("actionableMessage", vendorResult.actionableMessage()));
      return new IqaFailureResponse(transfer.getId(), vendorResult.actionableMessage());
    }

    traceEventService.record(
        transfer.getId(),
        TraceStage.VENDOR_RESULT,
        "PASS",
        java.util.Map.of(
            "vendorScore", vendorResult.vendorScore() != null ? vendorResult.vendorScore() : 0,
            "micrData", vendorResult.micrData() != null ? vendorResult.micrData() : ""));

    FundingValidationResult fundingResult = fundingService.validate(transfer, resolved);
    if (!fundingResult.passed()) {
      transfer.setState(TransferState.REJECTED);
      transferRepository.save(transfer);
      traceEventService.record(
          transfer.getId(),
          TraceStage.BUSINESS_RULE,
          "FAIL",
          java.util.Map.of(
              "reason",
              fundingResult.rejectionReason() != null
                  ? fundingResult.rejectionReason()
                  : "Deposit validation failed"));
      return new IqaFailureResponse(
          transfer.getId(),
          fundingResult.rejectionReason() != null
              ? fundingResult.rejectionReason()
              : "Deposit validation failed");
    }

    traceEventService.record(
        transfer.getId(),
        TraceStage.BUSINESS_RULE,
        "PASS",
        java.util.Map.of(
            "contributionType",
            fundingResult.defaultContributionType() != null
                ? fundingResult.defaultContributionType()
                : "INDIVIDUAL"));

    transfer.setState(TransferState.ANALYZING);
    transferRepository.save(transfer);
    ledgerPostingService.postApprovedDeposit(transfer.getId());
    return new DepositResponse(transfer.getId(), TransferState.APPROVED);
  }

  private static boolean isRetryableState(TransferState state) {
    return state == TransferState.VALIDATING || state == TransferState.REQUESTED;
  }

  private Transfer createTransfer(
      byte[] frontBytes,
      byte[] backBytes,
      BigDecimal amount,
      String toAccountId,
      String investorAccountId,
      String fromAccountId,
      TransferState state,
      VendorAssessmentResult vendorResult,
      LocalDate settlementDate) {
    UUID id = UUID.randomUUID();
    Transfer transfer =
        new Transfer(
            id,
            frontBytes,
            backBytes,
            amount,
            toAccountId,
            investorAccountId,
            fromAccountId,
            state,
            vendorResult.vendorScore(),
            vendorResult.micrData(),
            vendorResult.micrConfidence(),
            vendorResult.ocrAmount(),
            "INDIVIDUAL",
            null,
            settlementDate);
    populateMicrParsedFields(transfer, vendorResult.micrData());
    return transfer;
  }

  private void updateTransferForRetry(
      Transfer transfer, byte[] frontBytes, byte[] backBytes, VendorAssessmentResult vendorResult) {
    transfer.setFrontImageData(frontBytes);
    transfer.setBackImageData(backBytes);
    transfer.setVendorScore(vendorResult.vendorScore());
    transfer.setMicrData(vendorResult.micrData());
    transfer.setMicrConfidence(vendorResult.micrConfidence());
    transfer.setOcrAmount(vendorResult.ocrAmount());
    populateMicrParsedFields(transfer, vendorResult.micrData());
    transfer.setUpdatedAt(java.time.Instant.now());
  }

  private static void populateMicrParsedFields(Transfer transfer, String micrData) {
    if (micrData != null && !micrData.isBlank()) {
      transfer.setMicrRoutingNumber(MicrParser.extractRoutingNumber(micrData));
      transfer.setMicrAccountNumber(MicrParser.extractAccountNumber(micrData));
      transfer.setMicrCheckNumber(MicrParser.extractCheckNumber(micrData));
    }
  }

  private static byte[] decodeBase64(String base64) {
    if (base64 == null || base64.isBlank()) {
      return new byte[0];
    }
    return Base64.getDecoder().decode(base64);
  }
}
