package com.construction.management.account;
import static com.construction.management.account.AccountDtos.*;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/admin")
public class AdminController {
 private final AccountService service;
 public AdminController(AccountService service) { this.service=service; }
 @GetMapping("/users") public Object users(@RequestParam(required=false) String keyword,@RequestParam(required=false) Position position,@RequestParam(required=false) AccountStatus status,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) { return service.search(keyword,position,status,page,size); }
 @PostMapping("/users") @ResponseStatus(org.springframework.http.HttpStatus.CREATED) public Object create(@Valid @RequestBody Create req) { return service.create(req); }
 @GetMapping("/users/{id}") public Object get(@PathVariable String id) { return service.get(id); }
 @PatchMapping("/users/{id}") public Object update(@PathVariable String id,@Valid @RequestBody Update req) { return service.update(id,req); }
 @DeleteMapping("/users/{id}") @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT) public void delete(@PathVariable String id,@Valid @RequestBody Action req) { service.delete(id,req); }
 @PostMapping("/users/{id}/reset-password") public Object reset(@PathVariable String id,@Valid @RequestBody Reset req) { return service.reset(id,req); }
 @PostMapping("/users/{id}/unlock") public Object unlock(@PathVariable String id,@Valid @RequestBody Action req) { return service.unlock(id,req); }
 @GetMapping("/positions") public Object positions() { return service.positions(); }
 @PutMapping("/positions/{position}/permissions") public Object permissions(@PathVariable Position position,@Valid @RequestBody PermissionUpdate req) { return service.savePermissions(position,req); }
 @GetMapping("/audit-logs") public Object logs(@RequestParam(required=false) String action,@RequestParam(required=false) String keyword,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) { return service.auditLogs(action,keyword,page,size); }
}
