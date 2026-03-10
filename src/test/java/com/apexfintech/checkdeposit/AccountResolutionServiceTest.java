package com.apexfintech.checkdeposit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.apexfintech.checkdeposit.domain.Account;
import com.apexfintech.checkdeposit.dto.ResolvedAccount;
import com.apexfintech.checkdeposit.exception.AccountNotFoundException;
import com.apexfintech.checkdeposit.funding.AccountResolutionService;
import com.apexfintech.checkdeposit.repository.AccountRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AccountResolutionServiceTest {

  @Mock private AccountRepository accountRepository;

  @InjectMocks private AccountResolutionService accountResolutionService;

  @Test
  void resolve_returnsInternalNumberRoutingNumberAndOmnibusId_forKnownAccountId() {
    Account account =
        new Account(
            UUID.randomUUID(),
            "TEST001",
            "INT-12345678",
            "021000021",
            "12345678",
            "OMN-999",
            "RETIREMENT");
    when(accountRepository.findByExternalId("TEST001")).thenReturn(Optional.of(account));

    ResolvedAccount resolved = accountResolutionService.resolve("TEST001");

    assertThat(resolved.internalNumber()).isEqualTo("INT-12345678");
    assertThat(resolved.routingNumber()).isEqualTo("021000021");
    assertThat(resolved.micrAccountNumber()).isEqualTo("12345678");
    assertThat(resolved.omnibusAccountId()).isEqualTo("OMN-999");
    assertThat(resolved.accountType()).isEqualTo("RETIREMENT");
    verify(accountRepository).findByExternalId("TEST001");
  }

  @Test
  void resolve_throwsAccountNotFoundException_forUnknownAccountId() {
    when(accountRepository.findByExternalId("UNKNOWN999")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> accountResolutionService.resolve("UNKNOWN999"))
        .isInstanceOf(AccountNotFoundException.class)
        .hasMessageContaining("UNKNOWN999");

    verify(accountRepository).findByExternalId("UNKNOWN999");
  }
}
