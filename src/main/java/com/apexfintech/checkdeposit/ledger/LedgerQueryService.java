package com.apexfintech.checkdeposit.ledger;

import com.apexfintech.checkdeposit.domain.Account;
import com.apexfintech.checkdeposit.domain.LedgerEntry;
import com.apexfintech.checkdeposit.dto.BalanceResponse;
import com.apexfintech.checkdeposit.dto.LedgerEntryResponse;
import com.apexfintech.checkdeposit.repository.AccountRepository;
import com.apexfintech.checkdeposit.repository.LedgerEntryRepository;
import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

/**
 * Queries ledger balance and entries for accounts. Supports account lookup by external_id,
 * internal_number, or omnibus_id (ledger account id).
 */
@Service
public class LedgerQueryService {

  private final LedgerEntryRepository ledgerEntryRepository;
  private final AccountRepository accountRepository;

  public LedgerQueryService(
      LedgerEntryRepository ledgerEntryRepository, AccountRepository accountRepository) {
    this.ledgerEntryRepository = ledgerEntryRepository;
    this.accountRepository = accountRepository;
  }

  /**
   * Resolves the given accountId to the ledger account id (internal_number for investors,
   * omnibus_id for omnibus). Supports external_id, internal_number, UUID, or raw ledger id.
   */
  public String resolveLedgerAccountId(String accountId) {
    Optional<Account> byExternal = accountRepository.findByExternalId(accountId);
    if (byExternal.isPresent()) {
      return byExternal.get().getInternalNumber();
    }
    Optional<Account> byInternal = accountRepository.findByInternalNumber(accountId);
    if (byInternal.isPresent()) {
      return accountId;
    }
    try {
      UUID uuid = UUID.fromString(accountId);
      return accountRepository
          .findById(uuid)
          .map(Account::getInternalNumber)
          .orElse(accountId);
    } catch (IllegalArgumentException ignored) {
      // Not a UUID, use as-is (e.g. omnibus_id like OMN-999)
      return accountId;
    }
  }

  public BalanceResponse getBalance(String accountId) {
    String ledgerAccountId = resolveLedgerAccountId(accountId);
    BigDecimal balance =
        Objects.requireNonNullElse(
            ledgerEntryRepository.sumBalanceByAccountId(ledgerAccountId), BigDecimal.ZERO);
    return new BalanceResponse(balance);
  }

  public Page<LedgerEntryResponse> getLedgerEntries(String accountId, Pageable pageable) {
    String ledgerAccountId = resolveLedgerAccountId(accountId);
    return ledgerEntryRepository
        .findByAccountIdOrderByCreatedAtDesc(ledgerAccountId, pageable)
        .map(this::toResponse);
  }

  private LedgerEntryResponse toResponse(LedgerEntry e) {
    return new LedgerEntryResponse(
        e.getId(),
        e.getType(),
        e.getAmount(),
        e.getCounterpartyAccountId(),
        e.getTransactionId(),
        e.getCreatedAt());
  }
}
