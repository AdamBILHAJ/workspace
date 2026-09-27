package com.example.team_workspace.activity.repository;

import java.util.List;

import com.example.team_workspace.activity.domain.TaskComment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskCommentRepository extends JpaRepository<TaskComment, Long> {

    List<TaskComment> findAllByTaskIdOrderByCreatedAtAscIdAsc(Long taskId);

    long countByTaskId(Long taskId);
}
