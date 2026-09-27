package com.example.team_workspace.activity.controller;

import com.example.team_workspace.activity.exception.NotificationNotFoundException;
import com.example.team_workspace.auth.dto.ApiErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ActivityExceptionHandler {

    @ExceptionHandler(NotificationNotFoundException.class)
    ResponseEntity<ApiErrorResponse> handleNotFound(NotificationNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiErrorResponse.of(HttpStatus.NOT_FOUND, exception.getMessage()));
    }
}
