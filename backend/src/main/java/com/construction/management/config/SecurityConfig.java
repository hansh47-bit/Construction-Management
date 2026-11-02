package com.construction.management.config;
import com.construction.management.account.*;
import com.construction.management.auth.*;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.*;
@Configuration @EnableMethodSecurity
@org.springframework.transaction.annotation.EnableTransactionManagement(order=0)
public class SecurityConfig {
 @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }
 @Bean CsrfTokenRepository csrfTokenRepository() { return new HttpSessionCsrfTokenRepository(); }
 @Bean SecurityFilterChain security(HttpSecurity http,AppUserRepository users,ApiErrors errors,AuditService audit,CsrfTokenRepository csrf) throws Exception {
  http.cors(c -> {}).csrf(c -> c.csrfTokenRepository(csrf))
   .formLogin(c -> c.disable()).httpBasic(c -> c.disable()).logout(c -> c.disable()).requestCache(c -> c.disable())
   .addFilterBefore(new SessionValidationFilter(users,errors),CsrfFilter.class)
   .exceptionHandling(c -> c.authenticationEntryPoint((req,res,e) -> errors.write(req,res,401,"UNAUTHENTICATED","로그인이 필요합니다."))
    .accessDeniedHandler((req,res,e) -> { audit.event(null,"ACCESS_DENIED",req.getRequestURI(),"DENIED"); errors.write(req,res,403,e instanceof CsrfException?"CSRF_INVALID":"FORBIDDEN",e instanceof CsrfException?"보안 토큰이 만료되었습니다. 다시 시도하세요.":"이 작업에 대한 권한이 없습니다."); }))
   .authorizeHttpRequests(c -> c.requestMatchers(org.springframework.http.HttpMethod.OPTIONS,"/**").permitAll()
    .requestMatchers("/api/v1/auth/csrf","/api/v1/auth/login").permitAll()
    .requestMatchers("/api/v1/auth/me","/api/v1/auth/logout","/api/v1/auth/password").authenticated()
    .requestMatchers("/api/v1/admin/**","/api/v1/users/options","/api/v1/projects/**","/api/v1/approvals/**","/api/dashboard","/api/dashboard/workflow").authenticated()
    .anyRequest().denyAll());
  return http.build();
 }
}
