package com.taskflow.taskflow_backend.exception;


public class ConflictException extends RuntimeException {
    public ConflictException(String message) { super(message); }
}
