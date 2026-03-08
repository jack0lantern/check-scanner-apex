package com.apexfintech.checkdeposit.config;

import com.apexfintech.checkdeposit.auth.MockAuthInterceptor;
import com.apexfintech.checkdeposit.auth.OperatorRoleInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

  private final MockAuthInterceptor mockAuthInterceptor;
  private final OperatorRoleInterceptor operatorRoleInterceptor;

  public WebMvcConfig(
      MockAuthInterceptor mockAuthInterceptor,
      OperatorRoleInterceptor operatorRoleInterceptor) {
    this.mockAuthInterceptor = mockAuthInterceptor;
    this.operatorRoleInterceptor = operatorRoleInterceptor;
  }

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry
        .addInterceptor(mockAuthInterceptor)
        .addPathPatterns("/**")
        .excludePathPatterns("/actuator/**");
    registry
        .addInterceptor(operatorRoleInterceptor)
        .addPathPatterns("/operator/**");
  }
}
