package com.construction.management.account;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.*;
public interface PositionPermissionRepository extends JpaRepository<PositionPermission,String> {
 List<PositionPermission> findByPosition(Position position);
 @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select p from PositionPermission p where p.position=:position order by p.id")
 List<PositionPermission> lockPosition(@Param("position") Position position);
}
