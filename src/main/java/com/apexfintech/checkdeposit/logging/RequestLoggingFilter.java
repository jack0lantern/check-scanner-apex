package com.apexfintech.checkdeposit.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Filter that tags every request with traceId, depositSource, and transferId (when available) for
 * structured logging. All downstream logs inherit these MDC values.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestLoggingFilter extends OncePerRequestFilter {

  private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

  private static final String MDC_TRACE_ID = "traceId";
  private static final String MDC_DEPOSIT_SOURCE = "depositSource";
  private static final String MDC_TRANSFER_ID = "transferId";

  private static final String HEADER_USER_ROLE = "X-User-Role";

  /**
   * UUID pattern for extracting transferId from paths like /deposits/{uuid} or
   * /operator/queue/{uuid}/approve
   */
  private static final Pattern UUID_IN_PATH =
      Pattern.compile(
          "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");

  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain filterChain)
      throws ServletException, IOException {
    String traceId = UUID.randomUUID().toString();
    String depositSource = resolveDepositSource(request);
    String transferId = extractTransferIdFromPath(request.getRequestURI());

    try {
      MDC.put(MDC_TRACE_ID, traceId);
      MDC.put(MDC_DEPOSIT_SOURCE, depositSource);
      if (transferId != null) {
        MDC.put(MDC_TRANSFER_ID, transferId);
      }

      log.info("Request received path={}", redactPath(request.getRequestURI()));

      filterChain.doFilter(request, response);
    } finally {
      MDC.clear();
    }
  }

  private String resolveDepositSource(HttpServletRequest request) {
    String userRole = request.getHeader(HEADER_USER_ROLE);
    if (userRole == null || userRole.isBlank()) {
      return "UNKNOWN";
    }
    return switch (userRole.trim().toUpperCase()) {
      case "INVESTOR" -> "MOBILE";
      case "OPERATOR" -> "OPERATOR";
      default -> "UNKNOWN";
    };
  }

  private String extractTransferIdFromPath(String path) {
    Matcher matcher = UUID_IN_PATH.matcher(path);
    return matcher.find() ? matcher.group() : null;
  }

  /**
   * Redacts account IDs and other PII-adjacent path segments. Keeps UUIDs (transfer IDs) as-is.
   * Redacts /accounts/{accountId} segments to avoid logging raw account identifiers.
   */
  private String redactPath(String path) {
    if (path == null || path.isEmpty()) {
      return path;
    }
    // Redact accountId in /accounts/{accountId}/balance and /accounts/{accountId}/ledger
    return path.replaceAll("/accounts/([^/]+)(/balance|/ledger)", "/accounts/[REDACTED]$2");
  }
}
