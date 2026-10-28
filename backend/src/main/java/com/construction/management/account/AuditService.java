package com.construction.management.account;
import com.construction.management.auth.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import org.springframework.web.context.request.*;
@Service
public class AuditService {
 private final SecurityAuditLogRepository logs; private final AppUserRepository users;
 public AuditService(SecurityAuditLogRepository logs,AppUserRepository users) { this.logs=logs; this.users=users; }
 @Transactional public void record(String action,String type,String target,String before,String after,String reason,String result) {
  AppUser u=CurrentActor.optionalId()==null ? null : users.findById(CurrentActor.id()).orElse(null);
  append(u,action,type,target,before,after,reason,result);
 }
 @Transactional(propagation=Propagation.REQUIRES_NEW) public void event(AppUser user,String action,String target,String result) { append(user,action,"SECURITY",target,null,null,null,result); }
 private void append(AppUser u,String action,String type,String target,String before,String after,String reason,String result) {
  var a=RequestContextHolder.getRequestAttributes(); String source=a instanceof ServletRequestAttributes s ? s.getRequest().getRemoteAddr() : "command";
  logs.save(new SecurityAuditLog(u==null ? null : u.getId(),u==null ? null : u.getName(),u==null ? null : String.valueOf(u.getPosition()),action,type,target,before,after,reason,result,source,ApiErrors.requestId()));
 }
}
