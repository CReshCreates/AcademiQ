package com.softwareprojectmanagement.Exceptions;

public class SectionNotCurrentlyAvailableException extends RuntimeException {
    public SectionNotCurrentlyAvailableException(String message) {
        super(message);
    }
}
