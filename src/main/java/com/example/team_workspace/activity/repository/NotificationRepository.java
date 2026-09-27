package com.example.team_workspace.activity.repository;

import java.util.List;

import com.example.team_workspace.activity.domain.Notification;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findAllByRecipientIdOrderByCreatedAtDescIdDesc(
            Long recipientId,
            Pageable pageable
    );

    long countByRecipientIdAndIsReadFalse(Long recipientId);

    @Modifying
    @Query("update Notification n set n.isRead = true where n.recipient.id = :recipientId and n.isRead = false")
    int markAllAsRead(@Param("recipientId") Long recipientId);
}
