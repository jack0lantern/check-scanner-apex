package com.apexfintech.checkdeposit.funding;

import com.apexfintech.checkdeposit.domain.Account;
import com.apexfintech.checkdeposit.dto.ResolvedAccount;
import com.apexfintech.checkdeposit.exception.AccountNotFoundException;
import com.apexfintech.checkdeposit.repository.AccountRepository;
import org.springframework.stereotype.Service;

@Service
public class AccountResolutionService {

  private final AccountRepository accountRepository;

  public AccountResolutionService(AccountRepository accountRepository) {
    this.accountRepository = accountRepository;
  }

  public ResolvedAccount resolve(String externalAccountId) {
    Account account =
        accountRepository
            .findByExternalId(externalAccountId)
            .orElseThrow(() -> new AccountNotFoundException(externalAccountId));
    return new ResolvedAccount(
        account.getInternalNumber(),
        account.getExternalId(),
        account.getRoutingNumber(),
        account.getMicrAccountNumber(),
        account.getOmnibusId(),
        account.getAccountType());
  }
}
