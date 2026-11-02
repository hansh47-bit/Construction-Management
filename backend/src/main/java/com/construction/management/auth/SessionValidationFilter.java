package com.construction.management.auth;
import com.construction.management.account.*;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
public class SessionValidationFilter extends OncePerRequestFilter {
 private final AppUserRepository users; private final ApiErrors errors;
 public SessionValidationFilter(AppUserRepository users,ApiErrors errors) { this.users=users; this.errors=errors; }
 @Override protected void doFilterInternal(HttpServletRequest req,HttpServletResponse res,FilterChain chain) throws IOException,ServletException {
  res.setHeader("Cache-Control","no-store"); res.setHeader("X-Request-ID",ApiErrors.requestId(req));
  var auth=SecurityContextHolder.getContext().getAuthentication();
  if(auth!=null && auth.getPrincipal() instanceof SessionUser s) {
   AppUser u=users.findById(s.id()).orElse(null);
   if(u==null || u.getStatus()!=AccountStatus.ACTIVE || u.getAuthVersion()!=s.authVersion()) {
    if(req.getSession(false)!=null) req.getSession(false).invalidate(); SecurityContextHolder.clearContext(); errors.write(req,res,401,"SESSION_INVALIDATED","로그인이 만료되었습니다. 다시 로그인하세요."); return;
   }
   SecurityContextHolder.getContext().setAuthentication(org.springframework.security.authentication.UsernamePasswordAuthenticationToken.authenticated(new SessionUser(u.getId(),u.getAuthVersion(),u.getName(),u.getPosition()==null?null:u.getPosition().name()),null,java.util.List.of()));
   if(u.isMustChangePassword() && !java.util.Set.of("/api/v1/auth/me","/api/v1/auth/csrf","/api/v1/auth/password","/api/v1/auth/logout").contains(req.getRequestURI())) { errors.write(req,res,403,"PASSWORD_CHANGE_REQUIRED","비밀번호를 먼저 변경하세요."); return; }
  }
  chain.doFilter(req,res);
 }
}
