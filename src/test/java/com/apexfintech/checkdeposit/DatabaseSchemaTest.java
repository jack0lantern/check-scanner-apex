package com.apexfintech.checkdeposit;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
class DatabaseSchemaTest {

  @Autowired private EntityManager entityManager;

  @Test
  void contextLoadsAndCanExecuteSelect1() {
    Object result = entityManager.createNativeQuery("SELECT 1").getSingleResult();
    assertThat(result).isNotNull();
    assertThat(result).isEqualTo(1);
  }
}
