package com.construction.management.notification;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "approval_email_notifications", indexes = @Index(name = "ix_approval_email_due", columnList = "status,next_attempt_at"))
@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class ApprovalEmailNotification {
    public enum Event { TEAM_LEADER_REQUEST, CEO_REQUEST, APPROVED, REJECTED }
    public enum Status { WAITING, SENT, SKIPPED, FAILED }

    @Id @Column(length = 200) private String id;
    @Column(nullable = false, length = 64) private String approvalId;
    private String recipientUserId;
    @Enumerated(EnumType.STRING) @Column(nullable = false, columnDefinition = "varchar(32)") private Event event;
    @Enumerated(EnumType.STRING) @Column(nullable = false, columnDefinition = "varchar(16)") private Status status = Status.WAITING;
    @Column(nullable = false) private int attempts;
    @Column(nullable = false) private LocalDateTime createdAt = LocalDateTime.now();
    @Column(nullable = false) private LocalDateTime nextAttemptAt = LocalDateTime.now();
    private LocalDateTime sentAt;
    @Column(length = 64) private String lastErrorCode;

    public ApprovalEmailNotification(String approvalId, String recipientUserId, Event event) {
        this.id = key(approvalId, recipientUserId, event);
        this.approvalId = approvalId;
        this.recipientUserId = recipientUserId;
        this.event = event;
    }

    public static String key(String approvalId, String recipientUserId, Event event) {
        return approvalId + ":" + event + ":" + (recipientUserId == null ? "NO_RECIPIENT" : recipientUserId);
    }

    void skip(String reason) { status = Status.SKIPPED; lastErrorCode = reason; }
    void sent() { attempts++; status = Status.SENT; sentAt = LocalDateTime.now(); lastErrorCode = null; }
    void failed(int maxAttempts) {
        attempts++;
        lastErrorCode = "MAIL_SEND_FAILED";
        status = attempts >= maxAttempts ? Status.FAILED : Status.WAITING;
        nextAttemptAt = LocalDateTime.now().plusMinutes(1L << (attempts - 1));
    }
}
