package com.example.team_workspace.project.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.example.team_workspace.project.domain.KanbanColumn;
import com.example.team_workspace.project.domain.Project;
import com.example.team_workspace.project.domain.Task;
import com.example.team_workspace.project.dto.BoardColumnResponse;
import com.example.team_workspace.project.dto.BoardResponse;
import com.example.team_workspace.project.dto.CreateColumnRequest;
import com.example.team_workspace.project.dto.CreateProjectRequest;
import com.example.team_workspace.project.dto.ProjectResponse;
import com.example.team_workspace.project.dto.ProjectSummary;
import com.example.team_workspace.project.dto.TaskResponse;
import com.example.team_workspace.project.exception.ProjectKeyAlreadyExistsException;
import com.example.team_workspace.project.exception.ProjectNotFoundException;
import com.example.team_workspace.project.repository.KanbanColumnRepository;
import com.example.team_workspace.project.repository.ProjectRepository;
import com.example.team_workspace.project.repository.TaskRepository;
import com.example.team_workspace.user.domain.User;
import com.example.team_workspace.workspace.domain.Workspace;
import com.example.team_workspace.workspace.service.MembershipGuard;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProjectService {

    private static final List<String> DEFAULT_COLUMN_NAMES = List.of("To Do", "In Progress", "Done");
    private static final String DONE_COLUMN_NAME = "done";
    private static final int MAX_KEY_LENGTH = 10;
    private static final String FALLBACK_KEY = "PRJ";

    private final ProjectRepository projectRepository;
    private final KanbanColumnRepository kanbanColumnRepository;
    private final TaskRepository taskRepository;
    private final MembershipGuard membershipGuard;

    public ProjectService(
            ProjectRepository projectRepository,
            KanbanColumnRepository kanbanColumnRepository,
            TaskRepository taskRepository,
            MembershipGuard membershipGuard
    ) {
        this.projectRepository = projectRepository;
        this.kanbanColumnRepository = kanbanColumnRepository;
        this.taskRepository = taskRepository;
        this.membershipGuard = membershipGuard;
    }

    @Transactional
    public ProjectResponse createProject(
            Long workspaceId,
            CreateProjectRequest request,
            User actor
    ) {
        Workspace workspace = membershipGuard.requireManagement(workspaceId, actor);
        String key = resolveKey(workspaceId, request.key(), request.name());
        Project project = projectRepository.save(
                new Project(request.name(), key, request.description(), workspace)
        );

        for (int index = 0; index < DEFAULT_COLUMN_NAMES.size(); index++) {
            kanbanColumnRepository.save(
                    new KanbanColumn(DEFAULT_COLUMN_NAMES.get(index), index, project)
            );
        }

        return ProjectResponse.from(project, 0, 0);
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> listProjects(Long workspaceId, User actor) {
        membershipGuard.requireMembership(workspaceId, actor);
        List<Project> projects = projectRepository.findAllByWorkspaceIdOrderByNameAsc(workspaceId);

        if (projects.isEmpty()) {
            return List.of();
        }

        Map<Long, int[]> metrics = loadProgressMetrics(
                projects.stream().map(Project::getId).toList()
        );

        return projects.stream()
                .map(project -> {
                    int[] counts = metrics.getOrDefault(project.getId(), new int[]{0, 0});
                    return ProjectResponse.from(project, counts[0], counts[1]);
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public BoardResponse getBoard(Long projectId, User actor) {
        Project project = requireReadableProject(projectId, actor);
        List<KanbanColumn> columns = kanbanColumnRepository
                .findAllByProjectIdOrderByOrderIndexAscIdAsc(projectId);

        Map<Long, List<TaskResponse>> tasksByColumn = new HashMap<>();
        for (Task task : taskRepository.findAllByProjectIdWithUsers(projectId)) {
            tasksByColumn
                    .computeIfAbsent(task.getColumn().getId(), columnId -> new ArrayList<>())
                    .add(TaskResponse.from(task));
        }

        List<BoardColumnResponse> boardColumns = columns.stream()
                .map(column -> BoardColumnResponse.from(
                        column,
                        tasksByColumn.getOrDefault(column.getId(), List.of())
                ))
                .toList();

        return new BoardResponse(ProjectSummary.from(project), boardColumns);
    }

    @Transactional
    public BoardColumnResponse addColumn(
            Long projectId,
            CreateColumnRequest request,
            User actor
    ) {
        Project project = requireManagedProject(projectId, actor);
        int nextOrderIndex = kanbanColumnRepository.findMaxOrderIndex(projectId) + 1;
        KanbanColumn column = kanbanColumnRepository.save(
                new KanbanColumn(request.name(), nextOrderIndex, project)
        );
        return BoardColumnResponse.from(column, List.of());
    }

    private Project requireReadableProject(Long projectId, User actor) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(ProjectNotFoundException::new);
        membershipGuard.requireMembership(project.getWorkspace().getId(), actor);
        return project;
    }

    private Project requireManagedProject(Long projectId, User actor) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(ProjectNotFoundException::new);
        membershipGuard.requireManagement(project.getWorkspace().getId(), actor);
        return project;
    }

    private Map<Long, int[]> loadProgressMetrics(List<Long> projectIds) {
        Map<Long, int[]> metrics = new HashMap<>();

        for (ProjectRepository.ColumnTaskCount row : projectRepository.countTasksByColumnName(projectIds)) {
            int[] counts = metrics.computeIfAbsent(row.getProjectId(), id -> new int[2]);
            int taskCount = row.getTaskCount().intValue();
            counts[0] += taskCount;
            if (row.getColumnName() != null
                    && DONE_COLUMN_NAME.equals(row.getColumnName().trim().toLowerCase(Locale.ROOT))) {
                counts[1] += taskCount;
            }
        }

        return metrics;
    }

    private String resolveKey(Long workspaceId, String requestedKey, String projectName) {
        if (requestedKey != null && !requestedKey.isBlank()) {
            String explicitKey = requestedKey.trim().toUpperCase(Locale.ROOT);
            if (projectRepository.existsByWorkspaceIdAndKey(workspaceId, explicitKey)) {
                throw new ProjectKeyAlreadyExistsException();
            }
            return explicitKey;
        }

        String base = deriveKey(projectName);
        String candidate = base;
        int suffix = 2;

        while (projectRepository.existsByWorkspaceIdAndKey(workspaceId, candidate)) {
            String tail = String.valueOf(suffix++);
            int headLength = Math.max(1, MAX_KEY_LENGTH - tail.length());
            String head = base.length() > headLength ? base.substring(0, headLength) : base;
            candidate = head + tail;
        }

        return candidate;
    }

    private String deriveKey(String projectName) {
        String cleaned = projectName.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9 ]", " ").trim();
        String[] words = cleaned.isEmpty() ? new String[0] : cleaned.split("\\s+");
        String firstWord = words.length > 0 ? words[0] : "";

        String candidate = firstWord.length() >= 3 ? firstWord : initials(words);

        if (candidate.length() < 2) {
            candidate = FALLBACK_KEY;
        }

        return candidate.length() > MAX_KEY_LENGTH
                ? candidate.substring(0, MAX_KEY_LENGTH)
                : candidate;
    }

    private String initials(String[] words) {
        StringBuilder builder = new StringBuilder();

        for (String word : words) {
            if (word.isEmpty()) {
                continue;
            }
            builder.append(word.charAt(0));
            if (builder.length() >= 4) {
                break;
            }
        }

        return builder.toString();
    }
}
