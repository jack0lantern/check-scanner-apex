package com.apexfintech.checkdeposit.deposit;

import com.apexfintech.checkdeposit.domain.TransferState;
import java.util.UUID;

public class TransferNotRetryableException extends RuntimeException {

  public TransferNotRetryableException(UUID transferId, TransferState state) {
    super("Transfer %s in state %s cannot be retried".formatted(transferId, state));
  }
}
