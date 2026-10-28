package com.construction.management.account;
import com.construction.management.auth.*;
import static com.construction.management.account.AccountDtos.*;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
@Transactional
public class AccountService {
 @jakarta.persistence.PersistenceContext private jakarta.persistence.EntityManager entityManager;
 private final AppUserRepository users; private final PositionPermissionRepository permissions; private final SecurityAuditLogRepository logs;
 private final PermissionService policy; private final PasswordEncoder encoder; private final AuditService audit;
 public AccountService(AppUserRepository users,PositionPermissionRepository permissions,SecurityAuditLogRepository logs,PermissionService policy,PasswordEncoder encoder,AuditService audit) { this.users=users; this.permissions=permissions; this.logs=logs; this.policy=policy; this.encoder=encoder; this.audit=audit; }
 public Page<UserView> search(String keyword,Position position,AccountStatus status,int page,int size) {
  policy.requireAdmin();
  Specification<AppUser> spec=(root,q,cb) -> { var predicates=new ArrayList<jakarta.persistence.criteria.Predicate>();
   if(keyword!=null && !keyword.isBlank()) { String k="%"+keyword.trim().toLowerCase(Locale.ROOT)+"%"; predicates.add(cb.or(cb.like(cb.lower(root.get("loginId")),k),cb.like(cb.lower(root.get("name")),k))); }
   if(position!=null) predicates.add(cb.equal(root.get("position"),position)); if(status!=null) predicates.add(cb.equal(root.get("status"),status));
   return cb.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new)); };
  return users.findAll(spec,PageRequest.of(Math.max(0,page),Math.min(100,Math.max(1,size)),Sort.by("createdAt").descending())).map(UserView::from);
 }
 public UserView get(String id) { policy.requireAdmin(); return UserView.from(find(id)); }
 public UserView create(Create req) {
  policy.requireAdmin(); String login=PasswordPolicy.normalizeLogin(req.login_id());
  if(!login.matches("[a-z0-9._-]{4,50}")) throw new IllegalArgumentException("로그인 ID는 영문 소문자·숫자·점·밑줄·하이픈 4~50자로 입력하세요.");
  if(users.existsByLoginId(login)) throw new ApiException(409,"DUPLICATE_LOGIN","사용할 수 없는 로그인 ID입니다.");
  PasswordPolicy.validate(req.temporary_password(),login);
  AppUser user=new AppUser(login,encoder.encode(req.temporary_password()),req.name().trim(),AccountType.USER,req.position(),CurrentActor.id()); user.phone=req.phone(); user.email=req.email();
  users.saveAndFlush(user); audit.record("ACCOUNT_CREATE","USER",user.getId(),null,snapshot(user),null,"SUCCESS"); return UserView.from(user);
 }
 public UserView update(String id,Update req) {
  policy.requireAdmin(); var admins=lockAdmins(); AppUser u=locked(id,req.version()); String before=snapshot(u);
  if(req.status()==AccountStatus.DELETED || u.status==AccountStatus.DELETED) throw new ApiException(409,"DELETED_ACCOUNT","삭제는 별도 동작으로 처리하며 삭제 계정은 수정할 수 없습니다.");
  if(u.accountType==AccountType.ADMIN) {
   if(req.position()!=null) throw new IllegalArgumentException("어드민의 직책은 지정할 수 없습니다.");
   if(id.equals(CurrentActor.id()) && req.status()!=AccountStatus.ACTIVE) throw new IllegalArgumentException("자기 계정은 비활성화할 수 없습니다.");
   if(u.status==AccountStatus.ACTIVE && req.status()!=AccountStatus.ACTIVE && admins.stream().filter(a -> a.status==AccountStatus.ACTIVE).count()<=1) throw new ApiException(409,"LAST_ADMIN","마지막 활성 어드민은 비활성화할 수 없습니다.");
  }
  if(u.accountType==AccountType.USER && req.position()==null) throw new IllegalArgumentException("직책을 선택하세요.");
  boolean security=u.position!=req.position() || u.status!=req.status(); if(security) reason(req.reason());
  u.name=req.name().trim(); u.position=req.position(); u.status=req.status(); u.phone=req.phone(); u.email=req.email(); if(security) u.authVersion++; u.touch(CurrentActor.id());
  users.flush(); audit.record("ACCOUNT_UPDATE","USER",id,before,snapshot(u),req.reason(),"SUCCESS"); return UserView.from(u);
 }
 public void delete(String id,Action req) {
  policy.requireAdmin(); var admins=lockAdmins(); AppUser u=locked(id,req.version());
  if(id.equals(CurrentActor.id())) throw new IllegalArgumentException("자기 계정은 삭제할 수 없습니다.");
  if(u.accountType==AccountType.ADMIN && admins.stream().filter(a -> a.status==AccountStatus.ACTIVE).count()<=1) throw new ApiException(409,"LAST_ADMIN","마지막 활성 어드민은 삭제할 수 없습니다.");
  if(u.status==AccountStatus.DELETED) throw new ApiException(409,"DELETED_ACCOUNT","이미 삭제된 계정입니다.");
  String before=snapshot(u); u.status=AccountStatus.DELETED; u.authVersion++; u.deletedAt=java.time.LocalDateTime.now(); u.deletedBy=CurrentActor.id(); u.touch(CurrentActor.id());
  audit.record("ACCOUNT_DELETE","USER",id,before,snapshot(u),req.reason(),"SUCCESS");
 }
 public UserView reset(String id,Reset req) { policy.requireAdmin(); AppUser u=locked(id,req.version()); ensureUsable(u); PasswordPolicy.validate(req.temporary_password(),u.loginId); u.changePassword(encoder.encode(req.temporary_password()),true,CurrentActor.id()); u.unlock(CurrentActor.id()); users.flush(); audit.record("PASSWORD_RESET","USER",id,null,null,req.reason(),"SUCCESS"); return UserView.from(u); }
 public UserView unlock(String id,Action req) { policy.requireAdmin(); AppUser u=locked(id,req.version()); ensureUsable(u); u.unlock(CurrentActor.id()); users.flush(); audit.record("ACCOUNT_UNLOCK","USER",id,null,null,req.reason(),"SUCCESS"); return UserView.from(u); }
 public List<PositionView> positions() { policy.requireAdmin(); return Arrays.stream(Position.values()).map(this::positionView).toList(); }
 private PositionView positionView(Position p) { return new PositionView(p,permissions.findByPosition(p).stream().sorted(Comparator.comparing(v -> v.permissionKey.ordinal())).map(v -> new PermissionValue(v.permissionKey,v.permissionKey.label,v.allowed,!v.permissionKey.locked(),v.permissionKey.locked()?"대표 결재·재무 지급 통제 규칙은 변경할 수 없습니다.":null,v.permissionKey.dependency,v.version)).toList()); }
 public PositionView savePermissions(Position p,PermissionUpdate req) {
  policy.requireAdmin(); List<PositionPermission> values=permissions.lockPosition(p);
  Map<Permission,Boolean> proposed=new EnumMap<>(Permission.class); values.forEach(v -> proposed.put(v.permissionKey,v.allowed));
  if(req.permissions().isEmpty()) throw new IllegalArgumentException("변경할 권한을 선택하세요.");
  for(var e:req.permissions().entrySet()) {
   Permission key=e.getKey(); if(key.locked() || e.getValue()==null) throw new IllegalArgumentException("잠긴 권한은 변경할 수 없습니다.");
   PositionPermission v=values.stream().filter(x -> x.permissionKey==key).findFirst().orElseThrow();
   checkVersion(v.version,req.versions().get(key)); proposed.put(key,e.getValue());
  }
  for(var e:proposed.entrySet()) if(e.getValue() && e.getKey().dependency!=null && !proposed.get(Permission.valueOf(e.getKey().dependency))) throw new IllegalArgumentException("조회 권한을 먼저 허용하세요: "+e.getKey().label);
  String before=proposedSnapshot(values);
  values.forEach(v -> { if(req.permissions().containsKey(v.permissionKey)) v.update(proposed.get(v.permissionKey),CurrentActor.id()); });
  permissions.flush(); audit.record("PERMISSIONS_UPDATE","POSITION",p.name(),before,proposedSnapshot(values),req.reason(),"SUCCESS"); return positionView(p);
 }
 public Page<SecurityAuditLog> auditLogs(String action,String keyword,int page,int size) {
  policy.requireAdmin(); Specification<SecurityAuditLog> spec=(r,q,c) -> { var predicates=new ArrayList<jakarta.persistence.criteria.Predicate>(); if(action!=null && !action.isBlank()) predicates.add(c.equal(r.get("action"),action)); if(keyword!=null && !keyword.isBlank()) { String k="%"+keyword.trim()+"%"; predicates.add(c.or(c.like(r.get("actorName"),k),c.like(r.get("targetId"),k))); } return c.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new)); };
  return logs.findAll(spec,PageRequest.of(Math.max(page,0),Math.min(100,Math.max(size,1)),Sort.by("createdAt").descending()));
 }
 public List<UserOption> options() { policy.require(Permission.PROJECT_READ); return users.findByAccountTypeAndStatus(AccountType.USER,AccountStatus.ACTIVE).stream().map(u -> new UserOption(u.getId(),u.name,u.position)).toList(); }
 public String displayName(String id) { return id==null || id.isBlank()?id:users.findById(id).map(AppUser::getName).orElse(id); }
 public void validateAssignee(String id,Position required) { if(id==null || id.isBlank()) return; AppUser u=find(id); if(u.accountType!=AccountType.USER || u.status!=AccountStatus.ACTIVE || (required!=null && required!=u.position)) throw new IllegalArgumentException("활성 업무 계정을 선택하세요."); }
 private List<AppUser> lockAdmins() { var admins=users.lockAdmins(); admins.forEach(entityManager::refresh); return admins; }
 private AppUser find(String id) { return users.findById(id).orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("계정을 찾을 수 없습니다.")); }
 private AppUser locked(String id,Long version) { AppUser u=users.lockById(id).orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("계정을 찾을 수 없습니다.")); checkVersion(u.version,version); return u; }
 private void checkVersion(long actual,Long expected) { if(expected==null || actual!=expected) throw new ApiException(409,"STALE_VERSION","다른 사용자가 변경한 자료입니다. 다시 조회하세요."); }
 private void ensureUsable(AppUser u) { if(u.status==AccountStatus.DELETED) throw new ApiException(409,"DELETED_ACCOUNT","삭제된 계정입니다."); }
 private void reason(String value) { if(value==null || value.isBlank()) throw new IllegalArgumentException("변경 사유를 입력하세요."); }
 private String snapshot(AppUser u) { return "login="+u.loginId+", name="+u.name+", position="+u.position+", status="+u.status+", phone="+u.phone+", email="+u.email; }
 private String proposedSnapshot(List<PositionPermission> values) { return values.stream().map(v -> v.permissionKey+"="+v.allowed).collect(java.util.stream.Collectors.joining(",")); }
}
