package com.example.team_workspace.activity.controller;

import java.util.List;

import com.example.team_workspace.activity.dto.ActivityLogResponse;
import com.example.team_workspace.activity.dto.CommentResponse;
import com.example.team_workspace.activity.dto.CreateCommentRequest;
import com.example.team_workspace.activity.service.TaskEngagementService;
import com.example.team_workspace.user.domain.User;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tasks")
public class TaskEngagementRestController {

    private final TaskEngagementService taskEngagementService;

    public TaskEngagementRestController(TaskEngagementService taskEngagementService) {
        this.taskEngagementService = taskEngagementService;
    }

    @PostMapping("/{taskId}/comments")
    public ResponseEntity<CommentResponse> addComment(
            @PathVariable Long taskId,
            @Valid @RequestBody CreateCommentRequest request,
            @AuthenticationPrincipal User currentUser
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(taskEngagementService.addComment(taskId, request, currentUser));
    }

    @GetMapping("/{taskId}/comments")
    public ResponseEntity<List<CommentResponse>> listComments(
            @PathVariable Long taskId,
            @AuthenticationPrincipal User currentUser
    ) {
        return ResponseEntity.ok(taskEngagementService.listComments(taskId, currentUser));
    }

    @GetMapping("/{taskId}/activity")
    public ResponseEntity<List<ActivityLogResponse>> listActivity(
            @PathVariable Long taskId,
            @AuthenticationPrincipal User currentUser
    ) {
        return ResponseEntity.ok(taskEngagementService.listActivity(taskId, currentUser));
    }
}
