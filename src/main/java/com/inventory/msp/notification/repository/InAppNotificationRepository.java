package com.inventory.msp.notification.repository;

import com.inventory.msp.notification.model.InAppNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface InAppNotificationRepository extends JpaRepository<InAppNotification, Long> {

    @Query("""
            SELECT n FROM InAppNotification n
            WHERE n.username = :username
              AND (:unreadOnly = false OR n.read = false)
              AND (:type IS NULL OR n.type = :type)
              AND (:sinceId IS NULL OR n.id > :sinceId)
            ORDER BY n.createdAt DESC, n.id DESC
            """)
    List<InAppNotification> findForUser(
            @Param("username") String username,
            @Param("unreadOnly") boolean unreadOnly,
            @Param("type") String type,
            @Param("sinceId") Long sinceId,
            Pageable pageable);

    long countByUsernameAndReadFalse(String username);

    @Modifying
    @Transactional(transactionManager = "transactionManager")
    @Query("UPDATE InAppNotification n SET n.read = true WHERE n.id = :id AND n.username = :username AND n.read = false")
    int markReadForUser(@Param("id") Long id, @Param("username") String username);

    @Modifying
    @Transactional(transactionManager = "transactionManager")
    @Query("UPDATE InAppNotification n SET n.read = true WHERE n.username = :username AND n.read = false")
    int markAllReadForUser(@Param("username") String username);
}
