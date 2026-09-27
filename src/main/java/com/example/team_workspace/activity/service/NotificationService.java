package com.example.team_workspace.activity.service;

import com.example.team_workspace.activity.domain.Notification;
import com.example.team_workspace.activity.dto.NotificationResponse;
import com.example.team_workspace.activity.dto.NotificationsResponse;
import com.example.team_workspace.activity.exception.NotificationNotFoundException;
import com.example.team_workspace.activity.repository.NotificationRepository;
import com.example.team_workspace.user.domain.User;
import com.example.team_workspace.workspace.service.MembershipGuard;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {

    private static final int RECENT_LIMIT = 50;

    private final NotificationRepository notificationRepository;
    private final MembershipGuard membershipGuard;

    public NotificationService(
            NotificationRepository notificationRepository,
            MembershipGuard membershipGuard
    ) {
        this.notificationRepository = notificationRepository;
        this.membershipGuard = membershipGuard;
    }

    @Transactional(readOnly = true)
    public NotificationsResponse listNotifications(User actor) {
        User currentUser = membershipGuard.requireCurrentUser(actor);
        Long recipientId = currentUser.getId();

        var recent = notificationRepository.findAllByRecipientIdOrderByCreatedAtDescIdDesc(
                recipientId,
                PageRequest.of(0, RECENT_LIMIT)
        );

        return new NotificationsResponse(
                recent.stream().map(NotificationResponse::from).toList(),
                notificationRepository.countByRecipientIdAndIsReadFalse(recipientId)
        );
    }

    @Transactional
    public NotificationResponse markAsRead(Long notificationId, User actor) {
        User currentUser = membershipGuard.requireCurrentUser(actor);
        Notification notification = notificationRepository.findById(notificationId)
                .filter(candidate -> candidate.getRecipient().getId().equals(currentUser.getId()))
                .orElseThrow(NotificationNotFoundException::new);

        notification.markAsRead();
        return NotificationResponse.from(notificationRepository.save(notification));
    }

    @Transactional
    public int markAllAsRead(User actor) {
        User currentUser = membershipGuard.requireCurrentUser(actor);
        return notificationRepository.markAllAsRead(currentUser.getId());
    }
}
