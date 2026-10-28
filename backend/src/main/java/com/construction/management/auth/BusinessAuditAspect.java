package com.construction.management.auth;
import com.construction.management.account.*;
import org.aspectj.lang.*;
import org.aspectj.lang.annotation.*;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
@Aspect @Component @Order(100)
public class BusinessAuditAspect {
 private final AuditService audit; private final CurrentActor actor;
 public BusinessAuditAspect(AuditService audit,CurrentActor actor) { this.audit=audit; this.actor=actor; }
 @Around("@annotation(com.construction.management.auth.BusinessAction)")
 public Object log(ProceedingJoinPoint call) throws Throwable {
  String action=call.getSignature().getDeclaringType().getSimpleName()+"."+call.getSignature().getName();
  String target=call.getArgs().length>0 && call.getArgs()[0] instanceof String s?s:null;
  try { Object result=call.proceed(); audit.record(action,"BUSINESS",target,null,null,null,"SUCCESS"); return result; }
  catch(Throwable e) { audit.event(CurrentActor.optionalId()==null?null:actor.user(),action,target,"FAILURE"); throw e; }
 }
}
