package com.construction.management.notification;

import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ApprovalEmailNotificationRepository extends JpaRepository<ApprovalEmailNotification, String> {
    @Query("select n.id from ApprovalEmailNotification n where n.status = :status and n.nextAttemptAt <= :now order by n.createdAt, n.id")
    List<String> findDueIds(@Param("status") ApprovalEmailNotification.Status status, @Param("now") LocalDateTime now, Pageable page);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select n from ApprovalEmailNotification n where n.id = :id")
    Optional<ApprovalEmailNotification> lockById(@Param("id") String id);
}
