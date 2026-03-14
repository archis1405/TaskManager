package com.taskflow.taskflow_backend.services;


import com.taskflow.taskflow_backend.dto.response.AttachmentResponse;
import com.taskflow.taskflow_backend.entity.Attachment;
import com.taskflow.taskflow_backend.entity.Task;
import com.taskflow.taskflow_backend.entity.User;
import com.taskflow.taskflow_backend.exception.AccessDeniedException;
import com.taskflow.taskflow_backend.exception.ResourceNotFoundException;
import com.taskflow.taskflow_backend.repository.AttachmentRepository;
import com.taskflow.taskflow_backend.repository.TaskRepository;
import com.taskflow.taskflow_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

/**
 * Service for handling file attachments on tasks.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AttachmentService {

    private final AttachmentRepository attachmentRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;

    @Value("${app.upload.dir}")
    private String uploadDir;

    @Transactional
    public AttachmentResponse uploadAttachment(Long taskId, MultipartFile file, Long uploaderId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task", "id", taskId));
        User uploader = userRepository.findById(uploaderId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", uploaderId));

        // Validate file
        if (file.isEmpty()) throw new IllegalArgumentException("File cannot be empty");
        if (file.getSize() > 10 * 1024 * 1024) throw new IllegalArgumentException("File size exceeds 10MB limit");

        String originalName = StringUtils.cleanPath(file.getOriginalFilename());
        String extension = originalName.contains(".")
                ? originalName.substring(originalName.lastIndexOf('.'))
                : "";
        String storedName = UUID.randomUUID() + extension;

        // Save file to disk
        try {
            Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
            Files.createDirectories(uploadPath);
            Path targetLocation = uploadPath.resolve(storedName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file: " + e.getMessage(), e);
        }

        Attachment attachment = Attachment.builder()
                .fileName(storedName)
                .originalName(originalName)
                .contentType(file.getContentType())
                .fileSize(file.getSize())
                .filePath(uploadDir + "/" + storedName)
                .task(task)
                .uploader(uploader)
                .build();

        attachment = attachmentRepository.save(attachment);
        log.info("Attachment uploaded: {} for task {}", originalName, taskId);
        return toResponse(attachment);
    }

    @Transactional(readOnly = true)
    public List<AttachmentResponse> getTaskAttachments(Long taskId) {
        return attachmentRepository.findByTaskId(taskId).stream()
                .map(this::toResponse)
                .toList();
    }

    public Resource loadAttachmentAsResource(Long attachmentId) {
        Attachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Attachment", "id", attachmentId));
        try {
            Path filePath = Paths.get(uploadDir).toAbsolutePath().normalize()
                    .resolve(attachment.getFileName()).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists()) return resource;
            throw new ResourceNotFoundException("File", "name", attachment.getFileName());
        } catch (MalformedURLException e) {
            throw new RuntimeException("Could not load file: " + e.getMessage(), e);
        }
    }

    @Transactional
    public void deleteAttachment(Long attachmentId, Long userId) {
        Attachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Attachment", "id", attachmentId));

        if (!attachment.getUploader().getId().equals(userId)) {
            throw new AccessDeniedException("You can only delete your own attachments");
        }

        // Delete file from disk
        try {
            Path filePath = Paths.get(uploadDir).toAbsolutePath().normalize()
                    .resolve(attachment.getFileName());
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            log.warn("Failed to delete file from disk: {}", e.getMessage());
        }

        attachmentRepository.delete(attachment);
    }

    private AttachmentResponse toResponse(Attachment attachment) {
        return AttachmentResponse.builder()
                .id(attachment.getId())
                .originalName(attachment.getOriginalName())
                .contentType(attachment.getContentType())
                .fileSize(attachment.getFileSize())
                .downloadUrl("/api/attachments/" + attachment.getId() + "/download")
                .uploader(AuthService.toUserSummary(attachment.getUploader()))
                .createdAt(attachment.getCreatedAt())
                .build();
    }
}

