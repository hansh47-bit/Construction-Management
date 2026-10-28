package com.construction.management.auth;
import com.construction.management.account.*;
import java.time.*;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
public class LoginService {
 private final AppUserRepository users; private final PasswordEncoder encoder; private final AuditService audit;
 private final String dummyHash; private final ConcurrentHashMap<String,Attempt> sources=new ConcurrentHashMap<>();
 private record Attempt(int count,Instant until) {}
 public LoginService(AppUserRepository users,PasswordEncoder encoder,AuditService audit) { this.users=users; this.encoder=encoder; this.audit=audit; dummyHash=encoder.encode(java.util.UUID.randomUUID().toString()); }
 @Transactional(noRollbackFor=ApiException.class)
 public AppUser authenticate(String login,String password,String source) {
  String id=PasswordPolicy.normalizeLogin(login);
  if(sources.size()>10000) sources.entrySet().removeIf(e -> e.getValue().until().isBefore(Instant.now()));
  if(sources.size()>=10000 && !sources.containsKey(source)) throw new ApiException(429,"LOGIN_RATE_LIMIT","로그인 시도가 많습니다. 잠시 후 다시 시도하세요.");
  Attempt attempt=sources.compute(source,(k,v) -> v==null || v.until().isBefore(Instant.now()) ? new Attempt(1,Instant.now().plusSeconds(900)) : new Attempt(v.count()+1,v.until()));
  if(attempt.count()>50) { audit.event(null,"LOGIN_RATE_LIMIT",null,"DENIED"); throw new ApiException(429,"LOGIN_RATE_LIMIT","로그인 시도가 많습니다. 잠시 후 다시 시도하세요."); }
  AppUser u=users.lockByLoginId(id).orElse(null);
  boolean match=encoder.matches(password==null ? "" : password,u==null ? dummyHash : u.getPasswordHash());
  if(u==null || u.getStatus()!=AccountStatus.ACTIVE || u.locked() || !match) {
   if(u!=null && u.getStatus()==AccountStatus.ACTIVE && !u.locked()) { u.loginFailed(); users.flush(); }
   audit.event(u,u!=null && u.locked()?"LOGIN_LOCKED":"LOGIN_FAILURE",u==null?null:u.getId(),"FAILURE");
   throw new ApiException(401,"LOGIN_FAILED","로그인 ID 또는 비밀번호를 확인하세요.");
  }
  u.loginSucceeded(); users.flush(); sources.remove(source); audit.event(u,"LOGIN_SUCCESS",u.getId(),"SUCCESS"); return u;
 }
}
