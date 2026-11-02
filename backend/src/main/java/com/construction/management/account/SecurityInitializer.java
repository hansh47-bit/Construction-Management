package com.construction.management.account;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;
@Component @Order(0)
public class SecurityInitializer implements CommandLineRunner {
 private final PermissionService permissions; private final AdminBootstrap bootstrap; private final Environment env; private final ConfigurableApplicationContext context;
 public SecurityInitializer(PermissionService permissions,AdminBootstrap bootstrap,Environment env,ConfigurableApplicationContext context) { this.permissions=permissions; this.bootstrap=bootstrap; this.env=env; this.context=context; }
 public void run(String... args) {
  permissions.initializeDefaults();
  if(env.getProperty("app.bootstrap-admin",Boolean.class,false)) {
   String login=env.getProperty("app.bootstrap.login"), name=env.getProperty("app.bootstrap.name"), password=env.getProperty("app.bootstrap.password");
   if(env.getProperty("app.bootstrap-reset-accounts",Boolean.class,false)) bootstrap.resetAndCreate(login,name,password);
   else bootstrap.create(login,name,password);
   context.close();
  }
}
}
