package com.construction.management.auth;
import com.construction.management.account.*;
import jakarta.servlet.http.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.*;
import org.springframework.security.authentication.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/auth")
public class AuthController {
 private final LoginService login; private final AuthService auth; private final AuditService audit; private final CurrentActor actor;
 private final SessionCleanup sessions;
 private final HttpSessionSecurityContextRepository contexts=new HttpSessionSecurityContextRepository();
 public AuthController(LoginService login,AuthService auth,AuditService audit,CurrentActor actor,SessionCleanup sessions) { this.login=login; this.auth=auth; this.audit=audit; this.actor=actor; this.sessions=sessions; }
 public record Login(@NotBlank @Size(max=100) String login_id,@NotBlank @Size(max=256) String password) {}
 public record Password(@NotBlank String current_password,@NotBlank String new_password,@NotBlank String confirm_password) {}
 @PostMapping("/login") public Object login(@Valid @RequestBody Login req,HttpServletRequest request,HttpServletResponse response) {
  // A new login never inherits the previous account's session.
  sessions.clearSession(request,response);
  try {
   AppUser u=login.authenticate(req.login_id(),req.password(),request.getRemoteAddr());
   var authentication=UsernamePasswordAuthenticationToken.authenticated(new SessionUser(u.getId(),u.getAuthVersion(),u.getName(),u.getPosition()==null?null:u.getPosition().name()),null,List.of());
   var context=SecurityContextHolder.createEmptyContext(); context.setAuthentication(authentication); SecurityContextHolder.setContext(context); contexts.saveContext(context,request,response);
   return auth.view(u);
  } catch(RuntimeException error) { sessions.clear(request,response); throw error; }
 }
 @GetMapping("/me") public Object me() { return auth.me(); }
 @PostMapping("/logout") public void logout(HttpServletRequest request,HttpServletResponse response) { if(CurrentActor.optionalId()!=null) audit.event(actor.user(),"LOGOUT",CurrentActor.id(),"SUCCESS"); sessions.clear(request,response); }
 @PutMapping("/password") public void password(@Valid @RequestBody Password req,HttpServletRequest request,HttpServletResponse response) { auth.changePassword(req.current_password(),req.new_password(),req.confirm_password()); sessions.clear(request,response); }
}
