package com.construction.management.auth;
import com.construction.management.account.*;
import java.util.Set;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
public class AuthService {
 private final CurrentActor actor; private final PermissionService permissions; private final AppUserRepository users; private final PasswordEncoder encoder; private final AuditService audit;
 public AuthService(CurrentActor actor,PermissionService permissions,AppUserRepository users,PasswordEncoder encoder,AuditService audit) { this.actor=actor; this.permissions=permissions; this.users=users; this.encoder=encoder; this.audit=audit; }
 public record Me(String id,String login_id,String name,AccountType account_type,Position position,Set<Permission> permissions,boolean must_change_password) {}
 public Me me() { return view(actor.user()); }
 public Me view(AppUser u) { return new Me(u.getId(),u.getLoginId(),u.getName(),u.getAccountType(),u.getPosition(),permissions.effective(u),u.isMustChangePassword()); }
 @Transactional public void changePassword(String current,String next,String confirm) {
  AppUser u=users.lockById(CurrentActor.id()).orElseThrow();
  if(!encoder.matches(current==null?"":current,u.getPasswordHash())) throw new IllegalArgumentException("현재 비밀번호를 확인하세요.");
  if(next==null || !next.equals(confirm)) throw new IllegalArgumentException("새 비밀번호 확인값이 일치하지 않습니다.");
  PasswordPolicy.validate(next,u.getLoginId()); if(encoder.matches(next,u.getPasswordHash())) throw new IllegalArgumentException("현재 비밀번호와 다른 비밀번호를 사용하세요.");
  u.changePassword(encoder.encode(next),false,u.getId()); audit.record("PASSWORD_CHANGE","USER",u.getId(),null,null,null,"SUCCESS");
 }
}
