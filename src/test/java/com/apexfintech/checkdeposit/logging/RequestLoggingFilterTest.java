package com.apexfintech.checkdeposit.logging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Automated validation for structured request logging: asserts depositSource, transferId, and
 * traceId are present in the structured log output when a request is submitted.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RequestLoggingFilterTest {

  private static final String VALID_BASE64_IMAGE =
      Base64.getEncoder().encodeToString(new byte[] {0x01, 0x02, 0x03});

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  private ListAppender<ILoggingEvent> listAppender;
  private Logger requestLoggingLogger;

  @BeforeEach
  void setUp() {
    listAppender = new ListAppender<>();
    listAppender.start();
    requestLoggingLogger = (Logger) LoggerFactory.getLogger(RequestLoggingFilter.class);
    requestLoggingLogger.addAppender(listAppender);
    requestLoggingLogger.setLevel(Level.INFO);
  }

  @AfterEach
  void tearDown() {
    requestLoggingLogger.detachAppender(listAppender);
  }

  @Test
  void depositSubmission_logsContainDepositSourceTraceIdAndTransferIdWhenAvailable()
      throws Exception {
    // 1. Submit deposit to get a transferId
    String responseBody =
        mockMvc
            .perform(
                post("/deposits")
                    .header("X-User-Role", "INVESTOR")
                    .header("X-Account-Id", "clean-pass")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(
                            Map.of(
                                "frontImage",
                                VALID_BASE64_IMAGE,
                                "backImage",
                                VALID_BASE64_IMAGE,
                                "amount",
                                100.50,
                                "accountId",
                                "TEST001"))))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

    String transferId = objectMapper.readTree(responseBody).get("transferId").asText();

    // 2. Assert structured fields in log output
    assertThat(listAppender.list).isNotEmpty();
    ILoggingEvent logEvent = listAppender.list.get(0);

    assertThat(logEvent.getMDCPropertyMap())
        .containsKey("traceId")
        .containsEntry("depositSource", "MOBILE");

    String traceId = logEvent.getMDCPropertyMap().get("traceId");
    assertThat(traceId).isNotNull();
    assertThat(UUID.fromString(traceId)).isNotNull();

    // 3. GET /deposits/{transferId} - should have transferId in MDC
    listAppender.list.clear();
    mockMvc
        .perform(
            get("/deposits/{transferId}", transferId)
                .header("X-User-Role", "INVESTOR")
                .header("X-Account-Id", "clean-pass"))
        .andExpect(status().isOk());

    assertThat(listAppender.list).isNotEmpty();
    ILoggingEvent getLogEvent = listAppender.list.get(0);
    assertThat(getLogEvent.getMDCPropertyMap())
        .containsEntry("depositSource", "MOBILE")
        .containsEntry("transferId", transferId);
  }

  @Test
  void operatorRequest_logsContainDepositSourceOperator() throws Exception {
    listAppender.list.clear();

    mockMvc
        .perform(
            get("/operator/queue").header("X-User-Role", "OPERATOR").header("X-Account-Id", "op1"))
        .andExpect(status().isOk());

    assertThat(listAppender.list).isNotEmpty();
    ILoggingEvent logEvent = listAppender.list.get(0);
    assertThat(logEvent.getMDCPropertyMap()).containsEntry("depositSource", "OPERATOR");
  }

  @Test
  void requestWithoutAuthHeader_logsContainDepositSourceUnknown() throws Exception {
    listAppender.list.clear();

    // Request without X-User-Role (will get 401 from auth, but filter runs first)
    mockMvc
        .perform(post("/deposits").contentType(MediaType.APPLICATION_JSON).content("{}"))
        .andExpect(status().isUnauthorized());

    assertThat(listAppender.list).isNotEmpty();
    ILoggingEvent logEvent = listAppender.list.get(0);
    assertThat(logEvent.getMDCPropertyMap()).containsEntry("depositSource", "UNKNOWN");
  }
}
