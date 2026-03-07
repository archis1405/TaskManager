package com.taskflow.taskflow_backend.repository;

import com.taskflow.taskflow_backend.entity.Attachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AttachmentRepository extends JpaRepository<Attachment, Long> {
    List<Attachment> findByTaskId(Long taskId);
    long countByTaskId(Long taskId);
}
