package com.softwareprojectmanagement.Exceptions;

public class ProjectMembersCountExceededException extends RuntimeException {
    public ProjectMembersCountExceededException(String message) {
        super(message);
    }
}
