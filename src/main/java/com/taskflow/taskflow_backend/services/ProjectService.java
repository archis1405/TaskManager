package com.taskflow.taskflow_backend.services;


import com.taskflow.taskflow_backend.dto.request.ProjectRequest;
import com.taskflow.taskflow_backend.dto.response.ProjectResponse;
import com.taskflow.taskflow_backend.dto.response.UserSummaryResponse;
import com.taskflow.taskflow_backend.entity.Project;
import com.taskflow.taskflow_backend.entity.User;
import com.taskflow.taskflow_backend.exception.AccessDeniedException;
import com.taskflow.taskflow_backend.exception.ConflictException;
import com.taskflow.taskflow_backend.exception.ResourceNotFoundException;
import com.taskflow.taskflow_backend.repository.ProjectRepository;
import com.taskflow.taskflow_backend.repository.TaskRepository;
import com.taskflow.taskflow_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service for managing projects/teams and memberships.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final TaskRepository taskRepository;

    @Transactional
    public ProjectResponse createProject(ProjectRequest request, Long ownerId) {
        User owner = getUserOrThrow(ownerId);

        Project project = Project.builder()
                .name(request.getName())
                .description(request.getDescription())
                .owner(owner)
                .build();

        // Owner is automatically a member
        project.getMembers().add(owner);
        project = projectRepository.save(project);

        log.info("Project created: id={}, name={}, owner={}", project.getId(), project.getName(), ownerId);
        return toResponse(project);
    }

    @Transactional(readOnly = true)
    public ProjectResponse getProject(Long projectId, Long userId) {
        Project project = getProjectOrThrow(projectId);
        User user = getUserOrThrow(userId);
        if (!projectRepository.isUserMember(projectId, user)) {
            throw new AccessDeniedException("You are not a member of this project");
        }
        return toResponse(project);
    }

    @Transactional(readOnly = true)
    public Page<ProjectResponse> getMyProjects(Long userId, String search, Pageable pageable) {
        User user = getUserOrThrow(userId);
        Page<Project> projects = (search != null && !search.isBlank())
                ? projectRepository.searchByUser(user, search, pageable)
                : projectRepository.findAllByUser(user, pageable);
        return projects.map(this::toResponse);
    }

    @Transactional
    public ProjectResponse updateProject(Long projectId, ProjectRequest request, Long userId) {
        Project project = getProjectOrThrow(projectId);
        requireOwner(project, userId);

        project.setName(request.getName());
        if (request.getDescription() != null) project.setDescription(request.getDescription());
        project = projectRepository.save(project);
        return toResponse(project);
    }

    @Transactional
    public void deleteProject(Long projectId, Long userId) {
        Project project = getProjectOrThrow(projectId);
        requireOwner(project, userId);
        projectRepository.delete(project);
        log.info("Project deleted: id={}, by user={}", projectId, userId);
    }

    @Transactional
    public ProjectResponse addMember(Long projectId, Long memberId, Long requesterId) {
        Project project = getProjectOrThrow(projectId);
        requireOwner(project, requesterId);

        User newMember = getUserOrThrow(memberId);
        if (project.getMembers().contains(newMember)) {
            throw new ConflictException("User is already a member of this project");
        }
        project.getMembers().add(newMember);
        project = projectRepository.save(project);
        return toResponse(project);
    }

    @Transactional
    public ProjectResponse removeMember(Long projectId, Long memberId, Long requesterId) {
        Project project = getProjectOrThrow(projectId);
        requireOwner(project, requesterId);

        if (project.getOwner().getId().equals(memberId)) {
            throw new IllegalArgumentException("Cannot remove the project owner");
        }

        User member = getUserOrThrow(memberId);
        project.getMembers().remove(member);
        project = projectRepository.save(project);
        return toResponse(project);
    }

    @Transactional
    public ProjectResponse leaveProject(Long projectId, Long userId) {
        Project project = getProjectOrThrow(projectId);
        if (project.getOwner().getId().equals(userId)) {
            throw new IllegalArgumentException("Owner cannot leave their own project. Transfer ownership first.");
        }
        User user = getUserOrThrow(userId);
        project.getMembers().remove(user);
        project = projectRepository.save(project);
        return toResponse(project);
    }

    private void requireOwner(Project project, Long userId) {
        if (!project.getOwner().getId().equals(userId)) {
            throw new AccessDeniedException("Only the project owner can perform this action");
        }
    }

    private Project getProjectOrThrow(Long id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", id));
    }

    private User getUserOrThrow(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
    }

    public ProjectResponse toResponse(Project project) {
        List<UserSummaryResponse> members = project.getMembers().stream()
                .map(AuthService::toUserSummary)
                .toList();

        return ProjectResponse.builder()
                .id(project.getId())
                .name(project.getName())
                .description(project.getDescription())
                .status(project.getStatus())
                .owner(AuthService.toUserSummary(project.getOwner()))
                .members(members)
                .memberCount(members.size())
                .taskCount(project.getTasks().size())
                .createdAt(project.getCreatedAt())
                .updatedAt(project.getUpdatedAt())
                .build();
    }
}
