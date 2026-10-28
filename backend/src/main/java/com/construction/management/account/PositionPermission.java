package com.construction.management.account;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;
@Entity @Table(name="position_permissions",uniqueConstraints=@UniqueConstraint(columnNames={"position","permission_key"}))
@Getter @NoArgsConstructor
public class PositionPermission {
 @Id String id;
 @Enumerated(EnumType.STRING) @Column(nullable=false) Position position;
 @Enumerated(EnumType.STRING) @Column(name="permission_key", nullable=false) Permission permissionKey;
 @Column(nullable=false) boolean allowed;
 LocalDateTime updatedAt=LocalDateTime.now(); String updatedBy;
 @Version long version;
 public PositionPermission(Position position, Permission permission) { this.id=position+":"+permission; this.position=position; this.permissionKey=permission; this.allowed=permission.defaultAllowed(position); }
 public void update(boolean allowed,String actor) { this.allowed=allowed; updatedBy=actor; updatedAt=LocalDateTime.now(); }
}
