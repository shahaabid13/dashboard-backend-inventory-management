package com.inventory.msp.repository;

import com.inventory.msp.model.Attachment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AttachmentRepository extends JpaRepository<Attachment, Long> {
    List<Attachment> findByEntityTypeAndEntityId(String entityType, Long entityId);
    long countByEntityTypeAndEntityId(String entityType, Long entityId);
}
