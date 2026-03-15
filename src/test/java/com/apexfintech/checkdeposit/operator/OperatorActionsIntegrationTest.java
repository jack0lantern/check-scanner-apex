package com.apexfintech.checkdeposit.operator;

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
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration test: verifies that operator approve/reject actions are persisted to audit_logs and
 * returned by GET /operator/actions.
 */
@SpringBootTest(properties = "spring.task.scheduling.enabled=false")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class OperatorActionsIntegrationTest {

  private static final String VALID_BASE64_IMAGE =
      Base64.getEncoder().encodeToString(new byte[] {0x01, 0x02, 0x03});

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  @Test
  void approve_persistsToAuditLog_andGetActionsReturnsIt() throws Exception {
    // 1. Submit deposit that requires operator review (micr-fail → ANALYZING)
    String submitResponse =
        mockMvc
            .perform(
                post("/deposits")
                    .header("X-User-Role", "INVESTOR")
                    .header("X-Account-Id", "micr-fail")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(
                            Map.of(
                                "frontImage",
                                VALID_BASE64_IMAGE,
                                "backImage",
                                VALID_BASE64_IMAGE,
                                "amount",
                                100.00,
                                "accountId",
                                "TEST001"))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.transferId").exists())
            .andExpect(jsonPath("$.state").value("ANALYZING"))
            .andReturn()
            .getResponse()
            .getContentAsString();

    String transferId = objectMapper.readTree(submitResponse).get("transferId").asText();

    // 2. Approve via operator endpoint
    mockMvc
        .perform(
            post("/operator/queue/{transferId}/approve", transferId)
                .header("X-User-Role", "OPERATOR")
                .header("X-Account-Id", "op1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isOk());

    // 3. GET /operator/actions returns the APPROVE action
    String actionsResponse =
        mockMvc
            .perform(
                get("/operator/actions")
                    .header("X-User-Role", "OPERATOR")
                    .header("X-Account-Id", "op1"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

    JsonNode actions = objectMapper.readTree(actionsResponse);
    assertThat(actions.isArray()).isTrue();
    assertThat(actions.size()).isGreaterThanOrEqualTo(1);

    JsonNode approveAction = null;
    for (int i = 0; i < actions.size(); i++) {
      JsonNode a = actions.get(i);
      if ("APPROVE".equals(a.get("action").asText())
          && transferId.equals(a.get("transferId").asText())) {
        approveAction = a;
        break;
      }
    }
    assertThat(approveAction).isNotNull();
    assertThat(approveAction.get("operatorId").asText()).isEqualTo("op1");
    assertThat(approveAction.get("createdAt").asText()).isNotBlank();
  }

  @Test
  void reject_persistsToAuditLog_andGetActionsReturnsIt() throws Exception {
    // 1. Submit deposit that requires operator review
    String submitResponse =
        mockMvc
            .perform(
                post("/deposits")
                    .header("X-User-Role", "INVESTOR")
                    .header("X-Account-Id", "micr-fail")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(
                            Map.of(
                                "frontImage",
                                VALID_BASE64_IMAGE,
                                "backImage",
                                VALID_BASE64_IMAGE,
                                "amount",
                                50.00,
                                "accountId",
                                "TEST001"))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.state").value("ANALYZING"))
            .andReturn()
            .getResponse()
            .getContentAsString();

    String transferId = objectMapper.readTree(submitResponse).get("transferId").asText();

    // 2. Reject via operator endpoint
    mockMvc
        .perform(
            post("/operator/queue/{transferId}/reject", transferId)
                .header("X-User-Role", "OPERATOR")
                .header("X-Account-Id", "op1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(Map.of("reason", "Suspicious check image"))))
        .andExpect(status().isOk());

    // 3. GET /operator/actions returns the REJECT action
    String actionsResponse =
        mockMvc
            .perform(
                get("/operator/actions")
                    .header("X-User-Role", "OPERATOR")
                    .header("X-Account-Id", "op1"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

    JsonNode actions = objectMapper.readTree(actionsResponse);
    JsonNode rejectAction = null;
    for (int i = 0; i < actions.size(); i++) {
      JsonNode a = actions.get(i);
      if ("REJECT".equals(a.get("action").asText())
          && transferId.equals(a.get("transferId").asText())) {
        rejectAction = a;
        break;
      }
    }
    assertThat(rejectAction).isNotNull();
    assertThat(rejectAction.get("detail").asText()).contains("Suspicious check image");
  }
}
