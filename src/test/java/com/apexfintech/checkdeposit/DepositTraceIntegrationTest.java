package com.apexfintech.checkdeposit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.apexfintech.checkdeposit.repository.TraceEventRepository;
import com.apexfintech.checkdeposit.settlement.SettlementFileService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Integration test: walks a deposit through submission → vendor validation → funding service →
 * operator approval → settlement batch inclusion, then queries trace_events and asserts all 5+
 * stages appear in order with correct outcomes and timestamps.
 */
@SpringBootTest(properties = "spring.task.scheduling.enabled=false")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class DepositTraceIntegrationTest {

  private static final String VALID_BASE64_IMAGE =
      Base64.getEncoder().encodeToString(new byte[] {0x01, 0x02, 0x03});

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private SettlementFileService settlementFileService;
  @Autowired private TraceEventRepository traceEventRepository;

  @Test
  void traceEndpoint_returnsAllFiveStagesInOrder_afterFullDepositFlowIncludingSettlement()
      throws Exception {
    // 1. Submit deposit (clean-pass → vendor + funding pass)
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
                                "amount", 100.00,
                                "accountId", "TEST001"))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.transferId").exists())
            .andExpect(jsonPath("$.state").value("ANALYZING"))
            .andReturn()
            .getResponse()
            .getContentAsString();

    String transferId = objectMapper.readTree(submitResponse).get("transferId").asText();

    // 2. Operator approve
    mockMvc
        .perform(
            post("/operator/queue/{transferId}/approve", transferId)
                .header("X-User-Role", "OPERATOR")
                .header("X-Account-Id", "op1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isOk());

    // 3. Trigger settlement batch (EOD)
    settlementFileService.generateSettlementFile();

    // 4. Query trace_events directly and assert all stages appear with correct outcomes
    UUID transferUuid = UUID.fromString(transferId);
    var traceEvents = traceEventRepository.findByTransferIdOrderByCreatedAtAsc(transferUuid);
    assertThat(traceEvents).hasSizeGreaterThanOrEqualTo(5);
    assertThat(traceEvents.get(0).getStage().name()).isEqualTo("SUBMISSION");
    assertThat(traceEvents.get(0).getOutcome()).isEqualTo("CREATED");
    assertThat(traceEvents.get(0).getCreatedAt()).isNotNull();
    assertThat(traceEvents.get(1).getStage().name()).isEqualTo("VENDOR_RESULT");
    assertThat(traceEvents.get(1).getOutcome()).isEqualTo("PASS");
    assertThat(traceEvents.get(2).getStage().name()).isEqualTo("BUSINESS_RULE");
    assertThat(traceEvents.get(2).getOutcome()).isEqualTo("PASS");
    assertThat(traceEvents.get(3).getStage().name()).isEqualTo("OPERATOR_ACTION");
    assertThat(traceEvents.get(3).getOutcome()).isEqualTo("APPROVE");
    assertThat(traceEvents.get(4).getStage().name()).isEqualTo("SETTLEMENT");
    assertThat(traceEvents.get(4).getOutcome()).isEqualTo("INCLUDED");

    // 5. Call trace endpoint and verify same data via API
    String traceResponse =
        mockMvc
            .perform(
                get("/deposits/{transferId}/trace", transferId)
                    .header("X-User-Role", "INVESTOR")
                    .header("X-Account-Id", "TEST001"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

    JsonNode traceArray = objectMapper.readTree(traceResponse);
    assertThat(traceArray.isArray()).isTrue();
    assertThat(traceArray.size()).isGreaterThanOrEqualTo(5);

    assertThat(traceArray.get(0).get("stage").asText()).isEqualTo("SUBMISSION");
    assertThat(traceArray.get(0).get("outcome").asText()).isEqualTo("CREATED");
    assertThat(traceArray.get(0).get("timestamp")).isNotNull();

    assertThat(traceArray.get(1).get("stage").asText()).isEqualTo("VENDOR_RESULT");
    assertThat(traceArray.get(1).get("outcome").asText()).isEqualTo("PASS");
    assertThat(traceArray.get(1).get("timestamp")).isNotNull();

    assertThat(traceArray.get(2).get("stage").asText()).isEqualTo("BUSINESS_RULE");
    assertThat(traceArray.get(2).get("outcome").asText()).isEqualTo("PASS");
    assertThat(traceArray.get(2).get("timestamp")).isNotNull();

    assertThat(traceArray.get(3).get("stage").asText()).isEqualTo("OPERATOR_ACTION");
    assertThat(traceArray.get(3).get("outcome").asText()).isEqualTo("APPROVE");
    assertThat(traceArray.get(3).get("timestamp")).isNotNull();

    assertThat(traceArray.get(4).get("stage").asText()).isEqualTo("SETTLEMENT");
    assertThat(traceArray.get(4).get("outcome").asText()).isEqualTo("INCLUDED");
    assertThat(traceArray.get(4).get("timestamp")).isNotNull();
  }

  @Test
  void traceEndpoint_returnsReturnStage_afterReturnNotification() throws Exception {
    // 1. Submit and approve (same as above)
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
                                "amount", 100.00,
                                "accountId", "TEST001"))))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

    String transferId = objectMapper.readTree(submitResponse).get("transferId").asText();

    mockMvc
        .perform(
            post("/operator/queue/{transferId}/approve", transferId)
                .header("X-User-Role", "OPERATOR")
                .header("X-Account-Id", "op1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isOk());

    // 2. Post return notification (transfer stays APPROVED until we explicitly return)
    mockMvc
        .perform(
            post("/internal/returns")
                .header("X-User-Role", "OPERATOR")
                .header("X-Account-Id", "op1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        Map.of("transferId", transferId, "returnReason", "NSF"))))
        .andExpect(status().isOk());

    // 3. Query trace_events and assert RETURN stage appears
    UUID transferUuid = UUID.fromString(transferId);
    var traceEvents = traceEventRepository.findByTransferIdOrderByCreatedAtAsc(transferUuid);
    assertThat(traceEvents).hasSizeGreaterThanOrEqualTo(5);
    var returnEvent =
        traceEvents.stream()
            .filter(e -> "RETURN".equals(e.getStage().name()))
            .findFirst()
            .orElseThrow();
    assertThat(returnEvent.getOutcome()).isEqualTo("RETURNED");
    assertThat(returnEvent.getDetail()).contains("NSF");
    assertThat(returnEvent.getCreatedAt()).isNotNull();
  }
}
