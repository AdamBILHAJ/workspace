package com.example.team_workspace.activity.repository;

import java.util.List;

import com.example.team_workspace.activity.domain.ActivityAction;
import com.example.team_workspace.activity.domain.ActivityLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {

    List<ActivityLog> findAllByTaskIdOrderByCreatedAtDescIdDesc(Long taskId);

    boolean existsByTaskIdAndAction(Long taskId, ActivityAction action);
}
