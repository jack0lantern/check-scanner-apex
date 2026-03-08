package com.apexfintech.checkdeposit.repository;

import com.apexfintech.checkdeposit.domain.LedgerEntry;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, UUID> {

  List<LedgerEntry> findByTransactionId(UUID transactionId);

  Page<LedgerEntry> findByAccountIdOrderByCreatedAtDesc(String accountId, Pageable pageable);

  @Query(
      """
      SELECT COALESCE(SUM(CASE WHEN e.type = 'CREDIT' THEN e.amount ELSE -e.amount END), 0)
      FROM LedgerEntry e WHERE e.accountId = :accountId
      """)
  BigDecimal sumBalanceByAccountId(@Param("accountId") String accountId);
}
