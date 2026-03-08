package com.apexfintech.checkdeposit.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.apexfintech.checkdeposit.config.WebMvcConfig;
import com.apexfintech.checkdeposit.domain.TransferState;
import com.apexfintech.checkdeposit.dto.OperatorQueueItem;
import com.apexfintech.checkdeposit.dto.OperatorQueueItem.RiskIndicators;
import com.apexfintech.checkdeposit.exception.GlobalExceptionHandler;
import com.apexfintech.checkdeposit.operator.OperatorService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(OperatorController.class)
@Import({WebMvcConfig.class, GlobalExceptionHandler.class})
class OperatorControllerWebMvcTest {

  private static final String FRONT_BASE64 =
      java.util.Base64.getEncoder().encodeToString(new byte[] {0x01, 0x02, 0x03});
  private static final String BACK_BASE64 =
      java.util.Base64.getEncoder().encodeToString(new byte[] {0x04, 0x05, 0x06});

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  @MockBean private OperatorService operatorService;

  @Test
  void getQueue_returns403_withoutOperatorRole() throws Exception {
    mockMvc
        .perform(
            get("/operator/queue")
                .header("X-User-Role", "INVESTOR")
                .header("X-Account-Id", "TEST001"))
        .andExpect(status().isForbidden());
  }

  @Test
  void getQueue_returns200_withQueueContainingRequiredFields() throws Exception {
    UUID transferId = UUID.randomUUID();
    Instant submittedAt = Instant.parse("2025-03-08T10:00:00Z");
    OperatorQueueItem item =
        new OperatorQueueItem(
            transferId,
            TransferState.ANALYZING,
            "TEST001",
            new BigDecimal("100.50"),
            new BigDecimal("100.50"),
            "123456789|12345678|123",
            0.98,
            0.95,
            new RiskIndicators(false, false),
            FRONT_BASE64,
            BACK_BASE64,
            submittedAt);

    when(operatorService.getQueue(
            isNull(), isNull(), isNull(), isNull(), isNull(), isNull()))
        .thenReturn(List.of(item));

    mockMvc
        .perform(
            get("/operator/queue")
                .header("X-User-Role", "OPERATOR")
                .header("X-Account-Id", "OP001"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].transferId").value(transferId.toString()))
        .andExpect(jsonPath("$[0].micrData").value("123456789|12345678|123"))
        .andExpect(jsonPath("$[0].micrConfidence").value(0.98))
        .andExpect(jsonPath("$[0].vendorScore").value(0.95))
        .andExpect(jsonPath("$[0].frontImage").value(FRONT_BASE64))
        .andExpect(jsonPath("$[0].backImage").value(BACK_BASE64))
        .andExpect(jsonPath("$[0].ocrAmount").value(100.50));

    verify(operatorService)
        .getQueue(isNull(), isNull(), isNull(), isNull(), isNull(), isNull());
  }

  @Test
  void getQueue_filterByStatus_returnsOnlyMatchingRecords() throws Exception {
    when(operatorService.getQueue(
            eq(TransferState.REJECTED), isNull(), isNull(), isNull(), isNull(), isNull()))
        .thenReturn(List.of());

    mockMvc
        .perform(
            get("/operator/queue")
                .param("status", "REJECTED")
                .header("X-User-Role", "OPERATOR")
                .header("X-Account-Id", "OP001"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$.length()").value(0));

    verify(operatorService)
        .getQueue(eq(TransferState.REJECTED), isNull(), isNull(), isNull(), isNull(), isNull());
  }

  @Test
  void approve_withContributionTypeOverride_persistsOverride() throws Exception {
    UUID transferId = UUID.randomUUID();

    mockMvc
        .perform(
            post("/operator/queue/{transferId}/approve", transferId)
                .header("X-User-Role", "OPERATOR")
                .header("X-Account-Id", "OP001")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    java.util.Map.of("contributionTypeOverride", "ROTH"))))
        .andExpect(status().isOk());

    verify(operatorService).approve(eq(transferId), any(), eq("OP001"));
  }

  @Test
  void reject_requiresNonEmptyReason_returns400_whenEmpty() throws Exception {
    UUID transferId = UUID.randomUUID();

    mockMvc
        .perform(
            post("/operator/queue/{transferId}/reject", transferId)
                .header("X-User-Role", "OPERATOR")
                .header("X-Account-Id", "OP001")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(java.util.Map.of("reason", ""))))
        .andExpect(status().isBadRequest());
  }

  @Test
  void reject_requiresNonEmptyReason_returns400_whenNull() throws Exception {
    UUID transferId = UUID.randomUUID();

    mockMvc
        .perform(
            post("/operator/queue/{transferId}/reject", transferId)
                .header("X-User-Role", "OPERATOR")
                .header("X-Account-Id", "OP001")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(java.util.Map.of())))
        .andExpect(status().isBadRequest());
  }

  @Test
  void reject_returns200_withValidReason() throws Exception {
    UUID transferId = UUID.randomUUID();

    mockMvc
        .perform(
            post("/operator/queue/{transferId}/reject", transferId)
                .header("X-User-Role", "OPERATOR")
                .header("X-Account-Id", "OP001")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    java.util.Map.of("reason", "Suspicious activity"))))
        .andExpect(status().isOk());

    verify(operatorService).reject(eq(transferId), any(), eq("OP001"));
  }
}
