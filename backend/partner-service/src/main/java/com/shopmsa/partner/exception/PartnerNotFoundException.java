package com.shopmsa.partner.exception;

public class PartnerNotFoundException extends RuntimeException{
    public PartnerNotFoundException(String message) {
        super(message);
    }
    
    public PartnerNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
