package com.smarthr.exception;

/** A rule of the HR domain was violated (insufficient balance, duplicate email, ...). Message is user-facing. */
public class BusinessException extends RuntimeException {
    public BusinessException(String message) { super(message); }
}
