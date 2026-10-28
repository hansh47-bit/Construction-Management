package com.construction.management.account;
import java.util.*;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
public interface AppUserRepository extends JpaRepository<AppUser,String>, JpaSpecificationExecutor<AppUser> {
 Optional<AppUser> findByLoginId(String loginId);
 boolean existsByLoginId(String loginId);
 @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select u from AppUser u where u.loginId=:login")
 Optional<AppUser> lockByLoginId(@Param("login") String login);
 @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select u from AppUser u where u.id=:id")
 Optional<AppUser> lockById(@Param("id") String id);
 @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select u from AppUser u where u.accountType=com.construction.management.account.AccountType.ADMIN order by u.id")
 List<AppUser> lockAdmins();
 List<AppUser> findByAccountTypeAndStatus(AccountType type, AccountStatus status);
}
