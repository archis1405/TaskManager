package com.taskflow.taskflow_backend.services;


import com.taskflow.taskflow_backend.dto.request.TaskRequest;
import com.taskflow.taskflow_backend.dto.response.TaskResponse;
import com.taskflow.taskflow_backend.entity.Project;
import com.taskflow.taskflow_backend.entity.Task;
import com.taskflow.taskflow_backend.entity.User;
import com.taskflow.taskflow_backend.exception.AccessDeniedException;
import com.taskflow.taskflow_backend.exception.ResourceNotFoundException;
import com.taskflow.taskflow_backend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;


@Service
@RequiredArgsConstructor
@Slf4j
public class TaskService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final CommentRepository commentRepository;
    private final AttachmentRepository attachmentRepository;
    private final NotificationService notificationService;

    @Transactional
    public TaskResponse createTask(TaskRequest request, Long creatorId) {
        User creator = getUserOrThrow(creatorId);

        Task.TaskBuilder builder = Task.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .status(request.getStatus() != null ? request.getStatus() : Task.TaskStatus.OPEN)
                .priority(request.getPriority() != null ? request.getPriority() : Task.TaskPriority.MEDIUM)
                .dueDate(request.getDueDate())
                .creator(creator);

        // Attach to project if specified
        if (request.getProjectId() != null) {
            Project project = projectRepository.findById(request.getProjectId())
                    .orElseThrow(() -> new ResourceNotFoundException("Project", "id", request.getProjectId()));
            if (!projectRepository.isUserMember(project.getId(), creator)) {
                throw new AccessDeniedException("You are not a member of this project");
            }
            builder.project(project);
        }

        // Assign to user if specified
        User assignee = null;
        if (request.getAssigneeId() != null) {
            assignee = getUserOrThrow(request.getAssigneeId());
            builder.assignee(assignee);
        }

        Task task = taskRepository.save(builder.build());

        // Notify assignee if different from creator
        if (assignee != null && !assignee.getId().equals(creatorId)) {
            notificationService.notifyTaskAssigned(task, assignee, creator);
        }

        log.info("Task created: id={}, title={}, creator={}", task.getId(), task.getTitle(), creatorId);
        return toResponse(task);
    }

    @Transactional(readOnly = true)
    public TaskResponse getTask(Long taskId) {
        return toResponse(getTaskOrThrow(taskId));
    }

    @Transactional
    public TaskResponse updateTask(Long taskId, TaskRequest request, Long userId) {
        Task task = getTaskOrThrow(taskId);
        validateTaskAccess(task, userId);

        User previousAssignee = task.getAssignee();

        task.setTitle(request.getTitle());
        if (request.getDescription() != null) task.setDescription(request.getDescription());
        if (request.getPriority() != null) task.setPriority(request.getPriority());
        if (request.getDueDate() != null) task.setDueDate(request.getDueDate());

        // Handle status change
        if (request.getStatus() != null && request.getStatus() != task.getStatus()) {
            task.setStatus(request.getStatus());
            if (request.getStatus() == Task.TaskStatus.COMPLETED) {
                task.setCompletedAt(LocalDateTime.now());
            }
        }

        // Re-assign
        if (request.getAssigneeId() != null) {
            User newAssignee = getUserOrThrow(request.getAssigneeId());
            boolean reassigned = previousAssignee == null || !previousAssignee.getId().equals(request.getAssigneeId());
            task.setAssignee(newAssignee);
            if (reassigned && !newAssignee.getId().equals(userId)) {
                User updater = getUserOrThrow(userId);
                notificationService.notifyTaskAssigned(task, newAssignee, updater);
            }
        }

        task = taskRepository.save(task);
        return toResponse(task);
    }

    @Transactional
    public TaskResponse updateTaskStatus(Long taskId, Task.TaskStatus status, Long userId) {
        Task task = getTaskOrThrow(taskId);
        validateTaskAccess(task, userId);

        task.setStatus(status);
        if (status == Task.TaskStatus.COMPLETED) {
            task.setCompletedAt(LocalDateTime.now());
        }

        task = taskRepository.save(task);
        return toResponse(task);
    }

    @Transactional
    public void deleteTask(Long taskId, Long userId) {
        Task task = getTaskOrThrow(taskId);
        if (!task.getCreator().getId().equals(userId)) {
            throw new AccessDeniedException("Only the task creator can delete this task");
        }
        taskRepository.delete(task);
        log.info("Task deleted: id={}, by user={}", taskId, userId);
    }

    @Transactional(readOnly = true)
    public Page<TaskResponse> getTasks(
            Long projectId, Long assigneeId, Long creatorId,
            Task.TaskStatus status, Task.TaskPriority priority,
            LocalDate dueDateFrom, LocalDate dueDateTo,
            String search, Pageable pageable, User currentUser) {

        Specification<Task> spec = TaskSpecification.withFilters(
                projectId, assigneeId, creatorId, status, priority,
                dueDateFrom, dueDateTo, search, currentUser);

        return taskRepository.findAll(spec, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<TaskResponse> getMyTasks(Long userId, Task.TaskStatus status, Pageable pageable) {
        User user = getUserOrThrow(userId);
        if (status != null) {
            return taskRepository.findByAssigneeAndStatus(user, status, pageable).map(this::toResponse);
        }
        return taskRepository.findByAssignee(user, pageable).map(this::toResponse);
    }

    private void validateTaskAccess(Task task, Long userId) {
        boolean isCreator = task.getCreator().getId().equals(userId);
        boolean isAssignee = task.getAssignee() != null && task.getAssignee().getId().equals(userId);
        if (!isCreator && !isAssignee) {
            throw new AccessDeniedException("You don't have permission to modify this task");
        }
    }

    private Task getTaskOrThrow(Long id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task", "id", id));
    }

    private User getUserOrThrow(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
    }

    public TaskResponse toResponse(Task task) {
        return TaskResponse.builder()
                .id(task.getId())
                .title(task.getTitle())
                .description(task.getDescription())
                .status(task.getStatus())
                .priority(task.getPriority())
                .dueDate(task.getDueDate())
                .completedAt(task.getCompletedAt())
                .projectId(task.getProject() != null ? task.getProject().getId() : null)
                .projectName(task.getProject() != null ? task.getProject().getName() : null)
                .creator(AuthService.toUserSummary(task.getCreator()))
                .assignee(AuthService.toUserSummary(task.getAssignee()))
                .commentCount(commentRepository.countByTaskId(task.getId()))
                .attachmentCount(attachmentRepository.countByTaskId(task.getId()))
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                .build();
    }
}

