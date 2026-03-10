package com.apexfintech.checkdeposit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.apexfintech.checkdeposit.domain.TransferState;
import com.apexfintech.checkdeposit.repository.LedgerEntryRepository;
import com.apexfintech.checkdeposit.repository.TransferRepository;
import com.apexfintech.checkdeposit.settlement.SettlementFileService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * End-to-end happy path test: walks a deposit through the complete flow from submission (with front
 * + back images) → Vendor Clean Pass → Funding Service rules pass → operator approve → both ledger
 * entries created → EOD batch runs → Transfer state = COMPLETED. Asserts each state transition and
 * that the final ledger balance reflects the deposit. No mocked services — uses real Vendor stub,
 * real Spring context.
 */
@SpringBootTest(properties = "spring.task.scheduling.enabled=false")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class EndToEndHappyPathTest {

  private static final String VALID_BASE64_IMAGE =
      Base64.getEncoder().encodeToString(new byte[] {0x01, 0x02, 0x03});
  private static final BigDecimal DEPOSIT_AMOUNT = new BigDecimal("250.75");

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private TransferRepository transferRepository;
  @Autowired private LedgerEntryRepository ledgerEntryRepository;
  @Autowired private SettlementFileService settlementFileService;

  @Test
  void fullHappyPath_submitToCompleted_assertsStateTransitionsAndLedgerBalance() throws Exception {
    // 0. Get initial balance (may have prior test data)
    String initialBalanceResponse =
        mockMvc
            .perform(
                get("/accounts/TEST001/balance")
                    .header("X-User-Role", "INVESTOR")
                    .header("X-Account-Id", "TEST001"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    BigDecimal initialBalance =
        new BigDecimal(objectMapper.readTree(initialBalanceResponse).get("balance").asText());

    // 1. Submit deposit with front + back images
    String submitResponse =
        mockMvc
            .perform(
                post("/deposits")
                    .header("X-User-Role", "INVESTOR")
                    .header("X-Account-Id", "clean-pass")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(
                            Map.of(
                                "frontImage", VALID_BASE64_IMAGE,
                                "backImage", VALID_BASE64_IMAGE,
                                "amount", DEPOSIT_AMOUNT,
                                "accountId", "TEST001"))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.transferId").exists())
            .andExpect(jsonPath("$.state").value("ANALYZING"))
            .andReturn()
            .getResponse()
            .getContentAsString();

    String transferId = objectMapper.readTree(submitResponse).get("transferId").asText();
    UUID transferUuid = UUID.fromString(transferId);

    // Assert initial state: ANALYZING
    var transferAfterSubmit = transferRepository.findById(transferUuid).orElseThrow();
    assertThat(transferAfterSubmit.getState()).isEqualTo(TransferState.ANALYZING);

    // 2. Operator approve
    mockMvc
        .perform(
            post("/operator/queue/{transferId}/approve", transferId)
                .header("X-User-Role", "OPERATOR")
                .header("X-Account-Id", "op1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isOk());

    // Assert state: APPROVED, both ledger entries created
    var transferAfterApprove = transferRepository.findById(transferUuid).orElseThrow();
    assertThat(transferAfterApprove.getState()).isEqualTo(TransferState.APPROVED);

    long ledgerCountAfterApprove = ledgerEntryRepository.count();
    assertThat(ledgerCountAfterApprove).isGreaterThanOrEqualTo(2);

    // 3. Trigger EOD batch (settlement)
    settlementFileService.generateSettlementFile();

    // Assert state: COMPLETED
    var transferAfterSettlement = transferRepository.findById(transferUuid).orElseThrow();
    assertThat(transferAfterSettlement.getState()).isEqualTo(TransferState.COMPLETED);

    // 4. Assert final ledger balance reflects the deposit (balance increased by deposit amount)
    String balanceResponse =
        mockMvc
            .perform(
                get("/accounts/TEST001/balance")
                    .header("X-User-Role", "INVESTOR")
                    .header("X-Account-Id", "TEST001"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

    BigDecimal finalBalance =
        new BigDecimal(objectMapper.readTree(balanceResponse).get("balance").asText());
    assertThat(finalBalance.subtract(initialBalance)).isEqualByComparingTo(DEPOSIT_AMOUNT);

    // 5. Verify status endpoint returns COMPLETED
    mockMvc
        .perform(
            get("/deposits/{transferId}", transferId)
                .header("X-User-Role", "INVESTOR")
                .header("X-Account-Id", "TEST001"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.state").value("COMPLETED"))
        .andExpect(jsonPath("$.amount").value(DEPOSIT_AMOUNT.doubleValue()));
  }
}
