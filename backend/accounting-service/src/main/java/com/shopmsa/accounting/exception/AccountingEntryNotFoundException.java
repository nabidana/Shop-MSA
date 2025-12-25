package com.shopmsa.accounting.exception;

public class AccountingEntryNotFoundException extends RuntimeException{
    public AccountingEntryNotFoundException(String message) {
        super(message);
    }
    
    public AccountingEntryNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
