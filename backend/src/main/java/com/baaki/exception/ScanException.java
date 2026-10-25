package com.baaki.exception;

/** A scan problem with a code the frontend translates, and the HTTP status to return. */
public class ScanException extends RuntimeException {
    private final String code;
    private final int status;

    public ScanException(String code, int status, String message) { super(message); this.code = code; this.status = status; }

    public String code() { return code; }
    public int status() { return status; }
}
