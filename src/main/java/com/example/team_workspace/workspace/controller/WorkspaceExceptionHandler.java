package com.example.team_workspace.workspace.controller;

import com.example.team_workspace.auth.dto.ApiErrorResponse;
import com.example.team_workspace.workspace.exception.OrganizationNotFoundException;
import com.example.team_workspace.workspace.exception.WorkspaceAccessDeniedException;
import com.example.team_workspace.workspace.exception.WorkspaceMemberAlreadyExistsException;
import com.example.team_workspace.workspace.exception.WorkspaceNotFoundException;
import com.example.team_workspace.workspace.exception.WorkspaceUserNotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class WorkspaceExceptionHandler {

    @ExceptionHandler({OrganizationNotFoundException.class, WorkspaceNotFoundException.class})
    ResponseEntity<ApiErrorResponse> handleNotFound(RuntimeException exception) {
        return response(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(WorkspaceUserNotFoundException.class)
    ResponseEntity<ApiErrorResponse> handleUserNotFound(WorkspaceUserNotFoundException exception) {
        return response(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(WorkspaceAccessDeniedException.class)
    ResponseEntity<ApiErrorResponse> handleAccessDenied(WorkspaceAccessDeniedException exception) {
        return response(HttpStatus.FORBIDDEN, exception.getMessage());
    }

    @ExceptionHandler(WorkspaceMemberAlreadyExistsException.class)
    ResponseEntity<ApiErrorResponse> handleMemberAlreadyExists(WorkspaceMemberAlreadyExistsException exception) {
        return response(HttpStatus.CONFLICT, exception.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ApiErrorResponse> handleDataIntegrityViolation() {
        return response(HttpStatus.CONFLICT, "Resource already exists");
    }

    private ResponseEntity<ApiErrorResponse> response(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(ApiErrorResponse.of(status, message));
    }
}
