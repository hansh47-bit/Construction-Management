package com.construction.management.notification;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@EnableScheduling
@ConditionalOnProperty(prefix = "app.approval-email", name = {"enabled", "scheduler-enabled"}, havingValue = "true")
public class ApprovalEmailScheduler {
    private final ApprovalEmailDispatcher dispatcher;
    public ApprovalEmailScheduler(ApprovalEmailDispatcher dispatcher) { this.dispatcher = dispatcher; }

    @Scheduled(fixedDelayString = "${app.approval-email.poll-delay-ms:15000}", initialDelayString = "${app.approval-email.poll-delay-ms:15000}")
    public void sendPending() { dispatcher.dispatchDue(); }
}
