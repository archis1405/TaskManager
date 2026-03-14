package com.taskflow.taskflow_backend.controller;

import com.taskflow.taskflow_backend.dto.request.TaskRequest;
import com.taskflow.taskflow_backend.dto.response.ApiResponse;
import com.taskflow.taskflow_backend.dto.response.TaskResponse;
import com.taskflow.taskflow_backend.entity.Task;
import com.taskflow.taskflow_backend.entity.User;
import com.taskflow.taskflow_backend.services.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * RESTful endpoints for task CRUD, filtering, search, and assignment.
 */
@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
@Tag(name = "Tasks", description = "Task management operations")
public class TaskController {

    private final TaskService taskService;

    @PostMapping
    @Operation(summary = "Create a new task")
    public ResponseEntity<ApiResponse<TaskResponse>> createTask(
            @Valid @RequestBody TaskRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        TaskResponse task = taskService.createTask(request, userDetails.getId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Task created", task));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get task by ID")
    public ResponseEntity<ApiResponse<TaskResponse>> getTask(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(taskService.getTask(id)));
    }

    @GetMapping
    @Operation(summary = "Get tasks with optional filtering, sorting, and search")
    public ResponseEntity<ApiResponse<Page<TaskResponse>>> getTasks(
            @Parameter(description = "Filter by project ID") @RequestParam(required = false) Long projectId,
            @Parameter(description = "Filter by assignee ID") @RequestParam(required = false) Long assigneeId,
            @Parameter(description = "Filter by creator ID") @RequestParam(required = false) Long creatorId,
            @Parameter(description = "Filter by status") @RequestParam(required = false) Task.TaskStatus status,
            @Parameter(description = "Filter by priority") @RequestParam(required = false) Task.TaskPriority priority,
            @Parameter(description = "Filter by due date from") @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueDateFrom,
            @Parameter(description = "Filter by due date to") @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueDateTo,
            @Parameter(description = "Search in title and description") @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        User currentUser = userDetails.user();
        Page<TaskResponse> tasks = taskService.getTasks(
                projectId, assigneeId, creatorId, status, priority,
                dueDateFrom, dueDateTo, search, pageable, currentUser);
        return ResponseEntity.ok(ApiResponse.success(tasks));
    }

    @GetMapping("/my")
    @Operation(summary = "Get tasks assigned to me")
    public ResponseEntity<ApiResponse<Page<TaskResponse>>> getMyTasks(
            @RequestParam(required = false) Task.TaskStatus status,
            @PageableDefault(size = 20, sort = "dueDate") Pageable pageable,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Page<TaskResponse> tasks = taskService.getMyTasks(userDetails.getId(), status, pageable);
        return ResponseEntity.ok(ApiResponse.success(tasks));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a task")
    public ResponseEntity<ApiResponse<TaskResponse>> updateTask(
            @PathVariable Long id,
            @Valid @RequestBody TaskRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        TaskResponse task = taskService.updateTask(id, request, userDetails.getId());
        return ResponseEntity.ok(ApiResponse.success("Task updated", task));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update task status only")
    public ResponseEntity<ApiResponse<TaskResponse>> updateStatus(
            @PathVariable Long id,
            @RequestParam Task.TaskStatus status,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        TaskResponse task = taskService.updateTaskStatus(id, status, userDetails.getId());
        return ResponseEntity.ok(ApiResponse.success("Task status updated", task));
    }

    @PatchMapping("/{id}/complete")
    @Operation(summary = "Mark a task as completed")
    public ResponseEntity<ApiResponse<TaskResponse>> completeTask(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        TaskResponse task = taskService.updateTaskStatus(id, Task.TaskStatus.COMPLETED, userDetails.getId());
        return ResponseEntity.ok(ApiResponse.success("Task marked as completed", task));
    }

    @PatchMapping("/{id}/assign")
    @Operation(summary = "Assign task to a team member")
    public ResponseEntity<ApiResponse<TaskResponse>> assignTask(
            @PathVariable Long id,
            @RequestParam Long assigneeId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        TaskRequest request = new TaskRequest();
        request.setAssigneeId(assigneeId);
        // Fetch existing and merge
        TaskResponse existing = taskService.getTask(id);
        request.setTitle(existing.getTitle());
        request.setDescription(existing.getDescription());
        request.setStatus(existing.getStatus());
        request.setPriority(existing.getPriority());
        request.setDueDate(existing.getDueDate());
        request.setAssigneeId(assigneeId);
        TaskResponse task = taskService.updateTask(id, request, userDetails.getId());
        return ResponseEntity.ok(ApiResponse.success("Task assigned", task));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a task")
    public ResponseEntity<ApiResponse<Void>> deleteTask(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        taskService.deleteTask(id, userDetails.getId());
        return ResponseEntity.ok(ApiResponse.success("Task deleted", null));
    }
}
