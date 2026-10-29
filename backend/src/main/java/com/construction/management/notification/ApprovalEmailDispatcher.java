package com.construction.management.notification;

import java.time.LocalDateTime;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
public class ApprovalEmailDispatcher {
    private final ApprovalEmailProperties properties;
    private final ApprovalEmailNotificationRepository notifications;
    private final ApprovalEmailDelivery delivery;

    public ApprovalEmailDispatcher(ApprovalEmailProperties properties, ApprovalEmailNotificationRepository notifications, ApprovalEmailDelivery delivery) {
        this.properties = properties;
        this.notifications = notifications;
        this.delivery = delivery;
    }

    public void dispatchDue() {
        if (!properties.enabled()) return;
        for (String id : notifications.findDueIds(ApprovalEmailNotification.Status.WAITING, LocalDateTime.now(), PageRequest.of(0, 20))) delivery.deliver(id);
    }
}
