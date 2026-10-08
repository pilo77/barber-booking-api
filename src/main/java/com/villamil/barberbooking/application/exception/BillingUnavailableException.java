package com.villamil.barberbooking.application.exception;

public class BillingUnavailableException extends RuntimeException {
    public BillingUnavailableException() { super("Payments are temporarily unavailable"); }
}
