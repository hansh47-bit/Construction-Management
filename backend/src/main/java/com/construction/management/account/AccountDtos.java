package com.construction.management.account;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import java.util.*;
public final class AccountDtos {
 private AccountDtos() {}
 public record Create(@NotBlank String login_id,@NotBlank @Size(max=100) String name,@NotNull Position position,@NotNull String temporary_password,@Size(max=100) String phone,@Email String email) {}
 public record Update(@NotBlank @Size(max=100) String name,Position position,@NotNull AccountStatus status,@Size(max=100) String phone,@Email String email,@NotNull Long version,@Size(max=1000) String reason) {}
 public record Action(@NotNull Long version,@NotBlank @Size(max=1000) String reason) {}
 public record Reset(@NotNull Long version,@NotBlank String temporary_password,@NotBlank @Size(max=1000) String reason) {}
 public record UserView(String id,String login_id,String name,AccountType account_type,Position position,AccountStatus status,String phone,String email,boolean must_change_password,boolean locked,LocalDateTime locked_until,LocalDateTime last_login_at,LocalDateTime created_at,long version) {
  public static UserView from(AppUser u) { return new UserView(u.getId(),u.getLoginId(),u.getName(),u.getAccountType(),u.getPosition(),u.getStatus(),u.getPhone(),u.getEmail(),u.isMustChangePassword(),u.locked(),u.getLockedUntil(),u.getLastLoginAt(),u.getCreatedAt(),u.getVersion()); }
 }
 public record PermissionValue(Permission key,String label,boolean allowed,boolean editable,String locked_reason,String dependency,long version) {}
 public record PositionView(Position position,List<PermissionValue> permissions) {}
 public record PermissionUpdate(@NotNull Map<Permission,Boolean> permissions,@NotNull Map<Permission,Long> versions,@NotBlank @Size(max=1000) String reason) {}
 public record UserOption(String id,String name,Position position) {}
}
