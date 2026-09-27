package com.example.team_workspace.activity.dto;

import java.time.Instant;

import com.example.team_workspace.activity.domain.ActivityLog;

public record ActivityLogResponse(
        Long id,
        Long taskId,
        String action,
        String details,
        Long actorId,
        String actorName,
        Instant createdAt
) {

    public static ActivityLogResponse from(ActivityLog log) {
        return new ActivityLogResponse(
                log.getId(),
                log.getTask().getId(),
                log.getAction().name(),
                log.getDetails(),
                log.getActor().getId(),
                actorName(log),
                log.getCreatedAt()
        );
    }

    private static String actorName(ActivityLog log) {
        String fullName = (
                log.getActor().getFirstName() + " " + log.getActor().getLastName()
        ).trim();
        return fullName.isEmpty() ? log.getActor().getEmail() : fullName;
    }
}
