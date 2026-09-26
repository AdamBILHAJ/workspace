package com.example.team_workspace.workspace.repository;

import java.util.List;
import java.util.Optional;

import com.example.team_workspace.workspace.domain.Workspace;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WorkspaceRepository extends JpaRepository<Workspace, Long> {

    Optional<Workspace> findBySlug(String slug);

    boolean existsBySlug(String slug);

    @Query("""
            select w, m.role
            from WorkspaceMember m
            join m.workspace w
            where m.user.id = :userId
            order by w.name asc, w.id asc
            """)
    List<Object[]> findAllWithRoleByUserId(@Param("userId") Long userId);
}
