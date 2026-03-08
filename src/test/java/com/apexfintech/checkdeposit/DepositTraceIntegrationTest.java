package com.apexfintech.checkdeposit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Base64;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Integration test: walks a deposit through submission → vendor validation → funding service →
 * operator approval, then calls the trace endpoint and asserts all 4 stages appear in order with
 * correct outcomes.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class DepositTraceIntegrationTest {

  private static final String VALID_BASE64_IMAGE =
      Base64.getEncoder().encodeToString(new byte[] {0x01, 0x02, 0x03});

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  @Test
  void traceEndpoint_returnsAllFourStagesInOrder_afterFullDepositFlow() throws Exception {
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

    // 3. Call trace endpoint
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
    assertThat(traceArray.size()).isGreaterThanOrEqualTo(4);

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
  }
}
