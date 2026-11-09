package com.softwareprojectmanagement.Exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;


import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<String> handleEmailAlreadyExists(EmailAlreadyExistsException e){
        return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
    }

    @ExceptionHandler(AuthenticationFailedException.class)
    public ResponseEntity<String> handleAuthenticationFailed(AuthenticationFailedException e){
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
    }

    @ExceptionHandler(SubjectNotFoundException.class)
    public ResponseEntity<String> handleSubjectNotFound(SubjectNotFoundException e){
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
    }

    @ExceptionHandler(NoActiveBatchException.class)
    public ResponseEntity<String> handleActiveBatchNotFound(NoActiveBatchException e){
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
    }

    @ExceptionHandler(SectionNotCurrentlyAvailableException.class)
    public ResponseEntity<String> handleSectionNotFound(SectionNotCurrentlyAvailableException e){
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
    }

    @ExceptionHandler(NoSubmissionException.class)
    public ResponseEntity<String> handleNoSubmission(NoSubmissionException e){
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
    }

    @ExceptionHandler(NoSuchProjectsException.class)
    public ResponseEntity<String> handleProjectsNotFound(NoSuchProjectsException e){
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
    }

    @ExceptionHandler(ProjectMembersCountExceededException.class)
    public ResponseEntity<String> handleProjectMembersCount(ProjectMembersCountExceededException e){
        return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body(e.getMessage());
    }

    @ExceptionHandler(NoInvitationFoundException.class)
    public ResponseEntity<String> handleNoInvitationsFound(NoInvitationFoundException e){
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
    }


    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<String> handleIllegalStateException(
            IllegalStateException e) {

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(e.getMessage());
    }


    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<String> handleMethodArgumentNotValid(MethodArgumentNotValidException e){
        String message = e.getBindingResult()
                .getFieldErrors()
                .stream().map(error->
                        error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(message);
    }


    // ==========================================
    // 403 - ACCESS DENIED
    // ==========================================

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<String> handleAccessDenied(
            AccessDeniedException e
    ) {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body("You do not have permission to perform this action.");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgument(
            IllegalArgumentException e
    ) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(e.getMessage());
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<String> handleUsernameNotFound(
            UsernameNotFoundException e
    ) {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body("Authentication could not be verified.");
    }

}
