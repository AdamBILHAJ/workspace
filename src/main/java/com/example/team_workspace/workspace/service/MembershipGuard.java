package com.example.team_workspace.workspace.service;

import com.example.team_workspace.user.domain.User;
import com.example.team_workspace.user.repository.UserRepository;
import com.example.team_workspace.workspace.domain.Workspace;
import com.example.team_workspace.workspace.domain.WorkspaceMember;
import com.example.team_workspace.workspace.domain.WorkspaceRole;
import com.example.team_workspace.workspace.exception.WorkspaceAccessDeniedException;
import com.example.team_workspace.workspace.exception.WorkspaceNotFoundException;
import com.example.team_workspace.workspace.repository.WorkspaceMemberRepository;
import com.example.team_workspace.workspace.repository.WorkspaceRepository;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Component;

@Component
public class MembershipGuard {

    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final UserRepository userRepository;

    public MembershipGuard(
            WorkspaceRepository workspaceRepository,
            WorkspaceMemberRepository workspaceMemberRepository,
            UserRepository userRepository
    ) {
        this.workspaceRepository = workspaceRepository;
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.userRepository = userRepository;
    }

    public User requireCurrentUser(User actor) {
        if (actor == null || actor.getId() == null) {
            throw new BadCredentialsException("Authentication required");
        }
        return userRepository.findById(actor.getId())
                .orElseThrow(() -> new BadCredentialsException("Authenticated user no longer exists"));
    }

    public Workspace requireMembership(Long workspaceId, User actor) {
        Workspace workspace = requireWorkspace(workspaceId);
        requireCurrentUser(actor);
        workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, actor.getId())
                .orElseThrow(WorkspaceAccessDeniedException::new);
        return workspace;
    }

    public Workspace requireManagement(Long workspaceId, User actor) {
        Workspace workspace = requireWorkspace(workspaceId);
        requireCurrentUser(actor);
        WorkspaceRole role = workspaceMemberRepository
                .findByWorkspaceIdAndUserId(workspaceId, actor.getId())
                .map(WorkspaceMember::getRole)
                .orElseThrow(WorkspaceAccessDeniedException::new);

        if (role != WorkspaceRole.OWNER && role != WorkspaceRole.ADMIN) {
            throw new WorkspaceAccessDeniedException();
        }
        return workspace;
    }

    public boolean isMember(Long workspaceId, Long userId) {
        return workspaceMemberRepository.existsByWorkspaceIdAndUserId(workspaceId, userId);
    }

    private Workspace requireWorkspace(Long workspaceId) {
        if (workspaceId == null) {
            throw new WorkspaceNotFoundException();
        }
        return workspaceRepository.findById(workspaceId)
                .orElseThrow(WorkspaceNotFoundException::new);
    }
}
