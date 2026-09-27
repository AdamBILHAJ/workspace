package com.example.team_workspace.workspace.repository;

import java.util.List;
import java.util.Optional;

import com.example.team_workspace.workspace.domain.WorkspaceMember;
import com.example.team_workspace.workspace.domain.WorkspaceRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WorkspaceMemberRepository extends JpaRepository<WorkspaceMember, Long> {

    List<WorkspaceMember> findAllByWorkspaceIdOrderByIdAsc(Long workspaceId);

    Optional<WorkspaceMember> findByWorkspaceIdAndUserId(Long workspaceId, Long userId);

    boolean existsByWorkspaceIdAndUserId(Long workspaceId, Long userId);

    @Query("""
            select m.role
            from WorkspaceMember m
            where m.workspace.id = :workspaceId and m.user.id = :userId
            """)
    Optional<WorkspaceRole> findRoleByWorkspaceIdAndUserId(
            @Param("workspaceId") Long workspaceId,
            @Param("userId") Long userId
    );
}
