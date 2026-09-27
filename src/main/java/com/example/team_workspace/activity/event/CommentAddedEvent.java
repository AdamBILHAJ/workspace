package com.example.team_workspace.activity.event;

import com.example.team_workspace.activity.domain.TaskComment;
import com.example.team_workspace.project.domain.Task;
import com.example.team_workspace.user.domain.User;

public record CommentAddedEvent(
        Long commentId,
        Long taskId,
        String taskTitle,
        String projectKey,
        String workspaceSlug,
        Long authorId,
        String authorName,
        Long assigneeId
) {

    public static CommentAddedEvent from(TaskComment comment, Task task, User author) {
        return new CommentAddedEvent(
                comment.getId(),
                task.getId(),
                task.getTitle(),
                task.getColumn().getProject().getKey(),
                task.getColumn().getProject().getWorkspace().getSlug(),
                author.getId(),
                authorName(author),
                task.getAssignee() == null ? null : task.getAssignee().getId()
        );
    }

    private static String authorName(User author) {
        String fullName = (author.getFirstName() + " " + author.getLastName()).trim();
        return fullName.isEmpty() ? author.getEmail() : fullName;
    }
}
