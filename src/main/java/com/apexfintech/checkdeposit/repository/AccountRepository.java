package com.apexfintech.checkdeposit.repository;

import com.apexfintech.checkdeposit.domain.Account;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account, UUID> {

  Optional<Account> findByExternalId(String externalId);
}
