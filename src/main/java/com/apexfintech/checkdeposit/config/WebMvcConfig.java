package com.apexfintech.checkdeposit.config;

import com.apexfintech.checkdeposit.auth.MockAuthInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

  private final MockAuthInterceptor mockAuthInterceptor;

  public WebMvcConfig(MockAuthInterceptor mockAuthInterceptor) {
    this.mockAuthInterceptor = mockAuthInterceptor;
  }

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry
        .addInterceptor(mockAuthInterceptor)
        .addPathPatterns("/**")
        .excludePathPatterns("/actuator/**");
  }
}
