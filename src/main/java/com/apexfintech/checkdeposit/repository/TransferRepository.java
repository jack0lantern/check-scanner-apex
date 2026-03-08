package com.apexfintech.checkdeposit.repository;

import com.apexfintech.checkdeposit.domain.Transfer;
import com.apexfintech.checkdeposit.domain.TransferState;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TransferRepository
    extends JpaRepository<Transfer, UUID>, JpaSpecificationExecutor<Transfer> {

  @Query(
      """
      SELECT COUNT(t) > 0 FROM Transfer t
      WHERE t.fromAccountId = :fromAccountId
        AND t.amount = :amount
        AND t.micrData = :micrData
        AND t.state <> com.apexfintech.checkdeposit.domain.TransferState.REJECTED
        AND t.createdAt >= :windowStart
        AND (t.id <> :excludeTransferId OR :excludeTransferId IS NULL)
      """)
  boolean existsNonRejectedDuplicate(
      @Param("fromAccountId") String fromAccountId,
      @Param("amount") BigDecimal amount,
      @Param("micrData") String micrData,
      @Param("excludeTransferId") UUID excludeTransferId,
      @Param("windowStart") Instant windowStart);

  @Query(
      """
      SELECT COUNT(t) > 0 FROM Transfer t
      WHERE t.fromAccountId = :fromAccountId
        AND t.micrRoutingNumber = :micrRoutingNumber
        AND t.micrAccountNumber = :micrAccountNumber
        AND t.micrCheckNumber = :micrCheckNumber
        AND t.state <> com.apexfintech.checkdeposit.domain.TransferState.REJECTED
        AND t.createdAt >= :windowStart
        AND (t.id <> :excludeTransferId OR :excludeTransferId IS NULL)
      """)
  boolean existsNonRejectedDuplicateByCheckNumber(
      @Param("fromAccountId") String fromAccountId,
      @Param("micrRoutingNumber") String micrRoutingNumber,
      @Param("micrAccountNumber") String micrAccountNumber,
      @Param("micrCheckNumber") String micrCheckNumber,
      @Param("excludeTransferId") UUID excludeTransferId,
      @Param("windowStart") Instant windowStart);

  @Query(
      """
      SELECT COALESCE(SUM(t.amount), 0) FROM Transfer t
      WHERE t.toAccountId = :toAccountId
        AND t.state IN :states
        AND t.createdAt >= :yearStart
        AND t.createdAt < :yearEnd
      """)
  BigDecimal sumApprovedContributionsForAccountInYear(
      @Param("toAccountId") String toAccountId,
      @Param("states") java.util.List<TransferState> states,
      @Param("yearStart") Instant yearStart,
      @Param("yearEnd") Instant yearEnd);
}
