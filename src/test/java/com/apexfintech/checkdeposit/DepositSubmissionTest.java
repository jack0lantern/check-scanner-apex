package com.apexfintech.checkdeposit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.apexfintech.checkdeposit.domain.TransferState;
import com.apexfintech.checkdeposit.repository.TransferRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Base64;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DepositSubmissionTest {

  private static final String VALID_BASE64_IMAGE = Base64.getEncoder().encodeToString(new byte[] {0x01, 0x02, 0x03});

  @Autowired private MockMvc mockMvc;
  @Autowired private TransferRepository transferRepository;
  @Autowired private ObjectMapper objectMapper;

  @Test
  void submitDeposit_withIqaBlurAccount_returns422WithActionableMessage() throws Exception {
    ResultActions result =
        mockMvc.perform(
            post("/deposits")
                .header("X-User-Role", "INVESTOR")
                .header("X-Account-Id", "iqa-blur")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        Map.of(
                            "frontImage", VALID_BASE64_IMAGE,
                            "backImage", VALID_BASE64_IMAGE,
                            "amount", 100.50,
                            "accountId", "TEST001"))));

    result
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.transferId").exists())
        .andExpect(jsonPath("$.actionableMessage").exists())
        .andExpect(jsonPath("$.actionableMessage").value("Image too blurry — please retake in better lighting"));
  }

  @Test
  void retryDeposit_withRetryForTransferId_updatesExistingTransferAndAdvancesState() throws Exception {
    // 1. Submit deposit triggering IQA Blur -> 422
    String firstResponse =
        mockMvc
            .perform(
                post("/deposits")
                    .header("X-User-Role", "INVESTOR")
                    .header("X-Account-Id", "iqa-blur")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(
                            Map.of(
                                "frontImage", VALID_BASE64_IMAGE,
                                "backImage", VALID_BASE64_IMAGE,
                                "amount", 100.50,
                                "accountId", "TEST001"))))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.transferId").exists())
            .andExpect(jsonPath("$.actionableMessage").exists())
            .andReturn()
            .getResponse()
            .getContentAsString();

    String transferId = objectMapper.readTree(firstResponse).get("transferId").asText();
    long countAfterFirst = transferRepository.count();

    // 2. Re-submit with retryForTransferId using clean-pass to succeed
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
                            "amount", 100.50,
                            "accountId", "TEST001",
                            "retryForTransferId", transferId))))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.transferId").value(transferId))
        .andExpect(jsonPath("$.state").value("ANALYZING"));

    // 3. Assert no new record created — same transfer updated
    assertThat(transferRepository.count()).isEqualTo(countAfterFirst);

    var transfer = transferRepository.findById(java.util.UUID.fromString(transferId)).orElseThrow();
    assertThat(transfer.getState()).isEqualTo(TransferState.ANALYZING);
  }
}
