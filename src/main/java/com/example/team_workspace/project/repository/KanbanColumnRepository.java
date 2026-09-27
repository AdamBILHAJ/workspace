package com.example.team_workspace.project.repository;

import java.util.List;
import java.util.Optional;

import com.example.team_workspace.project.domain.KanbanColumn;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface KanbanColumnRepository extends JpaRepository<KanbanColumn, Long> {

    List<KanbanColumn> findAllByProjectIdOrderByOrderIndexAscIdAsc(Long projectId);

    Optional<KanbanColumn> findByIdAndProjectId(Long id, Long projectId);

    @Query("""
            select coalesce(max(c.orderIndex), -1)
            from KanbanColumn c
            where c.project.id = :projectId
            """)
    int findMaxOrderIndex(@Param("projectId") Long projectId);
}
