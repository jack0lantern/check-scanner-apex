package com.apexfintech.checkdeposit.config;

import com.apexfintech.checkdeposit.auth.MockAuthInterceptor;
import com.apexfintech.checkdeposit.auth.OperatorRoleInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

  private final MockAuthInterceptor mockAuthInterceptor;
  private final OperatorRoleInterceptor operatorRoleInterceptor;

  @Value("${cors.allowed-origins:http://localhost:5173}")
  private String allowedOrigins;

  public WebMvcConfig(
      MockAuthInterceptor mockAuthInterceptor,
      OperatorRoleInterceptor operatorRoleInterceptor) {
    this.mockAuthInterceptor = mockAuthInterceptor;
    this.operatorRoleInterceptor = operatorRoleInterceptor;
  }

  @Override
  public void addCorsMappings(CorsRegistry registry) {
    registry
        .addMapping("/**")
        .allowedOrigins(allowedOrigins.split(","))
        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
        .allowedHeaders("*")
        .allowCredentials(false);
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
