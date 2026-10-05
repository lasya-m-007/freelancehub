package com.freelancehub.exception;

/** Base type for any violated business rule (maps to HTTP 400). */
public class BusinessRuleException extends RuntimeException {
    public BusinessRuleException(String message) { super(message); }
}
