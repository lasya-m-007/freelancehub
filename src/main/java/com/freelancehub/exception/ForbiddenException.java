package com.freelancehub.exception;

/** Logged in but not allowed (HTTP 403). */
public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) { super(message); }
}
