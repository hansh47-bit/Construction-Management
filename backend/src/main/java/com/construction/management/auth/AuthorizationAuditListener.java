package com.construction.management.auth;
import com.construction.management.account.*;
import org.springframework.context.event.EventListener;
import org.springframework.security.authorization.event.AuthorizationDeniedEvent;
import org.springframework.stereotype.Component;
@Component
public class AuthorizationAuditListener {
 private final AuditService audit; private final CurrentActor actor;
 public AuthorizationAuditListener(AuditService audit,CurrentActor actor) { this.audit=audit; this.actor=actor; }
 @EventListener public void denied(AuthorizationDeniedEvent<?> event) { audit.event(CurrentActor.optionalId()==null?null:actor.user(),"ACCESS_DENIED","METHOD","DENIED"); }
}
