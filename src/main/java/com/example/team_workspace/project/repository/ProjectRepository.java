package com.example.team_workspace.project.repository;

import java.util.List;
import java.util.Optional;

import com.example.team_workspace.project.domain.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    Optional<Project> findByWorkspaceIdAndKey(Long workspaceId, String key);

    boolean existsByWorkspaceIdAndKey(Long workspaceId, String key);

    List<Project> findAllByWorkspaceIdOrderByNameAsc(Long workspaceId);

    @Query("""
            select t.column.project.id as projectId,
                   t.column.name as columnName,
                   count(t.id) as taskCount
            from Task t
            where t.column.project.id in :projectIds
            group by t.column.project.id, t.column.name
            """)
    List<ColumnTaskCount> countTasksByColumnName(
            @Param("projectIds") List<Long> projectIds
    );

    interface ColumnTaskCount {
        Long getProjectId();

        String getColumnName();

        Long getTaskCount();
    }
}
