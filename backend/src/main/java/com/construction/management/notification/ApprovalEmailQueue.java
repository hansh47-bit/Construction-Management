package com.construction.management.notification;

import com.construction.management.account.*;
import com.construction.management.project.domain.*;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(propagation = Propagation.MANDATORY)
public class ApprovalEmailQueue {
    private final ApprovalEmailProperties properties;
    private final ApprovalEmailNotificationRepository notifications;
    private final AppUserRepository users;
    private final PermissionService permissions;

    public ApprovalEmailQueue(ApprovalEmailProperties properties, ApprovalEmailNotificationRepository notifications,
            AppUserRepository users, PermissionService permissions) {
        this.properties = properties;
        this.notifications = notifications;
        this.users = users;
        this.permissions = permissions;
    }

    public void requested(Approval approval) {
        if (!properties.enabled() || approval.getStatus() != ApprovalStatus.REQUESTED) return;
        if (approval.getApprovalStage() == ApprovalStage.TEAM_LEADER) {
            add(approval, approval.getTeamLeaderId(), ApprovalEmailNotification.Event.TEAM_LEADER_REQUEST);
        } else if (approval.getApprovalStage() == ApprovalStage.CEO) {
            List<AppUser> recipients = users.findByAccountTypeAndStatus(AccountType.USER, AccountStatus.ACTIVE).stream()
                    .filter(user -> user.getPosition() == Position.CEO)
                    .filter(user -> permissions.effective(user).contains(Permission.APPROVAL_DECIDE)).toList();
            if (recipients.isEmpty()) add(approval, null, ApprovalEmailNotification.Event.CEO_REQUEST);
            recipients.forEach(user -> add(approval, user.getId(), ApprovalEmailNotification.Event.CEO_REQUEST));
        }
    }

    public void decided(Approval approval) {
        if (!properties.enabled()) return;
        if (approval.getStatus() == ApprovalStatus.APPROVED) {
            add(approval, approval.getCreatedUserId(), ApprovalEmailNotification.Event.APPROVED);
        } else if (approval.getStatus() == ApprovalStatus.REJECTED) {
            add(approval, approval.getCreatedUserId(), ApprovalEmailNotification.Event.REJECTED);
        }
    }

    private void add(Approval approval, String recipient, ApprovalEmailNotification.Event event) {
        String key = ApprovalEmailNotification.key(approval.getApprovalId(), recipient, event);
        if (!notifications.existsById(key)) notifications.save(new ApprovalEmailNotification(approval.getApprovalId(), recipient, event));
    }
}
