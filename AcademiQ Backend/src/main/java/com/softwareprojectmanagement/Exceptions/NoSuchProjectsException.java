package com.softwareprojectmanagement.Exceptions;

public class NoSuchProjectsException extends RuntimeException {
    public NoSuchProjectsException(String message) {
        super(message);
    }
}
