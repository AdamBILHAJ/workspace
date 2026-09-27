package com.example.team_workspace.project.controller;

import com.example.team_workspace.auth.dto.ApiErrorResponse;
import com.example.team_workspace.project.exception.AssigneeNotFoundException;
import com.example.team_workspace.project.exception.AssigneeNotInWorkspaceException;
import com.example.team_workspace.project.exception.InvalidTaskMoveException;
import com.example.team_workspace.project.exception.KanbanColumnNotFoundException;
import com.example.team_workspace.project.exception.ProjectKeyAlreadyExistsException;
import com.example.team_workspace.project.exception.ProjectNotFoundException;
import com.example.team_workspace.project.exception.TaskNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ProjectExceptionHandler {

    @ExceptionHandler({
            ProjectNotFoundException.class,
            KanbanColumnNotFoundException.class,
            TaskNotFoundException.class,
            AssigneeNotFoundException.class
    })
    ResponseEntity<ApiErrorResponse> handleNotFound(RuntimeException exception) {
        return response(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler({AssigneeNotInWorkspaceException.class, InvalidTaskMoveException.class})
    ResponseEntity<ApiErrorResponse> handleBadRequest(RuntimeException exception) {
        return response(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    @ExceptionHandler(ProjectKeyAlreadyExistsException.class)
    ResponseEntity<ApiErrorResponse> handleDuplicateKey(ProjectKeyAlreadyExistsException exception) {
        return response(HttpStatus.CONFLICT, exception.getMessage());
    }

    private ResponseEntity<ApiErrorResponse> response(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(ApiErrorResponse.of(status, message));
    }
}
