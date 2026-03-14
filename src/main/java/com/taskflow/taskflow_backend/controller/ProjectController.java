package com.taskflow.taskflow_backend.controller;

import com.taskflow.taskflow_backend.dto.request.ProjectRequest;
import com.taskflow.taskflow_backend.dto.response.ApiResponse;
import com.taskflow.taskflow_backend.dto.response.ProjectResponse;
import com.taskflow.taskflow_backend.services.ProjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
@Tag(name = "Projects", description = "Team and project management")
public class ProjectController {

    private final ProjectService projectService;

    @PostMapping
    @Operation(summary = "Create a new project/team")
    public ResponseEntity<ApiResponse<ProjectResponse>> createProject(
            @Valid @RequestBody ProjectRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        ProjectResponse project = projectService.createProject(request, userDetails.getId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Project created", project));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get project by ID")
    public ResponseEntity<ApiResponse<ProjectResponse>> getProject(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        ProjectResponse project = projectService.getProject(id, userDetails.getId());
        return ResponseEntity.ok(ApiResponse.success(project));
    }

    @GetMapping
    @Operation(summary = "Get all projects for the current user")
    public ResponseEntity<ApiResponse<Page<ProjectResponse>>> getMyProjects(
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20) Pageable pageable,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Page<ProjectResponse> projects = projectService.getMyProjects(userDetails.getId(), search, pageable);
        return ResponseEntity.ok(ApiResponse.success(projects));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update project details (owner only)")
    public ResponseEntity<ApiResponse<ProjectResponse>> updateProject(
            @PathVariable Long id,
            @Valid @RequestBody ProjectRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        ProjectResponse project = projectService.updateProject(id, request, userDetails.getId());
        return ResponseEntity.ok(ApiResponse.success("Project updated", project));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a project (owner only)")
    public ResponseEntity<ApiResponse<Void>> deleteProject(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        projectService.deleteProject(id, userDetails.getId());
        return ResponseEntity.ok(ApiResponse.success("Project deleted", null));
    }

    @PostMapping("/{id}/members/{userId}")
    @Operation(summary = "Add a member to project (owner only)")
    public ResponseEntity<ApiResponse<ProjectResponse>> addMember(
            @PathVariable Long id,
            @PathVariable Long userId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        ProjectResponse project = projectService.addMember(id, userId, userDetails.getId());
        return ResponseEntity.ok(ApiResponse.success("Member added", project));
    }

    @DeleteMapping("/{id}/members/{userId}")
    @Operation(summary = "Remove a member from project (owner only)")
    public ResponseEntity<ApiResponse<ProjectResponse>> removeMember(
            @PathVariable Long id,
            @PathVariable Long userId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        ProjectResponse project = projectService.removeMember(id, userId, userDetails.getId());
        return ResponseEntity.ok(ApiResponse.success("Member removed", project));
    }

    @PostMapping("/{id}/leave")
    @Operation(summary = "Leave a project")
    public ResponseEntity<ApiResponse<ProjectResponse>> leaveProject(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        ProjectResponse project = projectService.leaveProject(id, userDetails.getId());
        return ResponseEntity.ok(ApiResponse.success("Left project", project));
    }
}

