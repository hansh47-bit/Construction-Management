package com.construction.management.auth;
import com.construction.management.account.*;
import jakarta.servlet.http.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.*;
import org.springframework.security.authentication.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.*;
import org.springframework.security.web.authentication.session.*;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/auth")
public class AuthController {
 private final LoginService login; private final AuthService auth; private final AuditService audit; private final CurrentActor actor;
 private final CsrfTokenRepository csrf;
 private final HttpSessionSecurityContextRepository contexts=new HttpSessionSecurityContextRepository();
 public AuthController(LoginService login,AuthService auth,AuditService audit,CurrentActor actor,CsrfTokenRepository csrf) { this.login=login; this.auth=auth; this.audit=audit; this.actor=actor; this.csrf=csrf; }
 public record Login(@NotBlank @Size(max=100) String login_id,@NotBlank @Size(max=256) String password) {}
 public record Password(@NotBlank String current_password,@NotBlank String new_password,@NotBlank String confirm_password) {}
 @GetMapping("/csrf") public Object csrf(CsrfToken token) { return Map.of("token",token.getToken(),"header_name",token.getHeaderName()); }
 @PostMapping("/login") public Object login(@Valid @RequestBody Login req,HttpServletRequest request,HttpServletResponse response) {
  AppUser u=login.authenticate(req.login_id(),req.password(),request.getRemoteAddr());
  var authentication=UsernamePasswordAuthenticationToken.authenticated(new SessionUser(u.getId(),u.getAuthVersion(),u.getName(),u.getPosition()==null?null:u.getPosition().name()),null,List.of());
  new ChangeSessionIdAuthenticationStrategy().onAuthentication(authentication,request,response);
  new CsrfAuthenticationStrategy(csrf).onAuthentication(authentication,request,response);
  var context=SecurityContextHolder.createEmptyContext(); context.setAuthentication(authentication); SecurityContextHolder.setContext(context); contexts.saveContext(context,request,response);
  return auth.view(u);
 }
 @GetMapping("/me") public Object me() { return auth.me(); }
 @PostMapping("/logout") public void logout(HttpServletRequest request,HttpServletResponse response) { audit.event(actor.user(),"LOGOUT",CurrentActor.id(),"SUCCESS"); clear(request,response); }
 @PutMapping("/password") public void password(@Valid @RequestBody Password req,HttpServletRequest request,HttpServletResponse response) { auth.changePassword(req.current_password(),req.new_password(),req.confirm_password()); clear(request,response); }
 private void clear(HttpServletRequest request,HttpServletResponse response) { new org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler().logout(request,response,SecurityContextHolder.getContext().getAuthentication()); csrf.saveToken(null,request,response); var cookie=new Cookie("JSESSIONID",""); cookie.setPath("/"); cookie.setHttpOnly(true); cookie.setMaxAge(0); cookie.setSecure(request.isSecure()); response.addCookie(cookie); }
}
