package com.apexfintech.checkdeposit.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.apexfintech.checkdeposit.config.WebMvcConfig;
import com.apexfintech.checkdeposit.deposit.DepositService;
import com.apexfintech.checkdeposit.exception.GlobalExceptionHandler;
import com.apexfintech.checkdeposit.deposit.TransferNotFoundException;
import com.apexfintech.checkdeposit.domain.TransferState;
import com.apexfintech.checkdeposit.dto.DepositRequest;
import com.apexfintech.checkdeposit.dto.DepositResponse;
import com.apexfintech.checkdeposit.dto.IqaFailureResponse;
import com.apexfintech.checkdeposit.dto.TransferStatusResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(DepositController.class)
@Import({WebMvcConfig.class, GlobalExceptionHandler.class})
class DepositControllerWebMvcTest {

  private static final String VALID_BASE64_IMAGE =
      java.util.Base64.getEncoder().encodeToString(new byte[] {0x01, 0x02, 0x03});

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  @MockBean private DepositService depositService;

  @Test
  void submitDeposit_returns201_onValidSubmission() throws Exception {
    UUID transferId = UUID.randomUUID();
    when(depositService.submit(any(DepositRequest.class), eq("TEST001")))
        .thenReturn(new DepositResponse(transferId, TransferState.ANALYZING));

    mockMvc
        .perform(
            post("/deposits")
                .header("X-User-Role", "INVESTOR")
                .header("X-Account-Id", "TEST001")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        Map.of(
                            "frontImage", VALID_BASE64_IMAGE,
                            "backImage", VALID_BASE64_IMAGE,
                            "amount", 100.50,
                            "accountId", "TEST001"))))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.transferId").value(transferId.toString()))
        .andExpect(jsonPath("$.state").value("ANALYZING"));

    verify(depositService).submit(any(DepositRequest.class), eq("TEST001"));
  }

  @Test
  void submitDeposit_returns422_withActionableMessage_onIqaFailure() throws Exception {
    UUID transferId = UUID.randomUUID();
    when(depositService.submit(any(DepositRequest.class), eq("iqa-blur")))
        .thenReturn(
            new IqaFailureResponse(
                transferId, "Image too blurry — please retake in better lighting"));

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
        .andExpect(jsonPath("$.transferId").value(transferId.toString()))
        .andExpect(jsonPath("$.actionableMessage").value("Image too blurry — please retake in better lighting"));

    verify(depositService).submit(any(DepositRequest.class), eq("iqa-blur"));
  }

  @Test
  void getDepositStatus_returns200_withFullTransferRecord() throws Exception {
    UUID transferId = UUID.randomUUID();
    Instant createdAt = Instant.parse("2025-03-08T10:00:00Z");
    Instant updatedAt = Instant.parse("2025-03-08T10:01:00Z");
    TransferStatusResponse status =
        new TransferStatusResponse(
            transferId,
            TransferState.ANALYZING,
            new BigDecimal("100.50"),
            "TEST001",
            createdAt,
            updatedAt,
            0.95,
            "123456789|12345678|123",
            0.98,
            new BigDecimal("100.50"),
            null);

    when(depositService.getStatus(transferId)).thenReturn(status);

    mockMvc
        .perform(
            get("/deposits/{transferId}", transferId)
                .header("X-User-Role", "INVESTOR")
                .header("X-Account-Id", "TEST001"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.transferId").value(transferId.toString()))
        .andExpect(jsonPath("$.state").value("ANALYZING"))
        .andExpect(jsonPath("$.amount").value(100.50))
        .andExpect(jsonPath("$.accountId").value("TEST001"))
        .andExpect(jsonPath("$.createdAt").exists())
        .andExpect(jsonPath("$.updatedAt").exists())
        .andExpect(jsonPath("$.vendorScore").value(0.95))
        .andExpect(jsonPath("$.micrData").value("123456789|12345678|123"));

    verify(depositService).getStatus(transferId);
  }

  @Test
  void getDepositStatus_returns404_whenTransferNotFound() throws Exception {
    UUID transferId = UUID.randomUUID();
    when(depositService.getStatus(transferId)).thenThrow(new TransferNotFoundException(transferId));

    mockMvc
        .perform(
            get("/deposits/{transferId}", transferId)
                .header("X-User-Role", "INVESTOR")
                .header("X-Account-Id", "TEST001"))
        .andExpect(status().isNotFound());

    verify(depositService).getStatus(transferId);
  }
}
