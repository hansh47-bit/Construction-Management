package com.construction.management.account;

import com.construction.management.project.domain.Approval;
import com.construction.management.project.domain.ApprovalStatus;
import com.construction.management.project.domain.Project;
import com.construction.management.project.repository.ApprovalRepository;
import com.construction.management.project.repository.ProjectRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Upgrade identifiable pending requests without changing completed decisions or amounts. */
@Component
@Order(10)
@ConditionalOnProperty(name = "app.bootstrap-admin", havingValue = "false", matchIfMissing = true)
public class ApprovalRouteInitializer implements CommandLineRunner {
    private final ApprovalRepository approvals;
    private final AppUserRepository users;
    private final ProjectRepository projects;

    public ApprovalRouteInitializer(ApprovalRepository approvals, AppUserRepository users, ProjectRepository projects) {
        this.approvals = approvals;
        this.users = users;
        this.projects = projects;
    }

    @Override
    @Transactional
    public void run(String... args) {
        for (Approval approval : approvals.findByStatusOrderByRequestedAtDesc(ApprovalStatus.REQUESTED)) {
            if (approval.getRequesterPosition() != null || approval.getCreatedUserId() == null) continue;
            AppUser requester = users.findById(approval.getCreatedUserId()).orElse(null);
            if (requester == null || requester.getAccountType() != AccountType.USER) continue;
            AppUser leader = null;
            if (requester.getPosition() == Position.STAFF) {
                Project project = projects.findById(approval.getProjectId()).orElse(null);
                if (project != null && project.getTeamLeaderId() != null) {
                    leader = users.findById(project.getTeamLeaderId()).orElse(null);
                    if (leader != null && (leader.getAccountType() != AccountType.USER
                            || leader.getPosition() != Position.TEAM_LEADER || leader.getStatus() != AccountStatus.ACTIVE)) leader = null;
                }
                // Unidentifiable legacy requests retain their original route rather than becoming stranded.
                if (leader == null) continue;
            }
            approval.configureRoute(requester.getPosition().name(), leader == null ? null : leader.getId(), leader == null ? null : leader.getName());
            approvals.save(approval);
        }
    }
}
