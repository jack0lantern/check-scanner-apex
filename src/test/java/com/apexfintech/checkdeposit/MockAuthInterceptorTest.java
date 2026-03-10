package com.apexfintech.checkdeposit;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.apexfintech.checkdeposit.config.WebMvcConfig;
import com.apexfintech.checkdeposit.deposit.DepositService;
import com.apexfintech.checkdeposit.funding.AccountResolutionService;
import com.apexfintech.checkdeposit.ledger.LedgerPostingService;
import com.apexfintech.checkdeposit.ledger.LedgerQueryService;
import com.apexfintech.checkdeposit.ledger.ReturnService;
import com.apexfintech.checkdeposit.operator.OperatorService;
import com.apexfintech.checkdeposit.repository.AuditLogRepository;
import com.apexfintech.checkdeposit.repository.TransferRepository;
import com.apexfintech.checkdeposit.settlement.SettlementAckService;
import com.apexfintech.checkdeposit.settlement.SettlementDateService;
import com.apexfintech.checkdeposit.trace.TraceEventService;
import com.apexfintech.checkdeposit.vendor.VendorService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest
@Import(WebMvcConfig.class)
class MockAuthInterceptorTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private AccountResolutionService accountResolutionService;
  @MockBean private VendorService vendorService;
  @MockBean private DepositService depositService;
  @MockBean private LedgerPostingService ledgerPostingService;
  @MockBean private LedgerQueryService ledgerQueryService;
  @MockBean private ReturnService returnService;
  @MockBean private SettlementAckService settlementAckService;
  @MockBean private OperatorService operatorService;
  @MockBean private TraceEventService traceEventService;
  @MockBean private TransferRepository transferRepository;
  @MockBean private AuditLogRepository auditLogRepository;
  @MockBean private SettlementDateService settlementDateService;

  @Test
  void authTest_returns200_whenHeadersPresent() throws Exception {
    mockMvc
        .perform(
            get("/debug/auth-test")
                .header("X-User-Role", "INVESTOR")
                .header("X-Account-Id", "TEST001"))
        .andExpect(status().isOk());
  }

  @Test
  void authTest_returns401_whenUserRoleMissing() throws Exception {
    mockMvc
        .perform(get("/debug/auth-test").header("X-Account-Id", "TEST001"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void authTest_returns401_whenAccountIdMissing() throws Exception {
    mockMvc
        .perform(get("/debug/auth-test").header("X-User-Role", "INVESTOR"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void authTest_returns401_whenBothHeadersMissing() throws Exception {
    mockMvc.perform(get("/debug/auth-test")).andExpect(status().isUnauthorized());
  }

  @Test
  void authTest_returns401_whenHeadersBlank() throws Exception {
    mockMvc
        .perform(
            get("/debug/auth-test")
                .header("X-User-Role", "   ")
                .header("X-Account-Id", ""))
        .andExpect(status().isUnauthorized());
  }
}
