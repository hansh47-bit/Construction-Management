package com.construction.management.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.construction.management.auth.SessionUser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.construction.management.project.domain.*;
import com.construction.management.project.repository.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:approval-workflow-test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
    "app.demo-data.enabled=false",
    "app.contract-file.upload-dir=build/workflow-test-uploads/contracts",
    "app.execution-budget-attachment.upload-dir=build/workflow-test-uploads/budget"
})
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ApprovalWorkflowIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired AppUserRepository users;
    @Autowired PasswordEncoder encoder;
    @Autowired ProjectRepository projects;
    @Autowired PurchaseOrderRepository orders;
    @Autowired ProgressClaimRepository claims;
    @Autowired ActualPaymentRepository payments;
    @Autowired ApprovalRepository approvals;
    @Autowired PositionPermissionRepository permissions;
    @Autowired PermissionService permissionService;
    @Autowired ProjectExecutionBudgetItemRepository budgetItems;
    @Autowired ApprovalRouteInitializer routeInitializer;
    @Autowired ObjectMapper json;

    AppUser staff, otherStaff, leader, otherLeader, ceo, finance;
    Project project;
    PurchaseOrder order;

    @BeforeAll
    void createRoles() {
        String hash = encoder.encode("Workflow-test-password123!");
        staff = user(Position.STAFF, hash);
        otherStaff = user(Position.STAFF, hash);
        leader = user(Position.TEAM_LEADER, hash);
        otherLeader = user(Position.TEAM_LEADER, hash);
        ceo = user(Position.CEO, hash);
        finance = user(Position.FINANCE, hash);
    }

    private AppUser user(Position position, String hash) {
        AppUser user = new AppUser("workflow-" + UUID.randomUUID(), hash, position.name(), AccountType.USER, position, "test");
        user.mustChangePassword = false;
        return users.saveAndFlush(user);
    }

    @BeforeEach
    void createProject() {
        SecurityContextHolder.clearContext();
        project = new Project(UUID.randomUUID().toString(), "P-" + UUID.randomUUID().toString().substring(0, 20), "결재선 검증");
        project.updateDetails(null, null, null, null, null, leader.getId(), staff.getId(), ProjectStatus.공사진행, BigDecimal.ZERO, null);
        project = projects.saveAndFlush(project);
        order = orders.saveAndFlush(new PurchaseOrder(UUID.randomUUID().toString(), project.getProjectId(), null, "지급 업체", BigDecimal.valueOf(1000)));
    }

    @AfterEach
    void clearContext() { SecurityContextHolder.clearContext(); }

    private MockHttpSession session(AppUser user) {
        var authentication = UsernamePasswordAuthenticationToken.authenticated(
                new SessionUser(user.getId(), user.getAuthVersion(), user.getName(), user.getPosition().name()), null, List.of());
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
        return session;
    }

    private Approval requestClaim(AppUser requester) throws Exception {
        mvc.perform(post("/api/v1/projects/" + project.getProjectId() + "/progress-claims")
                .session(session(requester)).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"po_id\":\"" + order.getPoId() + "\",\"degree\":1,\"claim_amount\":600}"))
                .andExpect(status().isOk());
        return approvals.findByProjectIdOrderByRequestedAtDesc(project.getProjectId()).stream()
                .filter(a -> "PROGRESS_CLAIM".equals(a.getApprovalType())).findFirst().orElseThrow();
    }

    private String path(Approval approval) { return "/api/v1/approvals/" + approval.getApprovalId(); }

    @Test
    void staffMustPassAssignedLeaderBeforeCeoAndThenDocumentIsShared() throws Exception {
        Approval approval = requestClaim(staff);
        assertThat(approval.getApprovalStage()).isEqualTo(ApprovalStage.TEAM_LEADER);
        assertThat(approval.getTeamLeaderId()).isEqualTo(leader.getId());
        mvc.perform(post(path(approval) + "/approve").session(session(ceo)).with(csrf())).andExpect(status().isForbidden());
        mvc.perform(post(path(approval) + "/approve").session(session(otherLeader)).with(csrf())).andExpect(status().isNotFound());
        mvc.perform(get(path(approval)).session(session(otherStaff))).andExpect(status().isNotFound());
        mvc.perform(post(path(approval) + "/approve").session(session(leader)).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"comment\":\"검토 완료\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("status").value("REQUESTED"))
                .andExpect(jsonPath("approval_stage").value("CEO"))
                .andExpect(jsonPath("team_leader_approved_by").value(leader.getName()));
        assertThat(claims.findById(approval.getTargetId()).orElseThrow().getApprovalStatus()).isEqualTo(ApprovalStatus.REQUESTED);
        assertThat(payments.findByClaimId(approval.getTargetId())).isEmpty();
        mvc.perform(get(path(approval)).session(session(otherStaff))).andExpect(status().isNotFound());
        mvc.perform(post(path(approval) + "/approve").session(session(leader)).with(csrf())).andExpect(status().isForbidden());
        mvc.perform(post(path(approval) + "/approve").session(session(ceo)).with(csrf()))
                .andExpect(status().isOk()).andExpect(jsonPath("status").value("APPROVED"))
                .andExpect(jsonPath("approval_stage").value("COMPLETE"))
                .andExpect(jsonPath("approver").value(ceo.getName()));
        assertThat(claims.findById(approval.getTargetId()).orElseThrow().getApprovalStatus()).isEqualTo(ApprovalStatus.APPROVED);
        mvc.perform(get(path(approval)).session(session(otherStaff))).andExpect(status().isOk());
        mvc.perform(get("/api/v1/approvals").param("status", "APPROVED").param("projectId", project.getProjectId())
                .session(session(otherStaff))).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].approval_id").value(approval.getApprovalId()));
        mvc.perform(post(path(approval) + "/approve").session(session(ceo)).with(csrf())).andExpect(status().isConflict());
    }

    @Test
    void leaderRequestsGoDirectlyToCeoAndStayHiddenFromStaffUntilFinalApproval() throws Exception {
        Approval approval = requestClaim(leader);
        assertThat(approval.getApprovalStage()).isEqualTo(ApprovalStage.CEO);
        assertThat(approval.getRequesterPosition()).isEqualTo("TEAM_LEADER");
        mvc.perform(get(path(approval)).session(session(staff))).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/approvals").param("projectId", project.getProjectId()).session(session(staff)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(post(path(approval) + "/approve").session(session(leader)).with(csrf())).andExpect(status().isForbidden());
        mvc.perform(post(path(approval) + "/approve").session(session(ceo)).with(csrf())).andExpect(status().isOk());
        mvc.perform(get(path(approval)).session(session(staff))).andExpect(status().isOk());
    }

    @Test
    void financeCanCirculateEveryStageButCannotApproveOrReject() throws Exception {
        Approval approval = requestClaim(staff);
        for (String decision : List.of("approve", "reject")) {
            mvc.perform(post(path(approval) + "/" + decision).session(session(finance)).with(csrf())
                    .contentType(MediaType.APPLICATION_JSON).content("{\"comment\":\"회람 의견\"}"))
                    .andExpect(status().isForbidden());
        }
        mvc.perform(get(path(approval)).session(session(finance))).andExpect(status().isOk())
                .andExpect(jsonPath("approval_stage").value("TEAM_LEADER"));
        mvc.perform(post(path(approval) + "/approve").session(session(leader)).with(csrf())).andExpect(status().isOk());
        mvc.perform(get(path(approval)).session(session(finance))).andExpect(status().isOk())
                .andExpect(jsonPath("approval_stage").value("CEO"));
        mvc.perform(post(path(approval) + "/reject").session(session(ceo)).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"comment\":\"대표 보완 요청\"}"))
                .andExpect(status().isOk());
        mvc.perform(get(path(approval)).session(session(finance))).andExpect(status().isOk())
                .andExpect(jsonPath("status").value("REJECTED"));
    }

    @Test
    void leaderRejectionStopsTheWorkflowAndRejectsTheUnderlyingClaim() throws Exception {
        Approval approval = requestClaim(staff);
        mvc.perform(post(path(approval) + "/reject").session(session(leader)).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"comment\":\"수량 확인 필요\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("status").value("REJECTED"))
                .andExpect(jsonPath("approval_stage").value("TEAM_LEADER"));
        assertThat(claims.findById(approval.getTargetId()).orElseThrow().getApprovalStatus()).isEqualTo(ApprovalStatus.REJECTED);
        mvc.perform(post(path(approval) + "/approve").session(session(ceo)).with(csrf())).andExpect(status().isConflict());
    }

    @Test
    void onlyFinanceMayRequestVendorPaymentsAndCeoApprovalDoesNotRegisterPayment() throws Exception {
        ProgressClaim claim = claims.saveAndFlush(new ProgressClaim(UUID.randomUUID().toString(), order.getPoId(), 1, BigDecimal.valueOf(600), ApprovalStatus.APPROVED));
        String requestPath = "/api/v1/projects/" + project.getProjectId() + "/payment-requests";
        String payload = "{\"po_ids\":[\"" + order.getPoId() + "\"]}";
        for (AppUser user : List.of(staff, leader, ceo)) {
            mvc.perform(post(requestPath).session(session(user)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(payload))
                    .andExpect(status().isForbidden());
        }
        mvc.perform(post(requestPath).session(session(finance)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isCreated());
        Approval approval = approvals.findByProjectIdOrderByRequestedAtDesc(project.getProjectId()).get(0);
        assertThat(approval.getApprovalType()).isEqualTo("PAYMENT");
        assertThat(approval.getRequesterPosition()).isEqualTo("FINANCE");
        assertThat(approval.getApprovalStage()).isEqualTo(ApprovalStage.CEO);
        mvc.perform(post(path(approval) + "/approve").session(session(ceo)).with(csrf())).andExpect(status().isOk());
        assertThat(payments.findByClaimId(claim.getClaimId())).isEmpty();
        mvc.perform(get(path(approval)).session(session(staff))).andExpect(status().isOk());
    }

    @Test
    void staffCannotRequestWithoutAssignedActiveLeaderAndNoPartialDataIsSaved() throws Exception {
        project.updateDetails(null, null, null, null, null, null, staff.getId(), ProjectStatus.공사진행, BigDecimal.ZERO, null);
        projects.saveAndFlush(project);
        mvc.perform(post("/api/v1/projects/" + project.getProjectId() + "/progress-claims")
                .session(session(staff)).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"po_id\":\"" + order.getPoId() + "\",\"degree\":1,\"claim_amount\":600}"))
                .andExpect(status().isConflict());
        assertThat(claims.findByPoId(order.getPoId())).isEmpty();
        assertThat(approvals.findByProjectIdOrderByRequestedAtDesc(project.getProjectId())).isEmpty();
    }

    @Test
    void staffPaymentRequestPermissionCannotBeEnabledByLegacyPermissionRows() throws Exception {
        PositionPermission permission = permissions.findById("STAFF:PAYMENT_REQUEST").orElseThrow();
        permission.update(true, "legacy-setting");
        permissions.saveAndFlush(permission);
        try {
            mvc.perform(get("/api/v1/auth/me").session(session(staff))).andExpect(status().isOk())
                    .andExpect(jsonPath("permissions", org.hamcrest.Matchers.not(org.hamcrest.Matchers.hasItem("PAYMENT_REQUEST"))));
            permissionService.initializeDefaults();
            assertThat(permissions.findById("STAFF:PAYMENT_REQUEST").orElseThrow().isAllowed()).isFalse();
            assertThat(permissions.findById("FINANCE:PAYMENT_REQUEST").orElseThrow().isAllowed()).isTrue();
        } finally {
            permission = permissions.findById("STAFF:PAYMENT_REQUEST").orElseThrow();
            permission.update(false, "test-restore");
            permissions.saveAndFlush(permission);
        }
    }

    @Test
    void budgetApprovalAndAttachmentsAreNotSharedUntilCeoCompletesTheWorkflow() throws Exception {
        String base = "/api/v1/projects/" + project.getProjectId();
        mvc.perform(put(base + "/execution-budget-details").session(session(staff)).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"details\":[{\"budget_item_id\":\"EX-001\",\"category_name\":\"목공\",\"item_name\":\"목공\",\"cost_type\":\"외주비\",\"quantity\":1,\"unit_price\":600}]}"))
                .andExpect(status().isOk());
        var request = mvc.perform(multipart(base + "/execution-budget-details/request-approval")
                .file(new MockMultipartFile("request", "request.json", "application/json", "{}".getBytes(java.nio.charset.StandardCharsets.UTF_8)))
                .file(new MockMultipartFile("files", "budget.txt", "text/plain", "budget evidence".getBytes(java.nio.charset.StandardCharsets.UTF_8)))
                .session(session(staff)).with(csrf())).andExpect(status().isOk()).andReturn();
        var response = json.readTree(request.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
        Approval approval = approvals.findById(response.get("approval_id").asText()).orElseThrow();
        String attachment = response.get("attachments").get(0).get("download_url").asText();
        mvc.perform(get(attachment).session(session(otherStaff))).andExpect(status().isNotFound());
        mvc.perform(get(attachment).session(session(finance))).andExpect(status().isOk());
        mvc.perform(post(path(approval) + "/approve").session(session(leader)).with(csrf())).andExpect(status().isOk());
        assertThat(budgetItems.findByProjectIdOrderByItemIdAsc(project.getProjectId()))
                .allSatisfy(item -> assertThat(item.getApprovalStatus()).isEqualTo("REQUESTED"));
        mvc.perform(get(attachment).session(session(otherStaff))).andExpect(status().isNotFound());
        mvc.perform(post(path(approval) + "/approve").session(session(ceo)).with(csrf())).andExpect(status().isOk());
        assertThat(budgetItems.findByProjectIdOrderByItemIdAsc(project.getProjectId()))
                .allSatisfy(item -> assertThat(item.getApprovalStatus()).isEqualTo("APPROVED"));
        mvc.perform(get(attachment).session(session(otherStaff))).andExpect(status().isOk())
                .andExpect(content().string("budget evidence"));
    }

    @Test
    void assignedLeaderCanRejectAStaffContractRevision() throws Exception {
        String base = "/api/v1/projects/" + project.getProjectId() + "/revisions";
        var created = mvc.perform(post(base).session(session(staff)).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"계약 변경\",\"reason\":\"범위 변경\"}"))
                .andExpect(status().isCreated()).andReturn();
        String revisionId = json.readTree(created.getResponse().getContentAsString()).get("revision_id").asText();
        mvc.perform(put(base + "/" + revisionId + "/contract").session(session(staff)).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"target_profit_rate\":20,\"items\":[{\"work_category\":\"목공\",\"quoted_amount\":1200,\"contract_amount\":1200,\"vat_amount\":120}]}"))
                .andExpect(status().isOk());
        var requested = mvc.perform(post(base + "/" + revisionId + "/request-approval").session(session(staff)).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk()).andExpect(jsonPath("approval_stage").value("TEAM_LEADER")).andReturn();
        String approvalId = json.readTree(requested.getResponse().getContentAsString()).get("approval_id").asText();
        mvc.perform(post("/api/v1/approvals/" + approvalId + "/reject").session(session(leader)).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"comment\":\"계약서 보완\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("status").value("REJECTED"));
        mvc.perform(get(base + "/" + revisionId).session(session(staff))).andExpect(status().isOk())
                .andExpect(jsonPath("status").value("REJECTED"));
    }

    @Test
    void legacyGlobalVisibilityCannotExposeLeadersPendingRequestToStaff() throws Exception {
        PositionPermission permission = permissions.findById("STAFF:APPROVAL_ALL").orElseThrow();
        boolean previous = permission.isAllowed();
        permission.update(true, "legacy-setting");
        permissions.saveAndFlush(permission);
        try {
            Approval approval = requestClaim(leader);
            mvc.perform(get(path(approval)).session(session(staff))).andExpect(status().isNotFound());
            mvc.perform(get("/api/v1/approvals").param("projectId", project.getProjectId()).session(session(staff)))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        } finally {
            permission = permissions.findById("STAFF:APPROVAL_ALL").orElseThrow();
            permission.update(previous, "test-restore");
            permissions.saveAndFlush(permission);
        }
    }

    @Test
    void identifiableExistingPendingStaffRequestsAreUpgradedAtStartup() throws Exception {
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                new SessionUser(staff.getId(), staff.getAuthVersion(), staff.getName(), "STAFF"), null, List.of()));
        SecurityContextHolder.setContext(context);
        Approval legacy = approvals.saveAndFlush(new Approval(UUID.randomUUID().toString(), project.getProjectId(),
                "EXECUTION_BUDGET", project.getProjectId(), "기존 직원 요청", BigDecimal.TEN,
                BigDecimal.ZERO, BigDecimal.TEN, staff.getName(), "기존 대기 건"));
        SecurityContextHolder.clearContext();
        routeInitializer.run();
        Approval upgraded = approvals.findById(legacy.getApprovalId()).orElseThrow();
        assertThat(upgraded.getApprovalStage()).isEqualTo(ApprovalStage.TEAM_LEADER);
        assertThat(upgraded.getTeamLeaderId()).isEqualTo(leader.getId());
        assertThat(upgraded.getStatus()).isEqualTo(ApprovalStatus.REQUESTED);
        mvc.perform(post(path(upgraded) + "/approve").session(session(ceo)).with(csrf())).andExpect(status().isForbidden());
    }
}
