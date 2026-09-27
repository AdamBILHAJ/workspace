package com.example.team_workspace.workspace.controller;

import java.util.List;

import com.example.team_workspace.user.domain.User;
import com.example.team_workspace.workspace.dto.AddMemberRequest;
import com.example.team_workspace.workspace.dto.CreateOrganizationRequest;
import com.example.team_workspace.workspace.dto.CreateWorkspaceRequest;
import com.example.team_workspace.workspace.dto.MemberResponse;
import com.example.team_workspace.workspace.dto.OrganizationResponse;
import com.example.team_workspace.workspace.dto.WorkspaceResponse;
import com.example.team_workspace.workspace.service.WorkspaceService;
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
@RequestMapping("/api/v1")
public class WorkspaceRestController {

    private final WorkspaceService workspaceService;

    public WorkspaceRestController(WorkspaceService workspaceService) {
        this.workspaceService = workspaceService;
    }

    @PostMapping("/organizations")
    public ResponseEntity<OrganizationResponse> createOrganization(
            @Valid @RequestBody CreateOrganizationRequest request,
            @AuthenticationPrincipal User currentUser
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(workspaceService.createOrganization(request.name(), currentUser));
    }

    @GetMapping("/organizations")
    public ResponseEntity<List<OrganizationResponse>> listOrganizations(
            @AuthenticationPrincipal User currentUser
    ) {
        return ResponseEntity.ok(workspaceService.listOrganizations(currentUser));
    }

    @PostMapping("/organizations/{organizationId}/workspaces")
    public ResponseEntity<WorkspaceResponse> createWorkspace(
            @PathVariable Long organizationId,
            @Valid @RequestBody CreateWorkspaceRequest request,
            @AuthenticationPrincipal User currentUser
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(workspaceService.createWorkspace(organizationId, request, currentUser));
    }

    @GetMapping("/workspaces")
    public ResponseEntity<List<WorkspaceResponse>> listWorkspaces(
            @AuthenticationPrincipal User currentUser
    ) {
        return ResponseEntity.ok(workspaceService.listWorkspaces(currentUser));
    }

    @PostMapping("/workspaces/{workspaceId}/members")
    public ResponseEntity<MemberResponse> addMember(
            @PathVariable Long workspaceId,
            @Valid @RequestBody AddMemberRequest request,
            @AuthenticationPrincipal User currentUser
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(workspaceService.addMember(workspaceId, request, currentUser));
    }

    @GetMapping("/workspaces/{workspaceId}/members")
    public ResponseEntity<List<MemberResponse>> listMembers(
            @PathVariable Long workspaceId,
            @AuthenticationPrincipal User currentUser
    ) {
        return ResponseEntity.ok(workspaceService.listMembers(workspaceId, currentUser));
    }
}
