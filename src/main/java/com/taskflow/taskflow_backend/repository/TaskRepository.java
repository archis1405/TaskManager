package com.taskflow.taskflow_backend.repository;

import com.taskflow.taskflow_backend.entity.Task;
import com.taskflow.taskflow_backend.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task,Long> {

    // Tasks assigned to a user
    Page<Task> findByAssignee(User assignee, Pageable pageable);

    // Tasks created by a user
    Page<Task> findByCreator(User creator, Pageable pageable);

    // Tasks in a project
    Page<Task> findByProjectId(Long projectId, Pageable pageable);

    // Tasks by status in a project
    Page<Task> findByProjectIdAndStatus(Long projectId, Task.TaskStatus status, Pageable pageable);

    // Tasks assigned to user with status filter
    Page<Task> findByAssigneeAndStatus(User assignee, Task.TaskStatus status, Pageable pageable);

    // Full-text search on title and description
    @Query("SELECT t FROM Task t WHERE " +
            "(LOWER(t.title) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(t.description) LIKE LOWER(CONCAT('%', :query, '%'))) AND " +
            "(t.project.id = :projectId OR :projectId IS NULL)")
    Page<Task> searchTasks(@Param("query") String query, @Param("projectId") Long projectId, Pageable pageable);

    // Overdue tasks
    @Query("SELECT t FROM Task t WHERE t.dueDate < :today AND t.status NOT IN ('COMPLETED', 'CANCELLED')")
    List<Task> findOverdueTasks(@Param("today") LocalDate today);

    // Tasks due soon (within days)
    @Query("SELECT t FROM Task t WHERE t.dueDate BETWEEN :today AND :deadline AND t.status NOT IN ('COMPLETED', 'CANCELLED') AND t.assignee = :user")
    List<Task> findTasksDueSoon(@Param("user") User user, @Param("today") LocalDate today, @Param("deadline") LocalDate deadline);

    // Count tasks by status for a project
    @Query("SELECT t.status, COUNT(t) FROM Task t WHERE t.project.id = :projectId GROUP BY t.status")
    List<Object[]> countByStatusForProject(@Param("projectId") Long projectId);

}
