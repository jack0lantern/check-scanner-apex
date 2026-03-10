package com.apexfintech.checkdeposit.ledger;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

/**
 * Unit test verifying investor net impact math: investor is debited original amount + $30 fee (net
 * impact = original amount + $30, a deduction).
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = "return.fee-amount=30")
class ReturnServiceTest {

  @Autowired private ReturnService returnService;

  @Test
  void computeInvestorNetImpact_originalAmountPlusFee() {
    BigDecimal originalAmount = new BigDecimal("150.50");
    BigDecimal netImpact = returnService.computeInvestorNetImpact(originalAmount);

    // Net investor impact = original amount + $30 fee (both debits = deductions)
    assertThat(netImpact).isEqualByComparingTo(new BigDecimal("180.50"));
  }

  @Test
  void computeInvestorNetImpact_notOriginalMinusFee() {
    // Verify we are NOT doing original - fee (which would be wrong)
    BigDecimal originalAmount = new BigDecimal("100.00");
    BigDecimal netImpact = returnService.computeInvestorNetImpact(originalAmount);

    assertThat(netImpact).isEqualByComparingTo(new BigDecimal("130.00"));
    assertThat(netImpact).isNotEqualByComparingTo(new BigDecimal("70.00"));
  }
}
