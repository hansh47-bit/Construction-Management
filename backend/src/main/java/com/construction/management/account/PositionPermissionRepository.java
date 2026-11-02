package com.construction.management.account;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.*;
public interface PositionPermissionRepository extends JpaRepository<PositionPermission,String> {
 // Filter in SQL before Hibernate converts permission_key to an enum. Old DBs
 // can retain retired keys; those rows must neither break login nor grant access.
 default List<PositionPermission> findByPosition(Position position) {
  return findByPositionAndPermissionKeyIn(position,EnumSet.allOf(Permission.class));
 }
 List<PositionPermission> findByPositionAndPermissionKeyIn(Position position,Collection<Permission> keys);
 default List<PositionPermission> lockPosition(Position position) {
  return lockSupportedPosition(position,EnumSet.allOf(Permission.class));
 }
 @Lock(LockModeType.PESSIMISTIC_WRITE)
 @Query("select p from PositionPermission p where p.position=:position and p.permissionKey in :keys order by p.id")
 List<PositionPermission> lockSupportedPosition(@Param("position") Position position,@Param("keys") Collection<Permission> keys);
}
