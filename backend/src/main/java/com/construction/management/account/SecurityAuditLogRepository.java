package com.construction.management.account;
import org.springframework.data.jpa.repository.*;
public interface SecurityAuditLogRepository extends JpaRepository<SecurityAuditLog,Long>, JpaSpecificationExecutor<SecurityAuditLog> {}
