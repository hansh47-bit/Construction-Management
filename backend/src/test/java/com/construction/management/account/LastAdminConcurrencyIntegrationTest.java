package com.construction.management.account;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;

import com.construction.management.auth.SessionUser;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:last-admin-test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1", "app.demo-data.enabled=false"})
@AutoConfigureMockMvc
class LastAdminConcurrencyIntegrationTest {
 @Autowired MockMvc mvc;
 @Autowired AppUserRepository users;
 @Autowired AdminBootstrap bootstrap;
 @Autowired PasswordEncoder encoder;
 @Autowired ObjectMapper json;

 @Test void firstInstallAndConcurrentAdministratorDeletionPreserveOneActiveAdmin() throws Exception {
  assertThat(users.count()).isZero();
  bootstrap.create("first-admin","최초 관리자","Bootstrap-secret123!");
  AppUser first=users.findByLoginId("first-admin").orElseThrow();
  assertThat(first.accountType).isEqualTo(AccountType.ADMIN);
  assertThat(first.mustChangePassword).isTrue();
  assertThat(encoder.matches("Bootstrap-secret123!",first.passwordHash)).isTrue();
  String originalHash=first.passwordHash;
  assertThatThrownBy(() -> bootstrap.create("other-admin","다른 관리자","Another-secret123!")).isInstanceOf(IllegalStateException.class);
  assertThat(users.findById(first.getId()).orElseThrow().passwordHash).isEqualTo(originalHash);
  first.mustChangePassword=false; users.saveAndFlush(first);
  AppUser second=new AppUser("second-admin",encoder.encode("Another-secret123!"),"추가 관리자",AccountType.ADMIN,null,"test-operation"); second.mustChangePassword=false; users.saveAndFlush(second);
  ExecutorService pool=Executors.newFixedThreadPool(2); CountDownLatch ready=new CountDownLatch(2),start=new CountDownLatch(1);
  try {
   Callable<Integer> one=deleteOther(first,second,ready,start), two=deleteOther(second,first,ready,start);
   Future<Integer> r1=pool.submit(one),r2=pool.submit(two); assertThat(ready.await(5,TimeUnit.SECONDS)).isTrue(); start.countDown();
   var statuses=List.of(r1.get(15,TimeUnit.SECONDS),r2.get(15,TimeUnit.SECONDS));
   assertThat(statuses).contains(204); assertThat(statuses.stream().filter(s -> s==204).count()).isEqualTo(1);
   assertThat(users.findAll().stream().filter(u -> u.accountType==AccountType.ADMIN && u.status==AccountStatus.ACTIVE).count()).isEqualTo(1);
  } finally { pool.shutdownNow(); }
 }
 private Callable<Integer> deleteOther(AppUser actor,AppUser target,CountDownLatch ready,CountDownLatch start) {
  return () -> { ready.countDown(); start.await(); var auth=UsernamePasswordAuthenticationToken.authenticated(new SessionUser(actor.getId(),actor.authVersion,actor.name,null),null,List.of());
   return mvc.perform(delete("/api/v1/admin/users/"+target.getId()).with(authentication(auth)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("version",target.version,"reason","관리자 동시 삭제 검증")))).andReturn().getResponse().getStatus();
  };
 }
}
