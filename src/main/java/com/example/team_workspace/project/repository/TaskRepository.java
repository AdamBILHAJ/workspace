package com.example.team_workspace.project.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.example.team_workspace.project.domain.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TaskRepository extends JpaRepository<Task, Long> {

    long countByColumnId(Long columnId);

    List<Task> findAllByColumnIdOrderByOrderIndexAscIdAsc(Long columnId);

    List<Task> findAllByColumnIdInOrderByOrderIndexAscIdAsc(Collection<Long> columnIds);

    @Query("""
            select t
            from Task t
            left join fetch t.assignee
            join fetch t.reporter
            where t.column.project.id = :projectId
            order by t.column.orderIndex asc, t.orderIndex asc, t.id asc
            """)
    List<Task> findAllByProjectIdWithUsers(@Param("projectId") Long projectId);

    @Query("""
            select t
            from Task t
            left join fetch t.assignee
            join fetch t.reporter
            where t.id = :taskId
            """)
    Optional<Task> findByIdWithUsers(@Param("taskId") Long taskId);
}
