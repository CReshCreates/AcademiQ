package com.softwareprojectmanagement.Exceptions;

public class NoActiveBatchException extends RuntimeException {
    public NoActiveBatchException(String message) {
        super(message);
    }
}
