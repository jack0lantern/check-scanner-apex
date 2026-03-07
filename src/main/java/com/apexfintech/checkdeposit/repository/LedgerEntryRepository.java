package com.apexfintech.checkdeposit.repository;

import com.apexfintech.checkdeposit.domain.LedgerEntry;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, UUID> {

  List<LedgerEntry> findByTransactionId(UUID transactionId);
}
