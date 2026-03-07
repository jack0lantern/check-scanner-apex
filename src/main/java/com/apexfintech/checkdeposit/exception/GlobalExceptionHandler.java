package com.apexfintech.checkdeposit.exception;

import com.apexfintech.checkdeposit.deposit.TransferNotFoundException;
import com.apexfintech.checkdeposit.deposit.TransferNotRetryableException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(AccountNotFoundException.class)
  public ResponseEntity<String> handleAccountNotFound(AccountNotFoundException ex) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
  }

  @ExceptionHandler(TransferNotFoundException.class)
  public ResponseEntity<String> handleTransferNotFound(TransferNotFoundException ex) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
  }

  @ExceptionHandler(TransferNotRetryableException.class)
  public ResponseEntity<String> handleTransferNotRetryable(TransferNotRetryableException ex) {
    return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage());
  }
}
