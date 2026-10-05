package com.freelancehub.exception;

/** Not logged in (HTTP 401 / redirect to login). */
public class UnauthorizedException extends RuntimeException {
    public UnauthorizedException(String message) { super(message); }
}
