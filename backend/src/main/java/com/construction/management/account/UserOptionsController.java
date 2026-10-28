package com.construction.management.account;
import org.springframework.web.bind.annotation.*;
@RestController
public class UserOptionsController {
 private final AccountService service;
 public UserOptionsController(AccountService service) { this.service=service; }
 @GetMapping("/api/v1/users/options") public Object options() { return service.options(); }
}
