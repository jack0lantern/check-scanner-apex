package com.apexfintech.checkdeposit.operator;

import com.apexfintech.checkdeposit.deposit.TransferNotFoundException;
import com.apexfintech.checkdeposit.domain.Account;
import com.apexfintech.checkdeposit.domain.AuditLog;
import com.apexfintech.checkdeposit.domain.TraceStage;
import com.apexfintech.checkdeposit.domain.Transfer;
import com.apexfintech.checkdeposit.domain.TransferState;
import com.apexfintech.checkdeposit.dto.ApproveRequest;
import com.apexfintech.checkdeposit.dto.OperatorQueueItem;
import com.apexfintech.checkdeposit.dto.OperatorQueueItem.RiskIndicators;
import com.apexfintech.checkdeposit.dto.RejectRequest;
import com.apexfintech.checkdeposit.ledger.LedgerPostingService;
import com.apexfintech.checkdeposit.repository.AccountRepository;
import com.apexfintech.checkdeposit.repository.AuditLogRepository;
import com.apexfintech.checkdeposit.repository.TransferRepository;
import com.apexfintech.checkdeposit.trace.TraceEventService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OperatorService {

  private static final double LOW_VENDOR_SCORE_THRESHOLD = 0.8;

  private final TransferRepository transferRepository;
  private final AccountRepository accountRepository;
  private final LedgerPostingService ledgerPostingService;
  private final AuditLogRepository auditLogRepository;
  private final TraceEventService traceEventService;

  public OperatorService(
      TransferRepository transferRepository,
      AccountRepository accountRepository,
      LedgerPostingService ledgerPostingService,
      AuditLogRepository auditLogRepository,
      TraceEventService traceEventService) {
    this.transferRepository = transferRepository;
    this.accountRepository = accountRepository;
    this.ledgerPostingService = ledgerPostingService;
    this.auditLogRepository = auditLogRepository;
    this.traceEventService = traceEventService;
  }

  /**
   * Returns flagged deposits (or filtered by status). Default status is ANALYZING when not
   * specified.
   */
  public List<OperatorQueueItem> getQueue(
      TransferState status,
      String dateFrom,
      String dateTo,
      String accountId,
      BigDecimal minAmount,
      BigDecimal maxAmount) {
    TransferState effectiveStatus = status != null ? status : TransferState.ANALYZING;
    Instant from = TransferSpecs.parseDateStartOfDay(dateFrom);
    Instant to = TransferSpecs.parseDateEndOfDay(dateTo);

    String internalAccountId = resolveAccountIdForFilter(accountId);

    Specification<Transfer> spec =
        TransferSpecs.forOperatorQueue(
            effectiveStatus, from, to, internalAccountId, minAmount, maxAmount);

    List<Transfer> transfers =
        transferRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "createdAt"));

    return transfers.stream().map(this::toQueueItem).toList();
  }

  @Transactional
  public void approve(UUID transferId, ApproveRequest request, String operatorId) {
    Transfer transfer =
        transferRepository
            .findById(transferId)
            .orElseThrow(() -> new TransferNotFoundException(transferId));

    if (request != null
        && request.contributionTypeOverride() != null
        && !request.contributionTypeOverride().isBlank()) {
      transfer.setContributionType(request.contributionTypeOverride().trim());
      transferRepository.save(transfer);
      auditLogRepository.save(
          new AuditLog(
              UUID.randomUUID(),
              operatorId,
              "CONTRIBUTION_TYPE_OVERRIDE",
              transferId,
              "{\"contributionTypeOverride\":\"" + request.contributionTypeOverride() + "\"}",
              java.time.Instant.now()));
    }

    ledgerPostingService.postApprovedDeposit(transferId);
    auditLogRepository.save(
        new AuditLog(
            UUID.randomUUID(),
            operatorId,
            "APPROVE",
            transferId,
            request != null ? "{}" : "{}",
            java.time.Instant.now()));
    traceEventService.record(
        transferId,
        TraceStage.OPERATOR_ACTION,
        "APPROVE",
        java.util.Map.of(
            "operatorId",
            operatorId,
            "contributionTypeOverride",
            request != null && request.contributionTypeOverride() != null
                ? request.contributionTypeOverride()
                : ""));
  }

  @Transactional
  public void reject(UUID transferId, RejectRequest request, String operatorId) {
    if (request == null || request.reason() == null || request.reason().isBlank()) {
      throw new InvalidRejectRequestException("Reject requires non-empty reason");
    }

    Transfer transfer =
        transferRepository
            .findById(transferId)
            .orElseThrow(() -> new TransferNotFoundException(transferId));

    if (transfer.getState() != TransferState.ANALYZING) {
      throw new IllegalStateException(
          "Transfer " + transferId + " is not in ANALYZING state, cannot reject");
    }

    transfer.setState(TransferState.REJECTED);
    transfer.setRejectionReason(request.reason().trim());
    transfer.setUpdatedAt(java.time.Instant.now());
    transferRepository.save(transfer);

    auditLogRepository.save(
        new AuditLog(
            UUID.randomUUID(),
            operatorId,
            "REJECT",
            transferId,
            "{\"reason\":\"" + escapeJson(request.reason()) + "\"}",
            java.time.Instant.now()));
    traceEventService.record(
        transferId,
        TraceStage.OPERATOR_ACTION,
        "REJECT",
        java.util.Map.of("operatorId", operatorId, "reason", request.reason().trim()));
  }

  private OperatorQueueItem toQueueItem(Transfer t) {
    String investorAccountId =
        accountRepository
            .findByInternalNumber(t.getToAccountId())
            .map(a -> a.getExternalId())
            .orElse(t.getToAccountId());

    boolean amountMismatch =
        t.getOcrAmount() != null
            && t.getAmount() != null
            && t.getOcrAmount().compareTo(t.getAmount()) != 0;
    boolean lowVendorScore =
        t.getVendorScore() != null && t.getVendorScore() < LOW_VENDOR_SCORE_THRESHOLD;

    String frontBase64 =
        t.getFrontImageData() != null && t.getFrontImageData().length > 0
            ? Base64.getEncoder().encodeToString(t.getFrontImageData())
            : null;
    String backBase64 =
        t.getBackImageData() != null && t.getBackImageData().length > 0
            ? Base64.getEncoder().encodeToString(t.getBackImageData())
            : null;

    return new OperatorQueueItem(
        t.getId(),
        t.getState(),
        investorAccountId,
        t.getAmount(),
        t.getOcrAmount(),
        t.getMicrData(),
        t.getMicrConfidence(),
        t.getVendorScore(),
        new RiskIndicators(amountMismatch, lowVendorScore),
        frontBase64,
        backBase64,
        t.getCreatedAt());
  }

  private String resolveAccountIdForFilter(String accountId) {
    if (accountId == null || accountId.isBlank()) {
      return null;
    }
    return accountRepository
        .findByExternalId(accountId.trim())
        .map(Account::getInternalNumber)
        .orElse(accountId.trim());
  }

  private static String escapeJson(String s) {
    if (s == null) return "";
    return s.replace("\\", "\\\\").replace("\"", "\\\"");
  }
}
