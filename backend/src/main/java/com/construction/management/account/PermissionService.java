package com.construction.management.account;
import com.construction.management.auth.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
public class PermissionService {
 private final PositionPermissionRepository permissions; private final CurrentActor actor;
 public PermissionService(PositionPermissionRepository permissions,CurrentActor actor) { this.permissions=permissions; this.actor=actor; }
 public Set<Permission> effective(AppUser user) {
  if(user.getAccountType()!=AccountType.USER) return Set.of();
  Set<Permission> result=EnumSet.noneOf(Permission.class);
  permissions.findByPosition(user.getPosition()).forEach(p -> { if(p.isAllowed() && (!p.getPermissionKey().locked() || p.getPermissionKey().defaultAllowed(user.getPosition()))) result.add(p.getPermissionKey()); });
  boolean changed; do { changed=result.removeIf(p -> p.dependency!=null && !result.contains(Permission.valueOf(p.dependency))); } while(changed);
  return result;
 }
 public boolean has(Permission permission) { return effective(actor.user()).contains(permission); }
 public void require(Permission permission) { if(!has(permission)) throw new org.springframework.security.access.AccessDeniedException("권한이 없습니다."); }
 public void requireAdmin() { if(actor.user().getAccountType()!=AccountType.ADMIN) throw new org.springframework.security.access.AccessDeniedException("관리자만 사용할 수 있습니다."); }
 @Transactional public void initializeDefaults() {
  for(Position p:Position.values()) for(Permission key:Permission.values()) { String id=p+":"+key; if(!permissions.existsById(id)) permissions.save(new PositionPermission(p,key)); }
 }
}
