package com.construction.management.notification;

import com.construction.management.account.*;
import com.construction.management.project.domain.*;
import com.construction.management.project.repository.ApprovalRepository;
import com.construction.management.project.repository.ProjectRepository;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ApprovalEmailDelivery {
    private static final Logger log = LoggerFactory.getLogger(ApprovalEmailDelivery.class);
    private final ApprovalEmailProperties properties;
    private final ApprovalEmailNotificationRepository notifications;
    private final ApprovalRepository approvals;
    private final ProjectRepository projects;
    private final AppUserRepository users;
    private final PermissionService permissions;
    private final JavaMailSender sender;

    public ApprovalEmailDelivery(ApprovalEmailProperties properties, ApprovalEmailNotificationRepository notifications,
            ApprovalRepository approvals, ProjectRepository projects, AppUserRepository users,
            PermissionService permissions, JavaMailSender sender) {
        this.properties = properties;
        this.notifications = notifications;
        this.approvals = approvals;
        this.projects = projects;
        this.users = users;
        this.permissions = permissions;
        this.sender = sender;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void deliver(String id) {
        if (!properties.enabled()) return;
        ApprovalEmailNotification notification = notifications.lockById(id).orElse(null);
        if (notification == null || notification.getStatus() != ApprovalEmailNotification.Status.WAITING
                || notification.getNextAttemptAt().isAfter(LocalDateTime.now())) return;
        Approval approval = approvals.findById(notification.getApprovalId()).orElse(null);
        if (approval == null || !matches(notification, approval)) { notification.skip("SUPERSEDED"); return; }
        AppUser recipient = notification.getRecipientUserId() == null ? null : users.findById(notification.getRecipientUserId()).orElse(null);
        if (recipient == null) { notification.skip("NO_RECIPIENT"); return; }
        if (recipient.getStatus() != AccountStatus.ACTIVE || recipient.getAccountType() != AccountType.USER) {
            notification.skip("RECIPIENT_INACTIVE"); return;
        }
        if (!mayReceive(notification, approval, recipient)) { notification.skip("RECIPIENT_UNAUTHORIZED"); return; }
        if (!ApprovalEmailProperties.validAddress(recipient.getEmail())) { notification.skip("MISSING_OR_INVALID_EMAIL"); return; }
        Project project = projects.findById(approval.getProjectId()).orElse(null);
        if (project == null) { notification.skip("PROJECT_NOT_FOUND"); return; }

        String action = switch (notification.getEvent()) {
            case TEAM_LEADER_REQUEST -> "팀장 결재 요청";
            case CEO_REQUEST -> "대표 결재 요청";
            case APPROVED -> "최종 승인 완료";
            case REJECTED -> "결재 반려";
        };
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(properties.from());
        message.setTo(recipient.getEmail().trim());
        message.setSubject(oneLine("[아이엔씨] " + action + " · " + project.getProjectName()));
        String result = switch (notification.getEvent()) {
            case APPROVED -> "결재가 최종 승인되었습니다.";
            case REJECTED -> "결재가 반려되었습니다. 프로그램에서 사유를 확인해주세요.";
            default -> "확인하실 결재가 도착했습니다. 로그인 후 통합결재함에서 확인해주세요.";
        };
        message.setText(recipient.getName() + "님, " + result
                + "\n\n프로젝트: " + project.getProjectName()
                + "\n결재 제목: " + approval.getTitle()
                + "\n요청자: " + approval.getRequestUser()
                + "\n결재번호: " + approval.getApprovalId()
                + "\n\n프로그램 열기: " + properties.baseUrl()
                + "\n\n이 메일은 아이엔씨 공사관리 프로그램에서 자동 발송했습니다.");
        try {
            sender.send(message);
            notification.sent();
        } catch (MailException exception) {
            notification.failed(properties.maxAttempts());
            // SMTP exception messages may include account information; only record an error code.
            log.warn("Approval email failed: notification={}, attempt={}, status={}", id, notification.getAttempts(), notification.getStatus());
        }
    }

    private boolean matches(ApprovalEmailNotification notification, Approval approval) {
        return switch (notification.getEvent()) {
            case TEAM_LEADER_REQUEST -> approval.getStatus() == ApprovalStatus.REQUESTED && approval.getApprovalStage() == ApprovalStage.TEAM_LEADER;
            case CEO_REQUEST -> approval.getStatus() == ApprovalStatus.REQUESTED && approval.getApprovalStage() == ApprovalStage.CEO;
            case APPROVED -> approval.getStatus() == ApprovalStatus.APPROVED;
            case REJECTED -> approval.getStatus() == ApprovalStatus.REJECTED;
        };
    }

    private boolean mayReceive(ApprovalEmailNotification notification, Approval approval, AppUser recipient) {
        Set<Permission> effective = permissions.effective(recipient);
        if (!effective.contains(Permission.APPROVAL_READ)) return false;
        return switch (notification.getEvent()) {
            case TEAM_LEADER_REQUEST -> recipient.getPosition() == Position.TEAM_LEADER
                    && Objects.equals(approval.getTeamLeaderId(), recipient.getId()) && effective.contains(Permission.APPROVAL_REVIEW);
            case CEO_REQUEST -> recipient.getPosition() == Position.CEO && effective.contains(Permission.APPROVAL_DECIDE);
            case APPROVED, REJECTED -> Objects.equals(approval.getCreatedUserId(), recipient.getId());
        };
    }

    private String oneLine(String text) { return text.replace('\r', ' ').replace('\n', ' '); }
}
