package com.apexfintech.checkdeposit.funding;

/**
 * Parses US MICR (Magnetic Ink Character Recognition) line data. Uses a heuristic for common US
 * personal/business check format: [routing:9][account:8-14][check:3-6]. TOAD symbol parsing would
 * be more robust but adds complexity.
 */
public final class MicrParser {

  private static final int US_ROUTING_NUMBER_LENGTH = 9;

  /** Minimum digits for full parse: 9 routing + 8 account + 3 check. */
  private static final int MIN_DIGITS_FOR_ACCOUNT_AND_CHECK = 20;

  private static final int CHECK_NUMBER_LENGTH = 3;

  private MicrParser() {}

  /**
   * Extracts the 9-digit US routing number from the MICR line. The routing number is the first 9
   * digits in the MICR string (non-digit characters are ignored when extracting).
   *
   * @param micrData raw MICR line from the check, or null
   * @return the 9-digit routing number as a string, or null if the MICR cannot be parsed (null,
   *     empty, or fewer than 9 digits)
   */
  public static String extractRoutingNumber(String micrData) {
    if (micrData == null || micrData.isBlank()) {
      return null;
    }
    String digits = micrData.replaceAll("\\D", "");
    if (digits.length() < US_ROUTING_NUMBER_LENGTH) {
      return null;
    }
    return digits.substring(0, US_ROUTING_NUMBER_LENGTH);
  }

  /**
   * Extracts the account number from the MICR line. Heuristic: digits between routing (first 9) and
   * check number (last 3). Requires at least 20 digits total.
   *
   * @param micrData raw MICR line from the check, or null
   * @return the account number as a string, or null if unparseable
   */
  public static String extractAccountNumber(String micrData) {
    if (micrData == null || micrData.isBlank()) {
      return null;
    }
    String digits = micrData.replaceAll("\\D", "");
    if (digits.length() < MIN_DIGITS_FOR_ACCOUNT_AND_CHECK) {
      return null;
    }
    return digits.substring(US_ROUTING_NUMBER_LENGTH, digits.length() - CHECK_NUMBER_LENGTH);
  }

  /**
   * Extracts the check number from the MICR line. Heuristic: last 3 digits. Requires at least 20
   * digits total.
   *
   * @param micrData raw MICR line from the check, or null
   * @return the check number as a string, or null if unparseable
   */
  public static String extractCheckNumber(String micrData) {
    if (micrData == null || micrData.isBlank()) {
      return null;
    }
    String digits = micrData.replaceAll("\\D", "");
    if (digits.length() < MIN_DIGITS_FOR_ACCOUNT_AND_CHECK) {
      return null;
    }
    return digits.substring(digits.length() - CHECK_NUMBER_LENGTH);
  }
}
