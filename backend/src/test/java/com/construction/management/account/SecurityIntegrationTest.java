package com.construction.management.account;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import com.construction.management.auth.*;
import com.construction.management.project.domain.*;
import com.construction.management.project.repository.*;
import com.fasterxml.jackson.databind.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.*;

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:security-test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1", "app.demo-data.enabled=false", "app.contract-file.upload-dir=build/test-uploads/contracts", "app.site-record.upload-dir=build/test-uploads/site", "app.execution-budget-attachment.upload-dir=build/test-uploads/budget"})
@AutoConfigureMockMvc
class SecurityIntegrationTest {
 @Autowired MockMvc mvc;
 @Autowired ObjectMapper json;
 @Autowired AppUserRepository users;
 @Autowired PasswordEncoder encoder;
 @Autowired ProjectRepository projects;
 @Autowired PurchaseOrderRepository orders;
 @Autowired ProgressClaimRepository claims;
 @Autowired ActualPaymentRepository payments;
 @Autowired ApprovalRepository approvals;
 @Autowired PositionPermissionRepository permissions;
 @Autowired PermissionService permissionService;
 @Autowired SecurityAuditLogRepository audit;
 @Autowired AdminBootstrap bootstrap;
 private static final String PASSWORD="Temporary-pass123!";

 @AfterEach void clear() { SecurityContextHolder.clearContext(); }
 private String id() { return UUID.randomUUID().toString(); }
 private AppUser user(Position position,boolean temporary) { return account(AccountType.USER,position,temporary); }
 private AppUser admin() { return account(AccountType.ADMIN,null,false); }
 private AppUser account(AccountType type,Position position,boolean temporary) {
  AppUser u=new AppUser("user-"+id().substring(0,8),encoder.encode(PASSWORD),"사용자-"+id().substring(0,5),type,position,"test"); u.mustChangePassword=temporary; return users.saveAndFlush(u);
 }
 private void act(AppUser u) { SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(new SessionUser(u.getId(),u.authVersion,u.name,u.position==null?null:u.position.name()),null,List.of())); }
 private JsonNode body(MvcResult result) throws Exception { return json.readTree(result.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8)); }
 private MockHttpSession login(AppUser u) throws Exception {
  var tokenResult=mvc.perform(get("/api/v1/auth/csrf")).andExpect(status().isOk()).andReturn();
  MockHttpSession session=(MockHttpSession)tokenResult.getRequest().getSession();
  JsonNode token=body(tokenResult); String oldId=session.getId();
  var result=mvc.perform(post("/api/v1/auth/login").session(session).header(token.get("header_name").asText(),token.get("token").asText()).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("login_id",u.loginId,"password",PASSWORD)))).andExpect(status().isOk()).andReturn();
  assertThat(session.getId()).isNotEqualTo(oldId);
  assertThat(result.getResponse().getContentAsString()).doesNotContain("password_hash",PASSWORD);
  return (MockHttpSession)result.getRequest().getSession();
 }
 private Project project() { return projects.saveAndFlush(new Project(id(),"P-"+id().substring(0,8),"인증 통합 테스트")); }
 private Map<String,Object> createData(String login) { return Map.of("login_id",login,"name","새 계정","position","STAFF","temporary_password",PASSWORD); }

 @Test void anonymousAndUnknownEndpointsAreProtected() throws Exception {
  for(String path:List.of("/api/v1/projects","/api/dashboard","/api/v1/approvals","/api/v1/projects/p/contract-files/f","/api/v1/projects/p/daily-logs/d/photos/f")) mvc.perform(get(path)).andExpect(status().isUnauthorized()).andExpect(jsonPath("code").value("UNAUTHENTICATED"));
  mvc.perform(get("/api/unknown")).andExpect(status().isUnauthorized());
  mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content("{}" )).andExpect(status().isForbidden()).andExpect(jsonPath("code").value("CSRF_INVALID"));
  mvc.perform(get("/h2-console")).andExpect(status().isUnauthorized());
 }

 @Test void realSessionCsrfLifecycleAndForcedPasswordChange() throws Exception {
  AppUser u=user(Position.STAFF,true); MockHttpSession session=login(u);
  mvc.perform(get("/api/v1/auth/me").session(session)).andExpect(status().isOk()).andExpect(jsonPath("must_change_password").value(true));
  mvc.perform(get("/api/v1/projects").session(session)).andExpect(status().isForbidden()).andExpect(jsonPath("code").value("PASSWORD_CHANGE_REQUIRED"));
  mvc.perform(put("/api/v1/auth/password").session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("current_password",PASSWORD,"new_password","Permanent-pass456!","confirm_password","Permanent-pass456!")))).andExpect(status().isOk());
  assertThat(session.isInvalid()).isTrue();
  AppUser changed=users.findById(u.getId()).orElseThrow(); assertThat(changed.mustChangePassword).isFalse(); assertThat(changed.authVersion).isEqualTo(1); assertThat(encoder.matches("Permanent-pass456!",changed.passwordHash)).isTrue();
 }

 @Test void adminIsSeparateFromEveryBusinessRole() throws Exception {
  MockHttpSession admin=login(admin());
  mvc.perform(get("/api/v1/admin/users").session(admin)).andExpect(status().isOk());
  mvc.perform(get("/api/v1/projects").session(admin)).andExpect(status().isForbidden());
  mvc.perform(post("/api/v1/approvals/fake/approve").session(admin).with(csrf())).andExpect(status().isForbidden());
  mvc.perform(post("/api/v1/projects/p/progress-claims/c/payments").session(admin).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{}" )).andExpect(status().isForbidden());
  for(Position role:Position.values()) mvc.perform(get("/api/v1/admin/users").session(login(user(role,false)))).andExpect(status().isForbidden());
 }

 @Test void accountCrudUsesStrictDtosNormalizationAndOptimisticVersions() throws Exception {
  MockHttpSession session=login(admin()); String login="staff-"+id().substring(0,8);
  var created=mvc.perform(post("/api/v1/admin/users").session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(createData("  "+login.toUpperCase(Locale.ROOT)+"  ")))).andExpect(status().isCreated()).andExpect(jsonPath("login_id").value(login)).andReturn();
  String uid=body(created).get("id").asText(); long version=body(created).get("version").asLong();
  mvc.perform(post("/api/v1/admin/users").session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(createData(login)))).andExpect(status().isConflict());
  Map<String,Object> malicious=new LinkedHashMap<>(createData("new-"+id().substring(0,8))); malicious.put("account_type","ADMIN");
  mvc.perform(post("/api/v1/admin/users").session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(malicious))).andExpect(status().isBadRequest());
  Map<String,Object> update=Map.of("name","변경 이름","position","TEAM_LEADER","status","INACTIVE","version",version,"reason","직책 및 상태 변경");
  var changed=mvc.perform(patch("/api/v1/admin/users/"+uid).session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(update))).andExpect(status().isOk()).andReturn();
  mvc.perform(patch("/api/v1/admin/users/"+uid).session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(update))).andExpect(status().isConflict());
  long nextVersion=body(changed).get("version").asLong();
  mvc.perform(delete("/api/v1/admin/users/"+uid).session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("version",nextVersion,"reason","계정 삭제")))).andExpect(status().isNoContent());
  assertThat(users.findById(uid).orElseThrow().status).isEqualTo(AccountStatus.DELETED);
  mvc.perform(post("/api/v1/admin/users").session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(createData(login)))).andExpect(status().isConflict());
  mvc.perform(get("/api/v1/users/options").session(login(user(Position.CEO,false)))).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString(uid))));
 }

 @Test void invalidRolesPasswordPolicyAndAdministratorSelfProtection() throws Exception {
  AppUser admin=admin(); MockHttpSession session=login(admin);
  for(Object position:List.of("DIRECTOR",List.of("CEO","STAFF"))) {
   Map<String,Object> data=new LinkedHashMap<>(createData("bad-"+id().substring(0,8))); data.put("position",position);
   mvc.perform(post("/api/v1/admin/users").session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(data))).andExpect(status().isBadRequest());
  }
  assertThatThrownBy(() -> PasswordPolicy.validate("가".repeat(25),"someone")).isInstanceOf(IllegalArgumentException.class);
  assertThatThrownBy(() -> PasswordPolicy.validate("short","someone")).isInstanceOf(IllegalArgumentException.class);
  mvc.perform(delete("/api/v1/admin/users/"+admin.getId()).session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("version",users.findById(admin.getId()).orElseThrow().version,"reason","자기 삭제")))).andExpect(status().isBadRequest());
 }

 @Test void csrfProtectsJsonMultipartAndLogout() throws Exception {
  MockHttpSession session=login(user(Position.STAFF,false)); Project p=project();
  mvc.perform(put("/api/v1/projects/"+p.getProjectId()+"/contracts").session(session).contentType(MediaType.APPLICATION_JSON).content("{}" )).andExpect(status().isForbidden()).andExpect(jsonPath("code").value("CSRF_INVALID"));
  mvc.perform(multipart("/api/v1/projects/"+p.getProjectId()+"/execution-budget-details/request-approval").file(new MockMultipartFile("files","test.txt","text/plain",new byte[]{1})).session(session)).andExpect(status().isForbidden());
  mvc.perform(post("/api/v1/auth/logout").session(session)).andExpect(status().isForbidden());
  mvc.perform(post("/api/v1/auth/logout").session(session).with(csrf())).andExpect(status().isOk()); assertThat(session.isInvalid()).isTrue();
 }

 @Test void failedLoginLocksAccountAndAdminCanUnlockAndReset() throws Exception {
  AppUser u=user(Position.STAFF,false);
  for(int n=0;n<5;n++) mvc.perform(post("/api/v1/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("login_id",u.loginId,"password","incorrect-password")))).andExpect(status().isUnauthorized()).andExpect(jsonPath("code").value("LOGIN_FAILED"));
  AppUser locked=users.findById(u.getId()).orElseThrow(); assertThat(locked.locked()).isTrue(); assertThat(locked.failedLoginCount).isEqualTo(5);
  MockHttpSession session=login(admin());
  var result=mvc.perform(post("/api/v1/admin/users/"+u.getId()+"/unlock").session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("version",locked.version,"reason","본인 확인 후 잠금 해제")))).andExpect(status().isOk()).andReturn();
  assertThat(users.findById(u.getId()).orElseThrow().failedLoginCount).isZero(); MockHttpSession old=login(users.findById(u.getId()).orElseThrow());
  mvc.perform(post("/api/v1/admin/users/"+u.getId()+"/reset-password").session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("version",users.findById(u.getId()).orElseThrow().version,"reason","비밀번호 초기화","temporary_password","Reset-password123!")))).andExpect(status().isOk());
  mvc.perform(get("/api/v1/auth/me").session(old)).andExpect(status().isUnauthorized()).andExpect(jsonPath("code").value("SESSION_INVALIDATED"));
 }

 @Test void positionAndStatusChangesInvalidateOtherBrowserSessions() throws Exception {
  AppUser u=user(Position.STAFF,false); MockHttpSession one=login(u); MockHttpSession two=login(u); MockHttpSession admin=login(admin());
  AppUser latest=users.findById(u.getId()).orElseThrow();
  mvc.perform(patch("/api/v1/admin/users/"+u.getId()).session(admin).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("name",u.name,"position","TEAM_LEADER","status","ACTIVE","version",latest.version,"reason","팀장으로 변경")))).andExpect(status().isOk());
  for(MockHttpSession old:List.of(one,two)) mvc.perform(get("/api/v1/projects").session(old)).andExpect(status().isUnauthorized());
 }

 @Test void mutablePermissionsTakeEffectOnNextRequestWithoutLoggingOut() throws Exception {
  AppUser u=user(Position.TEAM_LEADER,false); MockHttpSession session=login(u); MockHttpSession admin=login(admin());
  var value=permissions.findById("TEAM_LEADER:SITE_WRITE").orElseThrow(); long version=value.version;
  try {
   mvc.perform(put("/api/v1/admin/positions/TEAM_LEADER/permissions").session(admin).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("permissions",Map.of("SITE_WRITE",false),"versions",Map.of("SITE_WRITE",version),"reason","현장 기록 권한 변경")))).andExpect(status().isOk());
   mvc.perform(get("/api/v1/auth/me").session(session)).andExpect(status().isOk()).andExpect(jsonPath("permissions",org.hamcrest.Matchers.not(org.hamcrest.Matchers.hasItem("SITE_WRITE"))));
   mvc.perform(post("/api/v1/projects/p/daily-logs").session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"log_date\":\"2026-10-28\",\"site_manager_name\":\"담당자\"}" )).andExpect(status().isForbidden());
   permissionService.initializeDefaults(); assertThat(permissions.findById("TEAM_LEADER:SITE_WRITE").orElseThrow().allowed).isFalse();
   mvc.perform(put("/api/v1/admin/positions/TEAM_LEADER/permissions").session(admin).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("permissions",Map.of("SITE_WRITE",true),"versions",Map.of("SITE_WRITE",version),"reason","오래된 화면 저장")))).andExpect(status().isConflict());
  } finally { var restore=permissions.findById("TEAM_LEADER:SITE_WRITE").orElseThrow(); restore.update(true,"test"); permissions.saveAndFlush(restore); }
 }

 @Test void lockedAndUnknownPermissionsAndInvalidDependenciesAreRejected() throws Exception {
  MockHttpSession session=login(admin());
  for(String key:List.of("APPROVAL_DECIDE","PAYMENT_EXECUTE","EXECUTIVE_CORRECT","UNKNOWN_KEY")) mvc.perform(put("/api/v1/admin/positions/STAFF/permissions").session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("permissions",Map.of(key,true),"versions",Map.of(key,0),"reason","통제 우회 시도")))).andExpect(status().isBadRequest());
  long version=permissions.findById("STAFF:SITE_READ").orElseThrow().version;
  mvc.perform(put("/api/v1/admin/positions/STAFF/permissions").session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("permissions",Map.of("SITE_READ",false),"versions",Map.of("SITE_READ",version),"reason","의존성 위반")))).andExpect(status().isBadRequest());
 }

 @Test void staffCannotTakeOverDraftsOrReadOtherApprovalDetailsAndAttachments() throws Exception {
  AppUser first=user(Position.STAFF,false), second=user(Position.STAFF,false); Project p=project();
  MockHttpSession owner=login(first), other=login(second);
  String contract=json.writeValueAsString(Map.of("target_profit_rate",45,"items",List.of(Map.of("work_category","목공","quoted_amount",1000,"contract_amount",1000,"vat_amount",100))));
  mvc.perform(put("/api/v1/projects/"+p.getProjectId()+"/contracts").session(owner).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(contract)).andExpect(status().isOk());
  mvc.perform(put("/api/v1/projects/"+p.getProjectId()+"/contracts").session(other).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(contract)).andExpect(status().isNotFound());
  act(first); Approval a=approvals.saveAndFlush(new Approval(id(),p.getProjectId(),"EXECUTION_BUDGET",p.getProjectId(),"소유자 테스트",BigDecimal.ONE,BigDecimal.ZERO,BigDecimal.ONE,"위조 이름",null)); clear();
  mvc.perform(get("/api/v1/approvals/"+a.getApprovalId()).session(other)).andExpect(status().isNotFound());
  mvc.perform(get("/api/v1/projects/"+p.getProjectId()+"/approvals/"+a.getApprovalId()+"/execution-budget-attachments/file").session(other)).andExpect(status().isNotFound());
  mvc.perform(get("/api/v1/approvals?userId="+first.getId()).session(other)).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString(a.getApprovalId()))));
 }

 @Test void ceoApprovalDoesNotPayAndOnlyFinanceCanRegisterActualPayment() throws Exception {
  AppUser staff=user(Position.STAFF,false), ceo=user(Position.CEO,false), finance=user(Position.FINANCE,false); Project p=project();
  PurchaseOrder po=orders.saveAndFlush(new PurchaseOrder(id(),p.getProjectId(),"budget","테스트 업체",BigDecimal.valueOf(1000)));
  act(staff); ProgressClaim claim=claims.saveAndFlush(new ProgressClaim(id(),po.getPoId(),1,BigDecimal.valueOf(600),ApprovalStatus.REQUESTED));
  Approval a=approvals.saveAndFlush(new Approval(id(),p.getProjectId(),"PROGRESS_CLAIM",claim.getClaimId(),"기성 요청",BigDecimal.valueOf(600),BigDecimal.ZERO,BigDecimal.valueOf(600),"위조",null)); clear();
  MockHttpSession employee=login(staff);
  mvc.perform(post("/api/v1/approvals/"+a.getApprovalId()+"/approve").session(employee).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"approver\":\"대표\"}" )).andExpect(status().isForbidden());
  mvc.perform(post("/api/v1/approvals/"+a.getApprovalId()+"/approve").session(login(ceo)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"approver\":\"위조 이름\"}" )).andExpect(status().isOk()).andExpect(jsonPath("approver").value(ceo.name)).andExpect(jsonPath("approver_user_id").value(ceo.getId()));
  assertThat(payments.findByClaimId(claim.getClaimId())).isEmpty();
  mvc.perform(post("/api/v1/projects/"+p.getProjectId()+"/payment-requests").session(employee).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("po_ids",List.of(po.getPoId()))))).andExpect(status().isCreated());
  Approval paymentApproval=approvals.findByProjectIdOrderByRequestedAtDesc(p.getProjectId()).stream().filter(v -> "PAYMENT".equals(v.getApprovalType())).findFirst().orElseThrow();
  mvc.perform(post("/api/v1/approvals/"+paymentApproval.getApprovalId()+"/approve").session(login(ceo)).with(csrf())).andExpect(status().isOk());
  assertThat(payments.findByClaimId(claim.getClaimId())).isEmpty();
  String path="/api/v1/projects/"+p.getProjectId()+"/progress-claims/"+claim.getClaimId()+"/payments"; String pay="{\"paid_amount\":600,\"paid_date\":\"2026-10-28\"}";
  mvc.perform(post(path).session(login(ceo)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(pay)).andExpect(status().isForbidden());
  mvc.perform(post(path).session(login(user(Position.TEAM_LEADER,false))).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(pay)).andExpect(status().isForbidden());
  MockHttpSession financeSession=login(finance);
  mvc.perform(post(path).session(financeSession).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(pay)).andExpect(status().isOk());
  assertThat(payments.findByClaimId(claim.getClaimId())).singleElement().satisfies(payment -> { assertThat(payment.getPaidAmount()).isEqualByComparingTo("600"); assertThat(payment.getCreatedUserId()).isEqualTo(finance.getId()); assertThat(payment.getCreatedUserName()).isEqualTo(finance.name); });
  mvc.perform(post(path).session(financeSession).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(pay)).andExpect(status().isBadRequest());
  assertThat(orders.findById(po.getPoId()).orElseThrow().getInitialPoAmount()).isEqualByComparingTo("1000");
 }

 @Test void settlementAliasCannotBypassAuthorizationAndApprovedProjectsCannotBeDeleted() throws Exception {
  Project p=project(); orders.saveAndFlush(new PurchaseOrder(id(),p.getProjectId(),null,"보존 업체",BigDecimal.TEN));
  MockHttpSession staff=login(user(Position.STAFF,false));
  for(String alias:List.of("complete","request-approval")) mvc.perform(post("/api/v1/projects/"+p.getProjectId()+"/settlement/"+alias).session(staff).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{}" )).andExpect(status().isForbidden());
  mvc.perform(delete("/api/v1/projects/"+p.getProjectId()).session(login(user(Position.CEO,false))).with(csrf())).andExpect(status().isConflict());
  assertThat(projects.existsById(p.getProjectId())).isTrue();
 }

 @Test void concurrentAdminEditsReturnOneConflict() throws Exception {
  AppUser admin=admin(), target=user(Position.STAFF,false); MockHttpSession session=login(admin);
  String content=json.writeValueAsString(Map.of("name","동시 수정","position","STAFF","status","ACTIVE","version",target.version,"reason","동시 저장"));
  ExecutorService pool=Executors.newFixedThreadPool(2);
  try {
   Callable<Integer> task=() -> mvc.perform(patch("/api/v1/admin/users/"+target.getId()).session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(content)).andReturn().getResponse().getStatus();
   var futures=pool.invokeAll(List.of(task,task)); var statuses=List.of(futures.get(0).get(),futures.get(1).get()); assertThat(statuses).containsExactlyInAnyOrder(200,409);
  } finally { pool.shutdownNow(); }
 }

 @Test void accountDisableAndDeleteInvalidateAllExistingSessions() throws Exception {
  MockHttpSession admin=login(admin());
  for(String operation:List.of("INACTIVE","DELETED")) {
   AppUser target=user(Position.STAFF,false); MockHttpSession old=login(target); AppUser latest=users.findById(target.getId()).orElseThrow();
   if(operation.equals("DELETED")) mvc.perform(delete("/api/v1/admin/users/"+target.getId()).session(admin).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("version",latest.version,"reason","퇴사 처리")))).andExpect(status().isNoContent());
   else mvc.perform(patch("/api/v1/admin/users/"+target.getId()).session(admin).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("name",target.name,"position","STAFF","status","INACTIVE","version",latest.version,"reason","사용 중지")))).andExpect(status().isOk());
   mvc.perform(get("/api/v1/auth/me").session(old)).andExpect(status().isUnauthorized());
  }
 }
 @Test void historicalAuthorAndApproverNamesSurviveRenamingAndDeletion() throws Exception {
  AppUser requester=user(Position.STAFF,false), ceo=user(Position.CEO,false); Project p=project();
  PurchaseOrder po=orders.saveAndFlush(new PurchaseOrder(id(),p.getProjectId(),null,"이력 보존",BigDecimal.TEN));
  ProgressClaim claim=claims.saveAndFlush(new ProgressClaim(id(),po.getPoId(),1,BigDecimal.TEN,ApprovalStatus.REQUESTED));
  act(requester); Approval a=approvals.saveAndFlush(new Approval(id(),p.getProjectId(),"PROGRESS_CLAIM",claim.getClaimId(),"이력 보존",BigDecimal.TEN,BigDecimal.ZERO,BigDecimal.TEN,"외부 입력",null)); clear();
  mvc.perform(post("/api/v1/approvals/"+a.getApprovalId()+"/approve").session(login(ceo)).with(csrf())).andExpect(status().isOk());
  MockHttpSession admin=login(admin()); AppUser latest=users.findById(ceo.getId()).orElseThrow();
  mvc.perform(patch("/api/v1/admin/users/"+ceo.getId()).session(admin).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("name","새 대표 이름","position","CEO","status","ACTIVE","version",latest.version)))).andExpect(status().isOk());
  mvc.perform(delete("/api/v1/admin/users/"+requester.getId()).session(admin).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("version",requester.version,"reason","요청자 계정 삭제")))).andExpect(status().isNoContent());
  Approval preserved=approvals.findById(a.getApprovalId()).orElseThrow(); assertThat(preserved.getCreatedUserId()).isEqualTo(requester.getId()); assertThat(preserved.getRequestUser()).isEqualTo(requester.name); assertThat(preserved.getApprover()).isEqualTo(ceo.name); assertThat(preserved.getApproverUserId()).isEqualTo(ceo.getId());
 }
 @Test void concurrentFailedLoginsCannotBypassAccountLock() throws Exception {
  AppUser u=user(Position.STAFF,false); ExecutorService pool=Executors.newFixedThreadPool(6);
  try {
   Callable<Integer> task=() -> mvc.perform(post("/api/v1/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("login_id",u.loginId,"password","incorrect-password")))).andReturn().getResponse().getStatus();
   var results=pool.invokeAll(Collections.nCopies(6,task)); for(var result:results) assertThat(result.get()).isEqualTo(401);
   AppUser locked=users.findById(u.getId()).orElseThrow(); assertThat(locked.failedLoginCount).isEqualTo(5); assertThat(locked.locked()).isTrue();
  } finally { pool.shutdownNow(); }
 }
 @Test void sourceRateLimitAndInvalidCsrfAreEnforced() throws Exception {
  for(int attempt=0;attempt<50;attempt++) mvc.perform(post("/api/v1/auth/login").with(request -> { request.setRemoteAddr("192.0.2.50"); return request; }).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"login_id\":\"missing-source-account\",\"password\":\"incorrect-password\"}" )).andExpect(status().isUnauthorized());
  mvc.perform(post("/api/v1/auth/login").with(request -> { request.setRemoteAddr("192.0.2.50"); return request; }).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"login_id\":\"missing-source-account\",\"password\":\"incorrect-password\"}" )).andExpect(status().isTooManyRequests()).andExpect(jsonPath("code").value("LOGIN_RATE_LIMIT"));
  mvc.perform(post("/api/v1/auth/login").with(csrf().useInvalidToken()).contentType(MediaType.APPLICATION_JSON).content("{}" )).andExpect(status().isForbidden()).andExpect(jsonPath("code").value("CSRF_INVALID"));
 }
 @Test void bootstrapRerunCannotOverwriteAccountsAndAuditContainsNoCredentials() {
  AppUser u=admin(); String hash=u.passwordHash;
  assertThatThrownBy(() -> bootstrap.create("bootstrap","관리자","Another-password123!")).isInstanceOf(IllegalStateException.class);
  assertThat(users.findById(u.getId()).orElseThrow().passwordHash).isEqualTo(hash);
  assertThat(audit.findAll()).allSatisfy(log -> { assertThat(String.valueOf(log.getBeforeValue())).doesNotContain(PASSWORD,"$2a$"); assertThat(String.valueOf(log.getAfterValue())).doesNotContain(PASSWORD,"$2a$"); });
 }
}
