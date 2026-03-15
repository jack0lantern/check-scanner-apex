package com.apexfintech.checkdeposit.exception;

public class AccountNotFoundException extends RuntimeException {

  public AccountNotFoundException(String accountId) {
    super("Account not found. Please verify the account ID: " + accountId);
  }
}
