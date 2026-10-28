package com.construction.management.auth;
import com.construction.management.account.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.stereotype.Component;
@Component
public class CurrentActor {
 private final AppUserRepository users;
 public CurrentActor(AppUserRepository users) { this.users=users; }
 public AppUser user() { return users.findById(id()).orElseThrow(() -> new AuthenticationCredentialsNotFoundException("로그인이 필요합니다.")); }
 public static String id() { var auth=SecurityContextHolder.getContext().getAuthentication(); if(auth==null || !(auth.getPrincipal() instanceof SessionUser u)) throw new AuthenticationCredentialsNotFoundException("로그인이 필요합니다."); return u.id(); }
 public static String optionalId() { try { return id(); } catch(AuthenticationCredentialsNotFoundException e) { return null; } }
 public static String name() { var auth=SecurityContextHolder.getContext().getAuthentication(); if(auth==null || !(auth.getPrincipal() instanceof SessionUser u)) throw new AuthenticationCredentialsNotFoundException("로그인이 필요합니다."); return u.name(); }
}
