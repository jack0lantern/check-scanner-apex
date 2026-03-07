package com.apexfintech.checkdeposit.deposit;

import com.apexfintech.checkdeposit.domain.Transfer;
import com.apexfintech.checkdeposit.domain.TransferState;
import com.apexfintech.checkdeposit.dto.DepositRequest;
import com.apexfintech.checkdeposit.dto.DepositResponse;
import com.apexfintech.checkdeposit.dto.IqaFailureResponse;
import com.apexfintech.checkdeposit.dto.ResolvedAccount;
import com.apexfintech.checkdeposit.dto.VendorAssessmentResult;
import com.apexfintech.checkdeposit.funding.AccountResolutionService;
import com.apexfintech.checkdeposit.repository.TransferRepository;
import com.apexfintech.checkdeposit.vendor.VendorService;
import java.math.BigDecimal;
import java.util.Base64;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DepositService {

  private final AccountResolutionService accountResolutionService;
  private final VendorService vendorService;
  private final TransferRepository transferRepository;

  public DepositService(
      AccountResolutionService accountResolutionService,
      VendorService vendorService,
      TransferRepository transferRepository) {
    this.accountResolutionService = accountResolutionService;
    this.vendorService = vendorService;
    this.transferRepository = transferRepository;
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
      return handleRetry(request.retryForTransferId(), frontBytes, backBytes, amount, accountId, scenarioAccountId);
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

    VendorAssessmentResult vendorResult =
        vendorService.assessCheck(frontBytes, backBytes, amount, scenarioAccountId);

    if (vendorResult.actionableMessage() != null) {
      Transfer transfer =
          createTransfer(
              frontBytes,
              backBytes,
              amount,
              toAccountId,
              fromAccountId,
              TransferState.VALIDATING,
              vendorResult);
      transferRepository.save(transfer);
      return new IqaFailureResponse(transfer.getId(), vendorResult.actionableMessage());
    }

    Transfer transfer =
        createTransfer(
            frontBytes,
            backBytes,
            amount,
            toAccountId,
            fromAccountId,
            TransferState.ANALYZING,
            vendorResult);
    transferRepository.save(transfer);
    return new DepositResponse(transfer.getId(), transfer.getState());
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

    VendorAssessmentResult vendorResult =
        vendorService.assessCheck(frontBytes, backBytes, amount, scenarioAccountId);

    updateTransferForRetry(transfer, frontBytes, backBytes, vendorResult);

    if (vendorResult.actionableMessage() != null) {
      transferRepository.save(transfer);
      return new IqaFailureResponse(transfer.getId(), vendorResult.actionableMessage());
    }

    transfer.setState(TransferState.ANALYZING);
    transferRepository.save(transfer);
    return new DepositResponse(transfer.getId(), transfer.getState());
  }

  private static boolean isRetryableState(TransferState state) {
    return state == TransferState.VALIDATING || state == TransferState.REQUESTED;
  }

  private Transfer createTransfer(
      byte[] frontBytes,
      byte[] backBytes,
      BigDecimal amount,
      String toAccountId,
      String fromAccountId,
      TransferState state,
      VendorAssessmentResult vendorResult) {
    UUID id = UUID.randomUUID();
    return new Transfer(
        id,
        frontBytes,
        backBytes,
        amount,
        toAccountId,
        fromAccountId,
        state,
        vendorResult.vendorScore(),
        vendorResult.micrData(),
        vendorResult.micrConfidence(),
        vendorResult.ocrAmount(),
        "INDIVIDUAL",
        null,
        null);
  }

  private void updateTransferForRetry(
      Transfer transfer,
      byte[] frontBytes,
      byte[] backBytes,
      VendorAssessmentResult vendorResult) {
    transfer.setFrontImageData(frontBytes);
    transfer.setBackImageData(backBytes);
    transfer.setVendorScore(vendorResult.vendorScore());
    transfer.setMicrData(vendorResult.micrData());
    transfer.setMicrConfidence(vendorResult.micrConfidence());
    transfer.setOcrAmount(vendorResult.ocrAmount());
    transfer.setUpdatedAt(java.time.Instant.now());
  }

  private static byte[] decodeBase64(String base64) {
    if (base64 == null || base64.isBlank()) {
      return new byte[0];
    }
    return Base64.getDecoder().decode(base64);
  }
}
