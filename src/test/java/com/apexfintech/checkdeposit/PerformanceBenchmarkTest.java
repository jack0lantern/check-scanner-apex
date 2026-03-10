package com.apexfintech.checkdeposit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.util.StopWatch;

/**
 * Performance benchmark tests using StopWatch and Awaitility: Vendor stub response &lt; 1 second;
 * after flagging a deposit, it appears in GET /operator/queue within 1 second; after any
 * state-changing action, the new state is queryable within 1 second.
 */
@SpringBootTest(properties = "spring.task.scheduling.enabled=false")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class PerformanceBenchmarkTest {

  private static final String VALID_BASE64_IMAGE =
      Base64.getEncoder().encodeToString(new byte[] {0x01, 0x02, 0x03});

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  @Test
  void vendorStubResponse_underOneSecond() throws Exception {
    StopWatch stopWatch = new StopWatch();
    stopWatch.start();

    mockMvc
        .perform(
            get("/debug/vendor-stub")
                .param("accountId", "clean-pass")
                .header("X-User-Role", "INVESTOR")
                .header("X-Account-Id", "TEST001"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.scenario").exists());

    stopWatch.stop();
    assertThat(stopWatch.getTotalTimeMillis()).isLessThan(1000);
  }

  @Test
  void flaggedDeposit_appearsInOperatorQueueWithinOneSecond() throws Exception {
    String transferId = submitDepositForOperatorReview();

    await()
        .atMost(1, TimeUnit.SECONDS)
        .untilAsserted(
            () -> {
              MvcResult result =
                  mockMvc
                      .perform(
                          get("/operator/queue")
                              .header("X-User-Role", "OPERATOR")
                              .header("X-Account-Id", "op1"))
                      .andExpect(status().isOk())
                      .andReturn();
              assertThat(result.getResponse().getContentAsString()).contains(transferId);
            });
  }

  @Test
  void afterApprove_newStateQueryableWithinOneSecond() throws Exception {
    String transferId = submitDepositForOperatorReview();

    mockMvc
        .perform(
            post("/operator/queue/{transferId}/approve", transferId)
                .header("X-User-Role", "OPERATOR")
                .header("X-Account-Id", "op1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isOk());

    await()
        .atMost(1, TimeUnit.SECONDS)
        .untilAsserted(
            () -> {
              MvcResult result =
                  mockMvc
                      .perform(
                          get("/deposits/{transferId}", transferId)
                              .header("X-User-Role", "INVESTOR")
                              .header("X-Account-Id", "TEST001"))
                      .andExpect(status().isOk())
                      .andReturn();
              String state =
                  objectMapper
                      .readTree(result.getResponse().getContentAsString())
                      .get("state")
                      .asText();
              assertThat(state).isIn("APPROVED", "COMPLETED");
            });
  }

  private String submitDepositForOperatorReview() throws Exception {
    String response =
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
                                200.00,
                                "accountId",
                                "TEST001"))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.transferId").exists())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return objectMapper.readTree(response).get("transferId").asText();
  }
}
