package com.apexfintech.checkdeposit.operator;

import com.apexfintech.checkdeposit.domain.Transfer;
import com.apexfintech.checkdeposit.domain.TransferState;
import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

final class TransferSpecs {

  private TransferSpecs() {}

  static Specification<Transfer> forOperatorQueue(
      TransferState status,
      Instant dateFrom,
      Instant dateTo,
      String accountId,
      BigDecimal minAmount,
      BigDecimal maxAmount) {
    return (root, query, cb) -> {
      List<Predicate> predicates = new ArrayList<>();

      if (status != null) {
        predicates.add(cb.equal(root.get("state"), status));
      }

      if (dateFrom != null) {
        predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), dateFrom));
      }

      if (dateTo != null) {
        predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), dateTo));
      }

      if (accountId != null && !accountId.isBlank()) {
        predicates.add(cb.equal(root.get("toAccountId"), accountId));
      }

      if (minAmount != null) {
        predicates.add(cb.greaterThanOrEqualTo(root.get("amount"), minAmount));
      }

      if (maxAmount != null) {
        predicates.add(cb.lessThanOrEqualTo(root.get("amount"), maxAmount));
      }

      return cb.and(predicates.toArray(new Predicate[0]));
    };
  }

  static Instant parseDateStartOfDay(String dateStr) {
    if (dateStr == null || dateStr.isBlank()) {
      return null;
    }
    try {
      return LocalDate.parse(dateStr).atStartOfDay(ZoneOffset.UTC).toInstant();
    } catch (Exception e) {
      return null;
    }
  }

  static Instant parseDateEndOfDay(String dateStr) {
    if (dateStr == null || dateStr.isBlank()) {
      return null;
    }
    try {
      return LocalDate.parse(dateStr).plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
    } catch (Exception e) {
      return null;
    }
  }
}
