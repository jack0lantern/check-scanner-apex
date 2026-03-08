package com.apexfintech.checkdeposit.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class OperatorRoleInterceptor implements HandlerInterceptor {

  private static final String OPERATOR_ROLE = "OPERATOR";

  @Override
  public boolean preHandle(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull Object handler)
      throws Exception {
    AuthContext ctx = AuthContextHolder.get();
    if (ctx == null || !OPERATOR_ROLE.equals(ctx.userRole())) {
      response.setStatus(HttpServletResponse.SC_FORBIDDEN);
      return false;
    }
    return true;
  }
}
