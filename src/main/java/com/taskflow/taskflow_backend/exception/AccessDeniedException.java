package com.taskflow.taskflow_backend.exception;


public class AccessDeniedException extends RuntimeException {
    public AccessDeniedException(String message) { super(message); }
}

