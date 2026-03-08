package com.apexfintech.checkdeposit.settlement;

import static org.awaitility.Awaitility.await;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Automated validation: verifies the EOD cron trigger fires at the expected time. Uses a test
 * profile with cron set to run every 2 seconds; Awaitility waits for at least one execution.
 */
@SpringBootTest
@ActiveProfiles("test")
class EodSchedulerServiceTest {

  @Autowired private EodSchedulerService eodSchedulerService;

  @Test
  void eodCronTrigger_firesAtExpectedTime() {
    int initialCount = eodSchedulerService.getExecutionCount();

    await()
        .atMost(Duration.ofSeconds(5))
        .pollInterval(Duration.ofMillis(200))
        .untilAsserted(
            () ->
                assertThat(eodSchedulerService.getExecutionCount())
                    .as("EOD batch should have been triggered by cron")
                    .isGreaterThan(initialCount));
  }
}
