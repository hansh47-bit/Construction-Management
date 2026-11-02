package com.construction.management.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.construction.management.auth.SessionUser;
import com.construction.management.notification.*;
import com.construction.management.project.domain.*;
import com.construction.management.project.repository.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:approval-email-test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
    "app.demo-data.enabled=false",
    "app.approval-email.enabled=true",
    "app.approval-email.scheduler-enabled=false",
    "app.approval-email.from=sender@example.test",
    "app.approval-email.base-url=http://192.168.0.5:5173",
    "spring.mail.username=sender@example.test",
    "spring.mail.password=test-only-not-a-real-credential"
})
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ApprovalEmailIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired AppUserRepository users;
    @Autowired ProjectRepository projects;
    @Autowired PurchaseOrderRepository orders;
    @Autowired ApprovalRepository approvals;
    @Autowired ApprovalEmailNotificationRepository notifications;
    @Autowired ApprovalEmailDispatcher dispatcher;
    @Autowired ApprovalEmailQueue queue;
    @Autowired PlatformTransactionManager transactions;
    @Autowired JdbcTemplate jdbc;
    @MockitoBean JavaMailSenderImpl sender;

    AppUser staff, leader, otherLeader, ceo, finance;
    Project project;
    PurchaseOrder order;

    @BeforeAll
    void createAccounts() {
        staff = account(Position.STAFF);
        leader = account(Position.TEAM_LEADER);
        otherLeader = account(Position.TEAM_LEADER);
        ceo = account(Position.CEO);
        finance = account(Position.FINANCE);
    }

    private AppUser account(Position position) {
        AppUser user = new AppUser("email-" + UUID.randomUUID(), "unused-test-hash", position.name(), AccountType.USER, position, "test");
        user.mustChangePassword = false;
        user.email = position.name().toLowerCase() + "+" + UUID.randomUUID() + "@example.test";
        return users.saveAndFlush(user);
    }

    @BeforeEach
    void createProject() {
        SecurityContextHolder.clearContext();
        notifications.deleteAll();
        reset(sender);
        for (AppUser user : List.of(staff, leader, otherLeader, ceo, finance)) {
            user.status = AccountStatus.ACTIVE;
            user.email = user.getId() + "@example.test";
            jdbc.update("update app_users set status='ACTIVE', email=? where id=?", user.email, user.getId());
        }
        project = new Project(UUID.randomUUID().toString(), "P-" + UUID.randomUUID().toString().substring(0, 20), "메일 알림 검증");
        project.updateDetails(null, null, null, null, null, leader.getId(), staff.getId(), ProjectStatus.공사진행, BigDecimal.ZERO, null);
        project = projects.saveAndFlush(project);
        order = orders.saveAndFlush(new PurchaseOrder(UUID.randomUUID().toString(), project.getProjectId(), null, "테스트 업체", BigDecimal.valueOf(1000)));
    }

    @AfterEach
    void clearContext() { SecurityContextHolder.clearContext(); }

    private MockHttpSession session(AppUser user) {
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                new SessionUser(user.getId(), user.getAuthVersion(), user.getName(), user.getPosition().name()), null, List.of()));
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
        return session;
    }

    private Approval request(AppUser requester) throws Exception {
        mvc.perform(post("/api/v1/projects/" + project.getProjectId() + "/progress-claims")
                .session(session(requester)).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"po_id\":\"" + order.getPoId() + "\",\"degree\":1,\"claim_amount\":600}"))
                .andExpect(status().isOk());
        return approvals.findByProjectIdOrderByRequestedAtDesc(project.getProjectId()).get(0);
    }

    private void approve(Approval approval, AppUser approver) throws Exception {
        mvc.perform(post("/api/v1/approvals/" + approval.getApprovalId() + "/approve")
                .session(session(approver)).with(csrf())).andExpect(status().isOk());
    }

    private List<SimpleMailMessage> sent(int count) {
        ArgumentCaptor<SimpleMailMessage> messages = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(sender, times(count)).send(messages.capture());
        return messages.getAllValues();
    }

    @Test
    void emailsFollowAssignedLeaderThenCeoThenRequesterWithoutSendingInsideApprovalRequests() throws Exception {
        Approval approval = request(staff);
        verifyNoInteractions(sender);
        assertThat(notifications.findAll()).singleElement().satisfies(n -> {
            assertThat(n.getRecipientUserId()).isEqualTo(leader.getId());
            assertThat(n.getEvent()).isEqualTo(ApprovalEmailNotification.Event.TEAM_LEADER_REQUEST);
        });
        new TransactionTemplate(transactions).executeWithoutResult(status -> queue.requested(approval));
        assertThat(notifications.count()).isEqualTo(1);
        dispatcher.dispatchDue();
        assertThat(sent(1).get(0).getTo()).containsExactly(leader.email);
        assertThat(sent(1).get(0).getText()).contains(project.getProjectName(), approval.getApprovalId(), "http://192.168.0.5:5173");

        approve(approval, leader);
        assertThat(sent(1)).hasSize(1);
        dispatcher.dispatchDue();
        assertThat(sent(2).get(1).getTo()).containsExactly(ceo.email);
        approve(approval, ceo);
        dispatcher.dispatchDue();
        assertThat(sent(3).get(2).getTo()).containsExactly(staff.email);
        assertThat(sent(3).get(2).getSubject()).contains("최종 승인 완료");
        dispatcher.dispatchDue();
        verify(sender, times(3)).send(any(SimpleMailMessage.class));
    }

    @Test
    void leaderRequestsGoDirectlyToCeoAndRejectionsNotifyOnlyTheRequester() throws Exception {
        Approval approval = request(leader);
        dispatcher.dispatchDue();
        assertThat(sent(1).get(0).getTo()).containsExactly(ceo.email);
        mvc.perform(post("/api/v1/approvals/" + approval.getApprovalId() + "/reject").session(session(ceo)).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"comment\":\"수량 확인 필요\"}"))
                .andExpect(status().isOk());
        dispatcher.dispatchDue();
        assertThat(sent(2).get(1).getTo()).containsExactly(leader.email);
        assertThat(sent(2).get(1).getSubject()).contains("결재 반려");
    }

    @Test
    void failedSmtpRetriesWithoutUndoingApprovalAndEventuallyStops() throws Exception {
        Approval approval = request(staff);
        doThrow(new MailSendException("simulated failure")).when(sender).send(any(SimpleMailMessage.class));
        dispatcher.dispatchDue();
        ApprovalEmailNotification notification = notifications.findAll().get(0);
        assertThat(notification.getAttempts()).isEqualTo(1);
        assertThat(notification.getStatus()).isEqualTo(ApprovalEmailNotification.Status.WAITING);
        assertThat(approvals.findById(approval.getApprovalId()).orElseThrow().getStatus()).isEqualTo(ApprovalStatus.REQUESTED);
        dispatcher.dispatchDue();
        verify(sender, times(1)).send(any(SimpleMailMessage.class));
        reset(sender);
        jdbc.update("update approval_email_notifications set next_attempt_at=DATEADD('SECOND',-1,CURRENT_TIMESTAMP) where id=?", notification.getId());
        dispatcher.dispatchDue();
        assertThat(notifications.findById(notification.getId()).orElseThrow().getStatus()).isEqualTo(ApprovalEmailNotification.Status.SENT);

        createProject();
        Approval another = request(leader);
        doThrow(new MailSendException("simulated failure")).when(sender).send(any(SimpleMailMessage.class));
        for (int attempt = 0; attempt < 5; attempt++) {
            jdbc.update("update approval_email_notifications set next_attempt_at=DATEADD('SECOND',-1,CURRENT_TIMESTAMP)");
            dispatcher.dispatchDue();
        }
        assertThat(notifications.findAll()).singleElement().satisfies(n -> {
            assertThat(n.getApprovalId()).isEqualTo(another.getApprovalId());
            assertThat(n.getStatus()).isEqualTo(ApprovalEmailNotification.Status.FAILED);
            assertThat(n.getAttempts()).isEqualTo(5);
        });
    }

    @Test
    void rolledBackAndSupersededRequestsNeverSendMail() throws Exception {
        TransactionTemplate tx = new TransactionTemplate(transactions);
        tx.executeWithoutResult(status -> {
            Approval approval = new Approval("rollback-" + UUID.randomUUID(), project.getProjectId(), "PROGRESS_CLAIM", "test", "rollback", BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.ONE, "test", null);
            approval.configureRoute("TEAM_LEADER", null, null);
            approvals.save(approval);
            queue.requested(approval);
            status.setRollbackOnly();
        });
        assertThat(notifications.count()).isZero();
        dispatcher.dispatchDue();
        verifyNoInteractions(sender);

        Approval approval = request(staff);
        approve(approval, leader);
        dispatcher.dispatchDue();
        assertThat(sent(1).get(0).getTo()).containsExactly(ceo.email);
        assertThat(notifications.findAll()).anySatisfy(n -> {
            assertThat(n.getEvent()).isEqualTo(ApprovalEmailNotification.Event.TEAM_LEADER_REQUEST);
            assertThat(n.getStatus()).isEqualTo(ApprovalEmailNotification.Status.SKIPPED);
            assertThat(n.getLastErrorCode()).isEqualTo("SUPERSEDED");
        });
    }

    @Test
    void missingAddressesAndDeactivatedRecipientsAreSkipped() throws Exception {
        leader.email = null;
        jdbc.update("update app_users set email=null where id=?", leader.getId());
        request(staff);
        dispatcher.dispatchDue();
        assertThat(notifications.findAll()).singleElement().satisfies(n -> {
            assertThat(n.getStatus()).isEqualTo(ApprovalEmailNotification.Status.SKIPPED);
            assertThat(n.getLastErrorCode()).isEqualTo("MISSING_OR_INVALID_EMAIL");
        });
        verifyNoInteractions(sender);
        createProject();
        request(leader);
        ceo.status = AccountStatus.INACTIVE;
        jdbc.update("update app_users set status='INACTIVE' where id=?", ceo.getId());
        dispatcher.dispatchDue();
        assertThat(notifications.findAll()).singleElement().satisfies(n -> {
            assertThat(n.getStatus()).isEqualTo(ApprovalEmailNotification.Status.SKIPPED);
            assertThat(n.getLastErrorCode()).isEqualTo("RECIPIENT_INACTIVE");
        });
        verifyNoInteractions(sender);
    }
}
