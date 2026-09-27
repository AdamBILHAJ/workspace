package com.example.team_workspace.project.controller;

import java.util.List;

import com.example.team_workspace.project.dto.BoardColumnResponse;
import com.example.team_workspace.project.dto.BoardResponse;
import com.example.team_workspace.project.dto.CreateColumnRequest;
import com.example.team_workspace.project.dto.CreateProjectRequest;
import com.example.team_workspace.project.dto.CreateTaskRequest;
import com.example.team_workspace.project.dto.MoveTaskRequest;
import com.example.team_workspace.project.dto.ProjectResponse;
import com.example.team_workspace.project.dto.TaskResponse;
import com.example.team_workspace.project.service.ProjectService;
import com.example.team_workspace.project.service.TaskService;
import com.example.team_workspace.user.domain.User;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class ProjectRestController {

    private final ProjectService projectService;
    private final TaskService taskService;

    public ProjectRestController(ProjectService projectService, TaskService taskService) {
        this.projectService = projectService;
        this.taskService = taskService;
    }

    @PostMapping("/workspaces/{workspaceId}/projects")
    public ResponseEntity<ProjectResponse> createProject(
            @PathVariable Long workspaceId,
            @Valid @RequestBody CreateProjectRequest request,
            @AuthenticationPrincipal User currentUser
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(projectService.createProject(workspaceId, request, currentUser));
    }

    @GetMapping("/workspaces/{workspaceId}/projects")
    public ResponseEntity<List<ProjectResponse>> listProjects(
            @PathVariable Long workspaceId,
            @AuthenticationPrincipal User currentUser
    ) {
        return ResponseEntity.ok(projectService.listProjects(workspaceId, currentUser));
    }

    @GetMapping("/projects/{projectId}/board")
    public ResponseEntity<BoardResponse> getBoard(
            @PathVariable Long projectId,
            @AuthenticationPrincipal User currentUser
    ) {
        return ResponseEntity.ok(projectService.getBoard(projectId, currentUser));
    }

    @PostMapping("/projects/{projectId}/columns")
    public ResponseEntity<BoardColumnResponse> addColumn(
            @PathVariable Long projectId,
            @Valid @RequestBody CreateColumnRequest request,
            @AuthenticationPrincipal User currentUser
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(projectService.addColumn(projectId, request, currentUser));
    }

    @PostMapping("/columns/{columnId}/tasks")
    public ResponseEntity<TaskResponse> createTask(
            @PathVariable Long columnId,
            @Valid @RequestBody CreateTaskRequest request,
            @AuthenticationPrincipal User currentUser
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(taskService.createTask(columnId, request, currentUser));
    }

    @PatchMapping("/tasks/{taskId}/move")
    public ResponseEntity<TaskResponse> moveTask(
            @PathVariable Long taskId,
            @Valid @RequestBody MoveTaskRequest request,
            @AuthenticationPrincipal User currentUser
    ) {
        return ResponseEntity.ok(taskService.moveTask(taskId, request, currentUser));
    }
}
