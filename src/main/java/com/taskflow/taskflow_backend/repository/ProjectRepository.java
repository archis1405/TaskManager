package com.taskflow.taskflow_backend.repository;

import com.taskflow.taskflow_backend.entity.Project;
import com.taskflow.taskflow_backend.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectRepository extends JpaRepository<Project,Long> {
    // Projects owned by user
    List<Project> findByOwner(User owner);

    // Projects where user is a member
    @Query("SELECT p FROM Project p JOIN p.members m WHERE m = :user")
    List<Project> findByMember(@Param("user") User user);

    // All projects accessible by user (owned or member)
    @Query("SELECT DISTINCT p FROM Project p WHERE p.owner = :user OR :user MEMBER OF p.members")
    Page<Project> findAllByUser(@Param("user") User user, Pageable pageable);

    @Query("SELECT DISTINCT p FROM Project p WHERE (p.owner = :user OR :user MEMBER OF p.members) AND " +
            "(LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(p.description) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<Project> searchByUser(@Param("user") User user, @Param("query") String query, Pageable pageable);

    // Check membership
    @Query("SELECT COUNT(p) > 0 FROM Project p WHERE p.id = :projectId AND (:user = p.owner OR :user MEMBER OF p.members)")
    boolean isUserMember(@Param("projectId") Long projectId, @Param("user") User user);
}
