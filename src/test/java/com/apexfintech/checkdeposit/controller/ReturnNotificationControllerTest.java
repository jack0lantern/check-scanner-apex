package com.apexfintech.checkdeposit.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.apexfintech.checkdeposit.config.WebMvcConfig;
import com.apexfintech.checkdeposit.deposit.TransferNotFoundException;
import com.apexfintech.checkdeposit.ledger.ReturnService;
import com.apexfintech.checkdeposit.settlement.SettlementAckService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(InternalController.class)
@Import(WebMvcConfig.class)
class ReturnNotificationControllerTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  @MockBean private ReturnService returnService;
  @MockBean private SettlementAckService settlementAckService;

  @Test
  void processReturn_returns403_withoutOperatorRole() throws Exception {
    mockMvc
        .perform(
            post("/internal/returns")
                .header("X-User-Role", "INVESTOR")
                .header("X-Account-Id", "TEST001")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        Map.of("transferId", UUID.randomUUID().toString(), "returnReason", "NSF"))))
        .andExpect(status().isForbidden());
  }

  @Test
  void processReturn_returns200_withValidPayloadAndOperatorRole() throws Exception {
    UUID transferId = UUID.randomUUID();
    doNothing().when(returnService).processReturn(eq(transferId), eq("NSF"));

    mockMvc
        .perform(
            post("/internal/returns")
                .header("X-User-Role", "OPERATOR")
                .header("X-Account-Id", "OP-001")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        Map.of("transferId", transferId.toString(), "returnReason", "NSF"))))
        .andExpect(status().isOk());
  }

  @Test
  void processReturn_returns404_forUnknownTransferId() throws Exception {
    UUID unknownTransferId = UUID.randomUUID();
    doThrow(new TransferNotFoundException(unknownTransferId))
        .when(returnService)
        .processReturn(eq(unknownTransferId), eq("NSF"));

    mockMvc
        .perform(
            post("/internal/returns")
                .header("X-User-Role", "OPERATOR")
                .header("X-Account-Id", "OP-001")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        Map.of("transferId", unknownTransferId.toString(), "returnReason", "NSF"))))
        .andExpect(status().isNotFound());
  }
}
