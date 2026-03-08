package com.apexfintech.checkdeposit.operator;

public class InvalidRejectRequestException extends RuntimeException {

  public InvalidRejectRequestException(String message) {
    super(message);
  }
}
