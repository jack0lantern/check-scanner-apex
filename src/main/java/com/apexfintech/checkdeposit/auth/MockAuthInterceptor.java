package com.apexfintech.checkdeposit.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class MockAuthInterceptor implements HandlerInterceptor {

  public static final String HEADER_USER_ROLE = "X-User-Role";
  public static final String HEADER_ACCOUNT_ID = "X-Account-Id";

  @Override
  public boolean preHandle(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull Object handler)
      throws Exception {
    String userRole = request.getHeader(HEADER_USER_ROLE);
    String accountId = request.getHeader(HEADER_ACCOUNT_ID);

    if (isBlank(userRole) || isBlank(accountId)) {
      response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
      return false;
    }

    AuthContextHolder.set(new AuthContext(userRole.trim(), accountId.trim()));
    return true;
  }

  @Override
  public void afterCompletion(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull Object handler,
      Exception ex) {
    AuthContextHolder.clear();
  }

  private static boolean isBlank(String s) {
    return s == null || s.isBlank();
  }
}
