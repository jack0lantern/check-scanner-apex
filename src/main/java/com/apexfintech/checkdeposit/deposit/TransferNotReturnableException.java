package com.apexfintech.checkdeposit.deposit;

import java.util.UUID;

public class TransferNotReturnableException extends RuntimeException {

  public TransferNotReturnableException(UUID transferId, String reason) {
    super("Transfer " + transferId + " cannot be returned: " + reason);
  }
}
