package com.construction.management.account;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
@Service
public class AdminBootstrap {
 @jakarta.persistence.PersistenceContext private jakarta.persistence.EntityManager entityManager;
 private final AppUserRepository users; private final PasswordEncoder encoder; private final AuditService audit;
 public AdminBootstrap(AppUserRepository users,PasswordEncoder encoder,AuditService audit) { this.users=users; this.encoder=encoder; this.audit=audit; }
 @Transactional public void create(String login,String name,String password) {
  entityManager.createNativeQuery("SELECT id FROM security_locks WHERE id='ADMIN_BOOTSTRAP' FOR UPDATE").getSingleResult();
  if(users.count()!=0) throw new IllegalStateException("기존 계정이 있어 초기 어드민 발급을 중단했습니다.");
  String normalized=PasswordPolicy.normalizeLogin(login);
  if(!normalized.matches("[a-z0-9._-]{4,50}") || name==null || name.isBlank() || name.length()>100) throw new IllegalArgumentException("초기 어드민 ID와 이름을 환경변수로 지정하세요.");
  PasswordPolicy.validate(password,normalized); AppUser u=users.save(new AppUser(normalized,encoder.encode(password),name.trim(),AccountType.ADMIN,null,"bootstrap")); audit.record("ADMIN_BOOTSTRAP","USER",u.getId(),null,null,"최초 설치","SUCCESS");
 }
}
