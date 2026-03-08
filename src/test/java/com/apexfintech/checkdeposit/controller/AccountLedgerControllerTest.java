package com.apexfintech.checkdeposit.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.apexfintech.checkdeposit.domain.LedgerEntry;
import com.apexfintech.checkdeposit.repository.LedgerEntryRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AccountLedgerControllerTest {

  private static final String TEST_ACCOUNT_ID = "INT-12345678";
  private static final String COUNTERPARTY_ID = "OMN-999";

  @Autowired private MockMvc mockMvc;
  @Autowired private LedgerEntryRepository ledgerEntryRepository;

  @BeforeEach
  void setUp() {
    ledgerEntryRepository.deleteAll();
  }

  @Test
  void getBalance_returnsCorrectNetBalanceFromCreditAndDebit() throws Exception {
    UUID transactionId = UUID.randomUUID();
    Instant now = Instant.now();

    ledgerEntryRepository.save(
        new LedgerEntry(
            UUID.randomUUID(),
            TEST_ACCOUNT_ID,
            transactionId,
            "CREDIT",
            new BigDecimal("100.00"),
            COUNTERPARTY_ID,
            now));

    ledgerEntryRepository.save(
        new LedgerEntry(
            UUID.randomUUID(),
            TEST_ACCOUNT_ID,
            transactionId,
            "DEBIT",
            new BigDecimal("30.00"),
            COUNTERPARTY_ID,
            now));

    mockMvc
        .perform(
            get("/accounts/{accountId}/balance", TEST_ACCOUNT_ID)
                .header("X-User-Role", "INVESTOR")
                .header("X-Account-Id", "TEST001"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.balance").value(70.00));
  }

  @Test
  void getBalance_resolvesExternalIdToLedgerAccount() throws Exception {
    ledgerEntryRepository.save(
        new LedgerEntry(
            UUID.randomUUID(),
            TEST_ACCOUNT_ID,
            UUID.randomUUID(),
            "CREDIT",
            new BigDecimal("50.25"),
            COUNTERPARTY_ID,
            Instant.now()));

    mockMvc
        .perform(
            get("/accounts/{accountId}/balance", "TEST001")
                .header("X-User-Role", "INVESTOR")
                .header("X-Account-Id", "TEST001"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.balance").value(50.25));
  }

  @Test
  void getLedger_returnsPaginatedEntries() throws Exception {
    ledgerEntryRepository.save(
        new LedgerEntry(
            UUID.randomUUID(),
            TEST_ACCOUNT_ID,
            UUID.randomUUID(),
            "CREDIT",
            new BigDecimal("100.00"),
            COUNTERPARTY_ID,
            Instant.now()));

    mockMvc
        .perform(
            get("/accounts/{accountId}/ledger", TEST_ACCOUNT_ID)
                .header("X-User-Role", "INVESTOR")
                .header("X-Account-Id", "TEST001"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray())
        .andExpect(jsonPath("$.content.length()").value(1))
        .andExpect(jsonPath("$.content[0].type").value("CREDIT"))
        .andExpect(jsonPath("$.content[0].amount").value(100.00))
        .andExpect(jsonPath("$.content[0].counterpartyAccountId").value(COUNTERPARTY_ID))
        .andExpect(jsonPath("$.content[0].entryId").exists())
        .andExpect(jsonPath("$.content[0].transactionId").exists())
        .andExpect(jsonPath("$.content[0].timestamp").exists());
  }
}
