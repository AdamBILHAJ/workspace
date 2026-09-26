package com.example.team_workspace.project.service;

import java.util.ArrayList;
import java.util.List;

import com.example.team_workspace.project.domain.KanbanColumn;
import com.example.team_workspace.project.domain.Task;
import com.example.team_workspace.project.dto.CreateTaskRequest;
import com.example.team_workspace.project.dto.MoveTaskRequest;
import com.example.team_workspace.project.dto.TaskResponse;
import com.example.team_workspace.project.exception.AssigneeNotFoundException;
import com.example.team_workspace.project.exception.AssigneeNotInWorkspaceException;
import com.example.team_workspace.project.exception.InvalidTaskMoveException;
import com.example.team_workspace.project.exception.KanbanColumnNotFoundException;
import com.example.team_workspace.project.exception.TaskNotFoundException;
import com.example.team_workspace.project.repository.KanbanColumnRepository;
import com.example.team_workspace.project.repository.TaskRepository;
import com.example.team_workspace.user.domain.User;
import com.example.team_workspace.user.repository.UserRepository;
import com.example.team_workspace.workspace.service.MembershipGuard;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final KanbanColumnRepository kanbanColumnRepository;
    private final UserRepository userRepository;
    private final MembershipGuard membershipGuard;

    public TaskService(
            TaskRepository taskRepository,
            KanbanColumnRepository kanbanColumnRepository,
            UserRepository userRepository,
            MembershipGuard membershipGuard
    ) {
        this.taskRepository = taskRepository;
        this.kanbanColumnRepository = kanbanColumnRepository;
        this.userRepository = userRepository;
        this.membershipGuard = membershipGuard;
    }

    @Transactional
    public TaskResponse createTask(Long columnId, CreateTaskRequest request, User actor) {
        KanbanColumn column = kanbanColumnRepository.findById(columnId)
                .orElseThrow(KanbanColumnNotFoundException::new);
        Long workspaceId = column.getProject().getWorkspace().getId();

        membershipGuard.requireMembership(workspaceId, actor);
        User reporter = membershipGuard.requireCurrentUser(actor);
        User assignee = resolveAssignee(request.assigneeId(), workspaceId);

        int orderIndex = Math.toIntExact(taskRepository.countByColumnId(columnId));
        Task task = taskRepository.save(new Task(
                request.title(),
                request.description(),
                request.priority(),
                orderIndex,
                column,
                assignee,
                reporter
        ));

        return TaskResponse.from(task);
    }

    @Transactional
    public TaskResponse moveTask(Long taskId, MoveTaskRequest request, User actor) {
        Task task = taskRepository.findByIdWithUsers(taskId)
                .orElseThrow(TaskNotFoundException::new);

        KanbanColumn sourceColumn = task.getColumn();
        Long projectId = sourceColumn.getProject().getId();
        membershipGuard.requireMembership(
                sourceColumn.getProject().getWorkspace().getId(),
                actor
        );

        KanbanColumn targetColumn = kanbanColumnRepository.findById(request.columnId())
                .orElseThrow(KanbanColumnNotFoundException::new);

        if (!targetColumn.getProject().getId().equals(projectId)) {
            throw new InvalidTaskMoveException("A task cannot be moved to a column in another project");
        }

        List<Task> targetTasks = new ArrayList<>(
                taskRepository.findAllByColumnIdInOrderByOrderIndexAscIdAsc(List.of(targetColumn.getId()))
        );
        targetTasks.removeIf(candidate -> candidate.getId().equals(taskId));

        int position = Math.min(Math.max(request.orderIndex(), 0), targetTasks.size());
        task.setColumn(targetColumn);
        targetTasks.add(position, task);
        taskRepository.saveAll(applyContiguousOrder(targetTasks));

        if (!sourceColumn.getId().equals(targetColumn.getId())) {
            List<Task> remaining = taskRepository
                    .findAllByColumnIdInOrderByOrderIndexAscIdAsc(List.of(sourceColumn.getId()));
            taskRepository.saveAll(applyContiguousOrder(remaining));
        }

        return TaskResponse.from(task);
    }

    private User resolveAssignee(Long assigneeId, Long workspaceId) {
        if (assigneeId == null) {
            return null;
        }

        User assignee = userRepository.findById(assigneeId)
                .orElseThrow(AssigneeNotFoundException::new);

        if (!membershipGuard.isMember(workspaceId, assignee.getId())) {
            throw new AssigneeNotInWorkspaceException();
        }

        return assignee;
    }

    private List<Task> applyContiguousOrder(List<Task> tasks) {
        for (int index = 0; index < tasks.size(); index++) {
            Task task = tasks.get(index);
            if (!task.getOrderIndex().equals(index)) {
                task.setOrderIndex(index);
            }
        }
        return tasks;
    }
}
