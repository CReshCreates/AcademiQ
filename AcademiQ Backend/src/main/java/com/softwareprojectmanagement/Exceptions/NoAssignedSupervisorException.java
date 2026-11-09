package com.softwareprojectmanagement.Exceptions;

public class NoAssignedSupervisorException extends RuntimeException {
    public NoAssignedSupervisorException(String message) {
        super(message);
    }
}
