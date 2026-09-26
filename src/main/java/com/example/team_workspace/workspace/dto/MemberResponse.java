package com.example.team_workspace.workspace.dto;

import com.example.team_workspace.auth.dto.UserSummary;
import com.example.team_workspace.workspace.domain.WorkspaceMember;
import com.example.team_workspace.workspace.domain.WorkspaceRole;

public record MemberResponse(
        Long id,
        UserSummary user,
        WorkspaceRole role
) {

    public static MemberResponse from(WorkspaceMember member) {
        return new MemberResponse(
                member.getId(),
                UserSummary.from(member.getUser()),
                member.getRole()
        );
    }
}
