package com.construction.management.account;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Check;

@Entity @Table(name = "app_users") @Getter @NoArgsConstructor
@Check(constraints = "(account_type = 'ADMIN' AND position IS NULL) OR (account_type = 'USER' AND position IS NOT NULL)")
public class AppUser {
 @Id private String id = UUID.randomUUID().toString();
 @Column(name="login_id", nullable=false, unique=true, length=50) String loginId;
 @Column(name="password_hash", nullable=false) String passwordHash;
 @Column(nullable=false, length=100) String name;
 @Enumerated(EnumType.STRING) @Column(name="account_type", nullable=false) AccountType accountType;
 @Enumerated(EnumType.STRING) Position position;
 @Enumerated(EnumType.STRING) @Column(nullable=false) AccountStatus status = AccountStatus.ACTIVE;
 String phone;
 String email;
 @Column(nullable=false) boolean mustChangePassword = true;
 @Column(nullable=false) int failedLoginCount;
 LocalDateTime lockedUntil;
 @Column(nullable=false) long authVersion;
 LocalDateTime lastLoginAt;
 @Column(nullable=false, updatable=false) LocalDateTime createdAt = LocalDateTime.now();
 String createdBy;
 LocalDateTime updatedAt = LocalDateTime.now();
 String updatedBy;
 LocalDateTime deletedAt;
 String deletedBy;
 @Version long version;
 public AppUser(String loginId, String hash, String name, AccountType type, Position position, String actor) {
  this.loginId=loginId; this.passwordHash=hash; this.name=name; this.accountType=type; this.position=position; this.createdBy=actor;
 }
 public boolean locked() { return lockedUntil != null && lockedUntil.isAfter(LocalDateTime.now()); }
 public void loginFailed() { if (++failedLoginCount >= 5) lockedUntil=LocalDateTime.now().plusMinutes(15); }
 public void loginSucceeded() { failedLoginCount=0; lockedUntil=null; lastLoginAt=LocalDateTime.now(); }
 public void changePassword(String hash, boolean temporary, String actor) { passwordHash=hash; mustChangePassword=temporary; authVersion++; touch(actor); }
 public void unlock(String actor) { failedLoginCount=0; lockedUntil=null; touch(actor); }
 public void touch(String actor) { updatedAt=LocalDateTime.now(); updatedBy=actor; }
}
